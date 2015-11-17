package mosaic.core.detection;


import java.util.Vector;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.plugin.filter.Convolver;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackStatistics;
import mosaic.core.utils.MosaicImageProcessingTools;
import mosaic.core.utils.MosaicUtils;


/**
 * FeaturePointDetector detects the "real" particles in provided frames.
 */
public class FeaturePointDetector {
    public static enum Mode { ABS_THRESHOLD_MODE, PERCENTILE_MODE }

    // user defined parameters and settings
    private float iGlobalMax;
    private float iGlobalMin;
    private double iCutoff;
    private float iPercentile;
    private int iRadius;
    private float iAbsIntensityThreshold;
    private Mode iThresholdMode = Mode.PERCENTILE_MODE;

    // Internal stuff
    private Vector<Particle> iParticles;
    private int iNumOfInitialyDetectedParticles;
    private int iNumOfRealParticles;
    private int[][] mask;
    
    
    /**
     * @param aGlobalMax - global maximum taken from processed movie / image sequence
     * @param aGlobalMin - global minimum
     */
    public FeaturePointDetector(float aGlobalMax, float aGlobalMin) {
        iGlobalMax = aGlobalMax;
        iGlobalMin = aGlobalMin;
        setDetectionParameters(3.0f, 0.001f, 3, 0.0f, false);
    }

    /**
     * First phase of the algorithm - time and memory consuming !! <br>
     * Determines the "real" particles in this frame (only for frame constructed from Image) <br>
     * Converts the <code>original_ip</code> to <code>FloatProcessor</code>, normalizes it, convolutes and dilates it,
     * finds the particles, refine their position and filters out non particles
     * 
     * @see ImageProcessor#convertToFloat()
     */
    public void featurePointDetection(MyFrame frame) {
        /*
         * Converting the original imageProcessor to float
         * This is a constraint caused by the lack of floating point precision of pixels
         * value in 16bit and 8bit image processors in ImageJ therefore, if the image is not
         * converted to 32bit floating point, false particles get detected
         */
        final ImageStack original_ips = frame.getOriginalImageStack();
        ImageStack restored_fps = new ImageStack(original_ips.getWidth(), original_ips.getHeight());

        for (int i = 1; i <= original_ips.getSize(); i++) {
            // if it is already a float, ImageJ does not create a duplicate
            restored_fps.addSlice(null, original_ips.getProcessor(i).convertToFloat().duplicate());
        }

        /* The algorithm is initialized by normalizing the frame */
        normalizeFrameFloat(restored_fps, iGlobalMin, iGlobalMax);
        // new StackWindow(new ImagePlus("after normalization",restored_fps));

        /* Image Restoration - Step 1 of the algorithm */
        restored_fps = imageRestoration(restored_fps);
        // new StackWindow(new ImagePlus("after restoration",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored_fps, 1, restored_fps.getSize())));

        /* Estimation of the point location - Step 2 of the algorithm */
        pointLocationsEstimation(restored_fps, frame.frame_number, frame.linkrange);
        
        /* Refinement of the point location - Step 3 of the algorithm */
        pointLocationsRefinement(restored_fps);
        
        /* Non Particle Discrimination(set a flag to particles) - Step 4 of the algorithm */
        nonParticleDiscrimination();

        /* Save frame information before particle discrimination/deletion - it will be lost otherwise */
        frame.setParticles(iParticles, iNumOfInitialyDetectedParticles);
        frame.generateFrameInfoBeforeDiscrimination();

        /* remove all the "false" particles from particles array */
        removeNonParticle();
        frame.setParticles(iParticles, iNumOfRealParticles);

        /* Set the real_particle_number */
        frame.real_particles_number = iNumOfRealParticles;
    }

    /**
     * Normalizes a given <code>ImageProcessor</code> to [0,1]. <br>
     * According to the pre determend global min and max pixel value in the movie. <br>
     * All pixel intensity values I are normalized as (I-gMin)/(gMax-gMin)
     * 
     * @param ip ImageProcessor to be normalized
     * @param global_min
     * @param global_max
     */
    private void normalizeFrameFloat(ImageStack is, float global_min, float global_max) {
        for (int s = 1; s <= is.getSize(); s++) {
            final float[] pixels = (float[]) is.getPixels(s);
            float tmp_pix_value;
            for (int i = 0; i < pixels.length; i++) {
                tmp_pix_value = (pixels[i] - global_min) / (global_max - global_min);
                pixels[i] = tmp_pix_value;
            }
        }
    }

