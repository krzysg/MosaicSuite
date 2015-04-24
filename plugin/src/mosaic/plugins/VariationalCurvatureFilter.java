package mosaic.plugins;


import ij.gui.GenericDialog;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mosaic.variationalCurvatureFilters.CurvatureFilter;
import mosaic.variationalCurvatureFilters.FilterKernel;
import mosaic.variationalCurvatureFilters.FilterKernelGc;
import mosaic.variationalCurvatureFilters.FilterKernelMc;
import mosaic.variationalCurvatureFilters.FilterKernelTv;
import mosaic.variationalCurvatureFilters.NoSplitFilter;
import mosaic.variationalCurvatureFilters.SplitFilter;

/**
 * Plugin providing variational curvature filters (GC/TV/MC) functionality for ImageJ/Fiji
 * @author Krzysztof Gonciarz
 */
public class VariationalCurvatureFilter extends PluginBase {
    // Chosen filter
    CurvatureFilter iCf;
    
    // Number of iterations to run filter
    private int iNumberOfIterations;

    // Image dimensions
    int originalWidth;
    int originalHeight;
    int roundedWidth;
    int roundedHeight;
    
    /**
     * Takes information from user about wanted filterType, filtering method and
     * number of iterations.
     * @return true in case if configuration was successful - false otherwise.
     */
    @Override
    boolean showDialog() {
        final String[] filters = {"GC", "MC", "TV"};
        final String[] types = {"Split", "No split"};
        
        GenericDialog gd = new GenericDialog("Curvature Filter Settings");
    
        gd.addRadioButtonGroup("Filter type: ", filters, 1, 3, filters[0]);
        gd.addRadioButtonGroup("Method: ", types, 1, 2, types[1]);
        gd.addNumericField("Number of iterations: ", 10, 0);
        
        gd.showDialog();
        
        if (!gd.wasCanceled()) {
            // Create user's chosen filter
            String filter = gd.getNextRadioButton();
            String type = gd.getNextRadioButton();
            iNumberOfIterations = (int)gd.getNextNumber();
            FilterKernel fk = null;
            if (filter.equals(filters[0])) {
                fk = new FilterKernelGc(); 
            } 
            else if (filter.equals(filters[1])) {
                fk = new FilterKernelMc(); 
            }
            else if (filter.equals(filters[2])) {
                fk = new FilterKernelTv(); 
            }
            if (fk == null) return false;
            
            if (type.equals(types[0])) {
                iCf = new SplitFilter(fk); 
            }
            else {
                iCf = new NoSplitFilter(fk); 
            }
            
            if (iCf != null && iNumberOfIterations >= 0) return true;
        }
        
        return false;
    }

    /**
     * Run filter on given image.
     * @param aOutputIp Image with result
     * @param aOriginalIp Input image
     * @param aFilter Filter to be used
     * @param aNumberOfIterations Number of iterations for filter
     */
    private void filterImage(ImageProcessor aOutputIp, ImageProcessor aOriginalIp, CurvatureFilter aFilter, int aNumberOfIterations) {
        // Get dimensions of input image
        originalWidth = aOriginalIp.getWidth();
        originalHeight = aOriginalIp.getHeight();
        
        // Generate 2D array for image (it will be rounded up to be divisible
        // by 2). Possible additional points will be filled with last column/row
        // values in convertToArrayAndNormalize
        roundedWidth = (int) (Math.ceil(originalWidth/2.0) * 2);
        roundedHeight = (int) (Math.ceil(originalHeight/2.0) * 2);
        float[][] img = new float[roundedHeight][roundedWidth]; 

        // create (normalized) 2D array with input image
        float maxValueOfPixel = (float) aOriginalIp.getMax();
        if (maxValueOfPixel < 1.0f) maxValueOfPixel = 1.0f;
        convertToArrayAndNormalize(aOriginalIp, img, maxValueOfPixel);
        
        // Run chosen filter on image      
        aFilter.runFilter(img, aNumberOfIterations);

        // Update input image with a result
        updateOriginal(aOutputIp, img, maxValueOfPixel);
    }

    /**
     * Converts ImageProcessor to 2D array with first dim Y and second X
     * If new image array is bigger than input image then additional pixels (right column(s) and
     * bottom row(s)) are padded with neighbors values.
     * All pixels are normalized by dividing them by provided normalization value (if 
     * this step is not needed 1.0 should be given).
     * 
     * @param aInputIp       Original image
     * @param aNewImgArray   Created 2D array to keep converted original image     
     * @param aNormalizationValue Maximum pixel value of original image -> converted one will be normalized [0..1]
     */
    private void convertToArrayAndNormalize(ImageProcessor aInputIp, float[][] aNewImgArray, float aNormalizationValue) {
        float[] pixels = (float[])aInputIp.getPixels();
    
        for (int y = 0; y < roundedHeight; ++y) {
            for (int x = 0; x < roundedWidth; ++x) {
                int yIdx = y;
                int xIdx = x;
                if (yIdx >= originalHeight) yIdx = originalHeight - 1;
                if (xIdx >= originalWidth) xIdx = originalWidth - 1;
                aNewImgArray[y][x] = (float)pixels[xIdx + yIdx * originalWidth]/aNormalizationValue;
    
            }
        }
    }

    /**
     * Updates ImageProcessor image with provided 2D pixel array. All pixels are multiplied by
     * normalization value (if this step is not needed 1.0 should be provided)
     * If output image is smaller than pixel array then it is truncated.
     * 
     * @param aIp                  ImageProcessor to be updated
     * @param aImg                 2D array (first dim Y, second X)
     * @param aNormalizationValue  Normalization value.
     */
    private void updateOriginal(ImageProcessor aIp, float[][] aImg, float aNormalizationValue) {
        float[] pixels = (float[])aIp.getPixels();
        
        for (int x = 0; x < originalWidth; ++x) {
            for (int y = 0; y < originalHeight; ++y) {
                     pixels[x + y * originalWidth] = aImg[y][x] * aNormalizationValue;
            }
        }
    }

    @Override
    void setup(String aArgs) {
        if (aArgs.equals("updateOriginal")) setChangeOriginal(true);
        setFilePrefix("filtered_");
    }

    @Override
    void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
        filterImage(aOutputImg, aOrigImg, iCf, iNumberOfIterations);
        
    }
}
