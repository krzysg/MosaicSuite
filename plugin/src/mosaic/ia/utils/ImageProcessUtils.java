package mosaic.ia.utils;


import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.Duplicator;
import ij.plugin.Macro_Runner;
import ij.process.ImageStatistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.vecmath.Point3d;

import mosaic.core.detection.FeaturePointDetector;
import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.ia.nn.KDTreeNearestNeighbor;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;


public class ImageProcessUtils {

    /**
     * Detect the particle in a stack from an image using particle tracker
     *
     * @param imp Image
     * @return Vector of particle detected
     */

    public static Vector<Particle> detectParticlesinStack(ImagePlus imp) {
        switch (imp.getType()) {
            case ImagePlus.GRAY8: {
                return ImageProcessUtils.<UnsignedByteType> detectParticlesinStackType(imp);
            }
            case ImagePlus.GRAY16: {
                return ImageProcessUtils.<UnsignedShortType> detectParticlesinStackType(imp);
            }
            case ImagePlus.GRAY32: {
                return ImageProcessUtils.<FloatType> detectParticlesinStackType(imp);
            }
            default: {
                IJ.error("Incompatible image type convert to 8-bit 16-bit or Float type");
                return null;
            }
        }

    }

    /**
     * Detect the particle in a stack from an image using particle tracker
     * The generic parameter is the type of the image
     *
     * @param imp Image
     * @return Vector of particle detected
     */

    private static <T extends RealType<T> & NativeType<T>> Vector<Particle> detectParticlesinStackType(ImagePlus imp) {
        // init the circle cache
        MyFrame.initCache();
        final ImageStatistics imageStat = imp.getStatistics();
        final FeaturePointDetector featurePointDetector = new FeaturePointDetector((float) imageStat.max, (float) imageStat.min);

        final GenericDialog gd = new GenericDialog("Particle Detection...", IJ.getInstance());

        System.out.println("No of N slices: " + imp.getNSlices());

        featurePointDetector.addUserDefinedParametersDialog(gd);

        // gd.addPanel(detector.makePreviewPanel(this, impA),
        // GridBagConstraints.CENTER, new Insets(5, 0, 0, 0));

        // show the image

        imp.show();

        // featurePointDetector.generatePreviewCanvas(imp);
        gd.showDialog();
        featurePointDetector.getUserDefinedParameters(gd);
        final Img<T> background = ImagePlusAdapter.wrap(imp);
        featurePointDetector.preview(imp, gd);
        final MyFrame myFrame = featurePointDetector.getPreviewFrame();

        myFrame.setParticleRadius(featurePointDetector.radius);
        final Img<ARGBType> detected = myFrame.createImage(background, imp.getCalibration());
        ImageJFunctions.show(detected);

        /* previewCanvas.repaint(); */
        return myFrame.getParticles();
    }

    /**
     * Get the coordinate of the Particles
     *
     * @param particles
     * @return
     */

    public static Point3d[] getCoordinates(Vector<Particle> particles) {
        double[] tempPosition = new double[3];
        final Point3d[] tempCoords = new Point3d[particles.size()];

        final Iterator<Particle> iter = particles.iterator();
        int i = 0;
        while (iter.hasNext()) {
            tempPosition = iter.next().getPosition();
            // System.out.println("position: "+tempPosition[0]+tempPosition[1]+tempPosition[2]);
            try {
                tempCoords[i] = new Point3d(tempPosition); // duplicate
                // initialization?
                // System.out.println(tempPosition[2]);
            }
            catch (final NullPointerException e) {
                e.printStackTrace();
                System.out.println("i= " + i + ", size:" + tempCoords.length + " temp position 0: " + tempPosition[0]);
            }
            i++;
        }

        return tempCoords;

    }

    public static double[] KDTreeDistCalc(Point3d[] X, Point3d[] Y) {
        // saveCoordinates(X, Y);
        final KDTreeNearestNeighbor kdtnn = new KDTreeNearestNeighbor();
        System.out.println("Size of X:" + X.length);
        System.out.println("Size of Y:" + Y.length);
        kdtnn.createKDTree(Y);
        return kdtnn.getNNDistances(X);

    }

    public static ImagePlus generateMask(ImagePlus image) {
        final Duplicator dupe = new Duplicator();
        final ImagePlus mask = dupe.run(image);

        mask.show();
        new Macro_Runner().run("JAR:src/mosaic/plugins/scripts/GenerateMask_.ijm");
        mask.changes = false;

        return mask;
    }

    public static ImagePlus openImage(String path) {
        // open image dialog and opens it and returns
        final Opener opener = new Opener();

        return opener.openImage(path);

    }

    public static Point3d[] openCSVFile(String title, String path) {
        // open image dialog and opens it and returns

        final OpenDialog od = new OpenDialog(title, path);
        final File file = new File(od.getDirectory() + od.getFileName());
        BufferedReader CSVFile = null;
        final Vector<Point3d> points = new Vector<Point3d>();
        try {
            CSVFile = new BufferedReader(new FileReader(file));
            if (CSVFile != null) {
                String dataRow = null;
                double xtemp, ytemp, ztemp;
                try {
                    dataRow = CSVFile.readLine();
                }
                catch (final IOException e) {

                    e.printStackTrace();
                }
                String[] dataArray;
                int count = 0, size = 0;
                while (dataRow != null) {
                    dataArray = dataRow.split(",");
                    if (count == 0) {
                        size = dataArray.length;
                        if (size != 2 && size != 3) {
                            return null;
                        }
                        count++;
                    }
                    if (dataArray.length != size) {
                        return null;
                    }

                    xtemp = Double.parseDouble(dataArray[0]);
                    ytemp = Double.parseDouble(dataArray[1]);
                    if (size > 2) {
                        ztemp = Double.parseDouble(dataArray[2]);
                    }
                    else {
                        ztemp = 0f; // 2D -- if only 2 columns, assume 2D.
                    }

                    points.add(new Point3d(xtemp, ytemp, ztemp));

                    try {
                        dataRow = CSVFile.readLine();
                    }
                    catch (final IOException e) {
                        e.printStackTrace();
                    } // Read next line of data.
                }
            }
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (CSVFile != null) CSVFile.close();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }

        return points.toArray(new Point3d[points.size()]);
    }
}