    /**
     * Finds and sets the threshold value for this frame given the
     * user defined percenticle and an ImageProcessor if the thresholdmode is PERCENTILE.
     * If not, the threshold is set to its absolute (normalized) value. There is only one parameter
     * used, either percent or aIntensityThreshold depending on the threshold mode.
     * 
     * @param ip ImageProcessor after conversion, normalization and restoration
     * @param percent the upper rth percentile to be considered as candidate Particles
     * @param absIntensityThreshold2 a intensity value which defines a threshold.
     * @return 
     */
    private float findThreshold(ImageStack ips, double percent, float absIntensityThreshold2) {
        if (iThresholdMode == Mode.ABS_THRESHOLD_MODE) {
            // the percent parameter corresponds to an absolute value (not percent)
            return absIntensityThreshold2 - iGlobalMin / (iGlobalMax - iGlobalMin);
        }
        int s, i, j, thold, width;
        width = ips.getWidth();

        /* find this ImageStacks min and max pixel value */
        float min = 0f;
        float max = 0f;
        if (ips.getSize() > 1) {
            final StackStatistics sstats = new StackStatistics(new ImagePlus(null, ips));
            min = (float) sstats.min;
            max = (float) sstats.max;
        }
        else { // speeds up the 2d version:
            final ImageStatistics istats = ImageStatistics.getStatistics(ips.getProcessor(1), Measurements.MIN_MAX + Measurements.MODE + Measurements.STD_DEV, null);
            min = (float) istats.min;
            max = (float) istats.max;
        }

        final double[] hist = new double[256];
        for (i = 0; i < hist.length; i++) {
            hist[i] = 0;
        }
        for (s = 0; s < ips.getSize(); s++) {
            final float[] pixels = (float[]) ips.getProcessor(s + 1).getPixels();
            for (i = 0; i < ips.getHeight(); i++) {
                for (j = 0; j < ips.getWidth(); j++) {
                    hist[(int) ((pixels[i * width + j] - min) * 255.0 / (max - min))]++;
                }
            }
        }

        for (i = 254; i >= 0; i--) {
            hist[i] += hist[i + 1];
        }

        thold = 0;
        while (hist[255 - thold] / hist[0] < percent) {
            thold++;
            if (thold > 255) {
                break;
            }
        }
        thold = 255 - thold + 1;
        return ((float) (thold / 255.0) * (max - min) + min);
    }

    /**
     * Estimates the feature point locations in the given <code>ImageProcessor</code> <br>
     * Any pixel with the same value before and after dilation and value higher
     * then the pre calculated threshold is considered as a feature point (Particle). <br>
     * Adds each found <code>Particle</code> to the <code>particles</code> array. <br>
     * Mostly adapted from Ingo Oppermann implementation
     * 
     * @param ip ImageProcessor, should be after conversion, normalization and restoration
     */
    private void pointLocationsEstimation(ImageStack ips, int frame_number, int linkrange) {
        float threshold = findThreshold(ips, iPercentile, iAbsIntensityThreshold);
        /* do a grayscale dilation */
        final ImageStack dilated_ips = MosaicImageProcessingTools.dilateGeneric(ips, iRadius, 4);
        // new StackWindow(new ImagePlus("dilated ", dilated_ips));
        iParticles = new Vector<Particle>();
        /* loop over all pixels */
        final int height = ips.getHeight();
        final int width = ips.getWidth();
        for (int s = 0; s < ips.getSize(); s++) {
            final float[] ips_pixels = (float[]) ips.getProcessor(s + 1).getPixels();
            final float[] ips_dilated_pixels = (float[]) dilated_ips.getProcessor(s + 1).getPixels();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (ips_pixels[i * width + j] > threshold && ips_pixels[i * width + j] == ips_dilated_pixels[i * width + j]) { // check if pixel is a local maximum

                        /* and add each particle that meets the criteria to the particles array */
                        // (the starting point is the middle of the pixel and exactly on a focal plane:)
                        iParticles.add(new Particle(j + .5f, i + .5f, s, frame_number, linkrange));
                    }
                }
            }
        }
        iNumOfInitialyDetectedParticles = iParticles.size();
    }

    private void pointLocationsRefinement(ImageStack ips) {
        final int mask_width = 2 * iRadius + 1;
        final int imageWidth = ips.getWidth();
        final int imageHeight = ips.getHeight();
        final int imageDepth = ips.getSize();
        
        /* Set every value that is smaller than 0 to 0 */
        for (int s = 0; s < ips.getSize(); ++s) {
            final float[] pixels = (float[]) ips.getPixels(s + 1);
            for (int i = 0; i < pixels.length; ++i) {
                if (pixels[i] < 0) {
                    pixels[i] = 0;
                }
            }
        }

        for (int m = 0; m < iParticles.size(); ++m) {
            final Particle currentParticle = iParticles.elementAt(m);
            currentParticle.special = true;
            currentParticle.nonParticleDiscriminationScore = 0;
            float epsx = 0;
            float epsy = 0;
            float epsz = 0;
            
            do {
                currentParticle.m0 = 0;
                currentParticle.m1 = 0;
                currentParticle.m2 = 0;
                currentParticle.m3 = 0;
                currentParticle.m4 = 0;
                epsx = 0;
                epsy = 0;
                epsz = 0;
                
                for (int s = -iRadius; s <= iRadius; ++s) {
                    if (((int) currentParticle.iZ + s) < 0 || ((int) currentParticle.iZ + s) >= imageDepth) {
                        continue;
                    }
                    int z = (int) currentParticle.iZ + s;
                    float[] pixels = (float[]) ips.getPixels(z + 1);
                    
                    for (int k = -iRadius; k <= iRadius; ++k) {
                        if (((int) currentParticle.iY + k) < 0 || ((int) currentParticle.iY + k) >= imageHeight) {
                            continue;
                        }
                        int y = (int) currentParticle.iY + k;

                        for (int l = -iRadius; l <= iRadius; ++l) {
                            if (((int) currentParticle.iX + l) < 0 || ((int) currentParticle.iX + l) >= imageWidth) {
                                continue;
                            }
                            int x = (int) currentParticle.iX + l;
                            
                            float c = pixels[y * imageWidth + x] * mask[s + iRadius][(k + iRadius) * mask_width + (l + iRadius)];

                            epsx += l * c;
                            epsy += k * c;
                            epsz += s * c;
                            currentParticle.m0 += c;
                            int squaredDistance = k * k + l * l + s * s;
                            currentParticle.m1 += (float) Math.sqrt(squaredDistance) * c;
                            currentParticle.m2 += squaredDistance * c;
                            currentParticle.m3 += (float) Math.pow(squaredDistance, 1.5f) * c;
                            currentParticle.m4 += (float) Math.pow(squaredDistance, 2) * c;
                        }
                    }
                }

                epsx /= currentParticle.m0;
                epsy /= currentParticle.m0;
                epsz /= currentParticle.m0;
                currentParticle.m1 /= currentParticle.m0;
                currentParticle.m2 /= currentParticle.m0;
                currentParticle.m3 /= currentParticle.m0;
                currentParticle.m4 /= currentParticle.m0;

                int tx = (int) (10.0 * epsx);
                int ty = (int) (10.0 * epsy);
                int tz = (int) (10.0 * epsz);

                if ((tx) / 10.0 > 0.5) {
                    if ((int) currentParticle.iX + 1 < imageWidth) {
                        currentParticle.iX++;
                    }
                }
                else if ((tx) / 10.0 < -0.5) {
                    if ((int) currentParticle.iX - 1 >= 0) {
                        currentParticle.iX--;
                    }
                }
                if ((ty) / 10.0 > 0.5) {
                    if ((int) currentParticle.iY + 1 < imageHeight) {
                        currentParticle.iY++;
                    }
                }
                else if ((ty) / 10.0 < -0.5) {
                    if ((int) currentParticle.iY - 1 >= 0) {
                        currentParticle.iY--;
                    }
                }
                if ((tz) / 10.0 > 0.5) {
                    if ((int) currentParticle.iZ + 1 < imageDepth) {
                        currentParticle.iZ++;
                    }
                }
                else if ((tz) / 10.0 < -0.5) {
                    if ((int) currentParticle.iZ - 1 >= 0) {
                        currentParticle.iZ--;
                    }
                }

                if ((tx) / 10.0 <= 0.5 && (tx) / 10.0 >= -0.5 && (ty) / 10.0 <= 0.5 && (ty) / 10.0 >= -0.5 && (tz) / 10.0 <= 0.5 && (tz) / 10.0 >= -0.5) {
                    break;
                }
            } while (epsx > 0.5 || epsx < -0.5 || epsy > 0.5 || epsy < -0.5 || epsz > 0.5 || epsz < -0.5);
            
            currentParticle.iX += epsx;
            currentParticle.iY += epsy;
            currentParticle.iZ += epsz;
        }
    }

    /**
     * Rejects spurious particles detections such as unspecific signals, dust, or particle aggregates. <br>
     * The implemented classification algorithm after Crocker and Grier [68] is based on the
     * intensity moments of orders 0 and 2. <br>
     * Particles with lower final score than the user-defined cutoff are discarded <br>
     * Adapted "as is" from Ingo Oppermann implementation
     */
    private void nonParticleDiscrimination() {
        int j, k;
        double score;
        int max_x = 1, max_y = 1, max_z = 1;
        iNumOfRealParticles = iNumOfInitialyDetectedParticles;
        if (iParticles.size() == 1) {
            iParticles.elementAt(0).nonParticleDiscriminationScore = Float.MAX_VALUE;
        }
        for (j = 0; j < iParticles.size(); j++) {
            // int accepted = 1;
            max_x = Math.max((int) iParticles.elementAt(j).iX, max_x);
            max_y = Math.max((int) iParticles.elementAt(j).iY, max_y);
            max_z = Math.max((int) iParticles.elementAt(j).iZ, max_z);

            for (k = j + 1; k < iParticles.size(); k++) {
                score = (1.0 / (2.0 * Math.PI * 0.1 * 0.1))
                        * Math.exp(-(iParticles.elementAt(j).m0 - iParticles.elementAt(k).m0) * (iParticles.elementAt(j).m0 - iParticles.elementAt(k).m0) / (2.0 * 0.1)
                                - (iParticles.elementAt(j).m2 - iParticles.elementAt(k).m2) * (iParticles.elementAt(j).m2 - iParticles.elementAt(k).m2) / (2.0 * 0.1));
                iParticles.elementAt(j).nonParticleDiscriminationScore += score;
                iParticles.elementAt(k).nonParticleDiscriminationScore += score;
            }
            if (iParticles.elementAt(j).nonParticleDiscriminationScore < iCutoff) {
                iParticles.elementAt(j).special = false;
                iNumOfRealParticles--;
            }
            // System.out.println(j + "\t" + particles.elementAt(j).m0 + "\t" + particles.elementAt(j).m2 + "\t" + accepted);
        }

        // Remove duplicates (happens when dealing with artificial images)
        // Create a bitmap (with ghostlayers to not have to perform bounds checking)
        final boolean[][][] vBitmap = new boolean[max_z + 3][max_y + 3][max_x + 3];
        for (int z = 0; z < max_z + 3; z++) {
            for (int y = 0; y < max_y + 3; y++) {
                for (int x = 0; x < max_x + 3; x++) {
                    vBitmap[z][y][x] = false;
                }
            }
        }

        for (j = 0; j < iParticles.size(); j++) {
            boolean vParticleInNeighborhood = false;
            for (int oz = -1; !vParticleInNeighborhood && oz <= 1; oz++) {
                for (int oy = -1; !vParticleInNeighborhood && oy <= 1; oy++) {
                    for (int ox = -1; !vParticleInNeighborhood && ox <= 1; ox++) {
                        if (vBitmap[(int) iParticles.elementAt(j).iZ + 1 + oz][(int) iParticles.elementAt(j).iY + 1 + oy][(int) iParticles.elementAt(j).iX + 1 + ox]) {
                            vParticleInNeighborhood = true;
                        }
                    }
                }
            }

            if (vParticleInNeighborhood) {
                iParticles.elementAt(j).special = false;
                iNumOfRealParticles--;
            }
            else {
                vBitmap[(int) iParticles.elementAt(j).iZ + 1][(int) iParticles.elementAt(j).iY + 1][(int) iParticles.elementAt(j).iX + 1] = true;
            }
        }

    }

    /**
     * removes particles that were discarded by the <code>nonParticleDiscrimination</code> method
     * from the particles array. <br>
     * Non particles will be removed from the <code>particles</code> array so if their info is
     * needed, it should be saved before calling this method
     * 
     * @see MyFrame#nonParticleDiscrimination()
     */
    private void removeNonParticle() {
        for (int i = iParticles.size() - 1; i >= 0; i--) {
            if (!iParticles.elementAt(i).special) {
                iParticles.removeElementAt(i);
            }
        }
    }

    /**
     * Corrects imperfections in the given <code>ImageStack</code> by
     * convolving it (slice by slice, not 3D) with the pre calculated <code>kernel</code>
     * 
     * @param is ImageStack to be restored
     * @return the restored <code>ImageProcessor</code>
     * @see Convolver#convolve(ij.process.ImageProcessor, float[], int, int)
     */
    private ImageStack imageRestoration(ImageStack is) {
        // remove the clutter
        ImageStack restored = null;

        // pad the imagestack
        if (is.getSize() > 1) {
            // 3D mode
            restored = MosaicUtils.padImageStack3D(is, iRadius);
        }
        else {
            // we're in 2D mode
            final ImageProcessor rp = MosaicUtils.padImageStack2D(is.getProcessor(1), iRadius);
            restored = new ImageStack(rp.getWidth(), rp.getHeight());
            restored.addSlice("", rp);
        }

        // Old switch statement for:  case BOX_CAR_AVG:
        // There was found to be a 2*lambda_n for the sigma of the Gaussian kernel.
        // Set it back to 1*lambda_n to match the 2D implementation.
        final float lambda_n = 1;
        GaussBlur3D(restored, 1 * lambda_n);
        // new StackWindow(new ImagePlus("convolved 3d",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));
        boxCarBackgroundSubtractor(restored);
        // new StackWindow(new ImagePlus("after bg subtraction",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));

        if (is.getSize() > 1) {
            // again, 3D crop
            // new StackWindow(new ImagePlus("before cropping",mosaic.core.utils.MosaicUtils.GetSubStackCopyInFloat(restored, 1, restored.getSize())));
            restored = MosaicUtils.cropImageStack3D(restored, iRadius);
        }
        else {
            // 2D crop
            final ImageProcessor rp = MosaicUtils.cropImageStack2D(restored.getProcessor(1), iRadius);
            restored = new ImageStack(rp.getWidth(), rp.getHeight());
            restored.addSlice("", rp);
        }
        // new StackWindow(new ImagePlus("restored", GetSubStackCopyInFloat(restored, 1, restored.getSize())));
        return restored;

    }

    private void GaussBlur3D(ImageStack is, float aSigma) {
        final float[] vKernel = CalculateNormalizedGaussKernel(aSigma);
        int kernel_radius = vKernel.length / 2;
        final int nSlices = is.getSize();
        final int vWidth = is.getWidth();
        for (int i = 1; i <= nSlices; i++) {
            final ImageProcessor restored_proc = is.getProcessor(i);
            final Convolver convolver = new Convolver();
            // no need to normalize the kernel - its already normalized
            convolver.setNormalize(false);
            // the gaussian kernel is separable and can done in 3x 1D convolutions.
            convolver.convolve(restored_proc, vKernel, vKernel.length, 1);
            convolver.convolve(restored_proc, vKernel, 1, vKernel.length);
        }
        // 2D mode, abort here; the rest is unnecessary
        if (is.getSize() == 1) {
            return;
        }

        kernel_radius = vKernel.length / 2;
        // to speed up the method, store the processor in an array (not invoke getProcessor()):
        final float[][] vOrigProcessors = new float[nSlices][];
        final float[][] vRestoredProcessors = new float[nSlices][];
        for (int s = 0; s < nSlices; s++) {
            vOrigProcessors[s] = (float[]) is.getProcessor(s + 1).getPixelsCopy();
            vRestoredProcessors[s] = (float[]) is.getProcessor(s + 1).getPixels();
        }
        // convolution with 1D gaussian in 3rd dimension:
        for (int y = kernel_radius; y < is.getHeight() - kernel_radius; y++) {
            for (int x = kernel_radius; x < is.getWidth() - kernel_radius; x++) {
                for (int s = kernel_radius + 1; s <= is.getSize() - kernel_radius; s++) {
                    float sum = 0;
                    for (int i = -kernel_radius; i <= kernel_radius; i++) {
                        sum += vKernel[i + kernel_radius] * vOrigProcessors[s + i - 1][y * vWidth + x];
                    }
                    vRestoredProcessors[s - 1][y * vWidth + x] = sum;
                }
            }
        }
    }

    private void boxCarBackgroundSubtractor(ImageStack is) {
        final Convolver convolver = new Convolver();
        final float[] kernel = new float[iRadius * 2 + 1];
        final int n = kernel.length;
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = 1f / n;
        }
        for (int s = 1; s <= is.getSize(); s++) {
            final ImageProcessor bg_proc = is.getProcessor(s).duplicate();
            convolver.convolveFloat(bg_proc, kernel, 1, n);
            convolver.convolveFloat(bg_proc, kernel, n, 1);
            is.getProcessor(s).copyBits(bg_proc, 0, 0, Blitter.SUBTRACT);
        }
    }

    private float[] CalculateNormalizedGaussKernel(float aSigma) {
        int vL = (int) aSigma * 3 * 2 + 1;
        if (vL < 3) {
            vL = 3;
        }
        final float[] vKernel = new float[vL];
        final int vM = vKernel.length / 2;
        for (int vI = 0; vI < vM; vI++) {
            vKernel[vI] = (float) (1f / (2f * Math.PI * aSigma * aSigma) * Math.exp(-(float) ((vM - vI) * (vM - vI)) / (2f * aSigma * aSigma)));
            vKernel[vKernel.length - vI - 1] = vKernel[vI];
        }
        vKernel[vM] = (float) (1f / (2f * Math.PI * aSigma * aSigma));

        // normalize the kernel numerically:
        float vSum = 0;
        for (int vI = 0; vI < vKernel.length; vI++) {
            vSum += vKernel[vI];
        }
        final float vScale = 1.0f / vSum;
        for (int vI = 0; vI < vKernel.length; vI++) {
            vKernel[vI] *= vScale;
        }
        return vKernel;
    }

    /**
     * (Re)Initialize the binary and weighted masks. This is necessary if the radius changed.
     * @param mask_radius
     */
    private void generateDilationMasks(int mask_radius) {
        mask = MosaicImageProcessingTools.generateMask(mask_radius);
    }

    /**
     * Sets user defined params that are necessary to particle detection
     * and generates the <code>mask</code> according to these params
     * @return 
     * 
     * @see #generateDilationMasks(int)
     */
    public boolean setDetectionParameters(double cutoff, float percentile, int radius, float Threshold, boolean absolute) {
        final boolean changed = (radius != iRadius || cutoff != iCutoff || (percentile != iPercentile));// && intThreshold != absIntensityThreshold || mode != getThresholdMode() || thsmode != getThresholdMode();
        
        iCutoff = cutoff;
        iPercentile = percentile;
        iAbsIntensityThreshold = Threshold;
        iRadius = radius;
        if (absolute == true) {
            iThresholdMode = Mode.ABS_THRESHOLD_MODE;
        }
        else {
            iThresholdMode = Mode.PERCENTILE_MODE;
        }

        // create Mask for Dilation with the user defined radius
        generateDilationMasks(iRadius);
        
        return changed;
    }

    public int getRadius() {
        return iRadius;
    }
    
    public Mode getThresholdMode() {
        return iThresholdMode;
    }
    
    public float getGlobalMax() {
        return iGlobalMax;
    }
    
    public float getGlobalMin() {
        return iGlobalMin;
    }

    public void setGlobalMax(float aValue) {
        iGlobalMax = aValue;
    }

    public void setGlobalMin(float aValue) {
        iGlobalMin = aValue;
    }
    
    public double getCutoff() {
        return iCutoff;
    }

    public float getPercentile() {
        return iPercentile;
    }
    
    public float getAbsIntensityThreshold() {
        return iAbsIntensityThreshold;
    }
}
