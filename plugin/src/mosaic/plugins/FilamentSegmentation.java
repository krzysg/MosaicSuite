package mosaic.plugins;

import ij.process.FloatProcessor;
import mosaic.nurbs.BSplineSurface;
import mosaic.nurbs.BSplineSurfaceFactory;
import mosaic.nurbs.Function;
import mosaic.plugins.utils.ImgUtils;
import mosaic.plugins.utils.ImgUtils.MinMax;
import mosaic.plugins.utils.PlugInFloatBase;

public class FilamentSegmentation extends PlugInFloatBase {
	// Distance between control points 
	private int iCoefficientStep;

	protected void segmentation(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		// Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();
        int stepSize = (int) Math.pow(2, iCoefficientStep);
        int width = (int) (Math.ceil((float)originalWidth/stepSize) * stepSize);
        int height = (int) (Math.ceil((float)originalHeight/stepSize) * stepSize);        
        float[][] img = new float[width][height];

        // Convert to array and normalize to 0..1 values range
        ImgUtils.ImgToXY2Darray(aOrigImg, img, 1.0f);
        MinMax<Float> minMax = ImgUtils.findMinMax(img);
        ImgUtils.normalize(img);
        
        
        // TODO: segmentation goes here...
        float scale = 1f;
        
        System.out.println("function jav()");
        System.out.println("figure(1); hold off");
        BSplineSurface phi = generatePhi(88, 73, scale);
        phi.showMatlabCode(1, 1);
        phi.showMatlabCode((88/50)*1/scale, (73/50) *1/scale);
        System.out.println("hold on");
        BSplineSurface psi = generatePsi(88, 73, scale);
        psi.showMatlabCode((88/50)*1/scale, (73/50) *1/scale);
        System.out.println("view([90 0])");
        System.out.println("end");

        
        
        // Convert array to Image with converting back range of values
        ImgUtils.convertRange(img, minMax.getMax() - minMax.getMin(), minMax.getMin());
        ImgUtils.XY2DarrayToImg(img, aOutputImg, 1.0f);
	}

	/**
	 * Generate Phi function needed for segmentation
	 * @param aX
	 * @param aY
	 * @param aStep
	 * @return
	 */
	BSplineSurface generatePhi(int aX, int aY, float aStep) {
		final float min = (aX < aY) ? aX : aY ;
		final float uMax = aX;
		final float vMax = aY;
			
		return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aStep, 3, new Function() {
					@Override
					public float getValue(float u, float v) {
						return min/4 - (float) Math.sqrt(Math.pow(v - vMax/2, 2) + Math.pow(u - uMax/2, 2));
					}
				}).normalizeCoefficients();
	}
	
	/**
	 * Generate Psi function needed for segmentation
	 * @param aX
	 * @param aY
	 * @param aStep
	 * @return
	 */
	BSplineSurface generatePsi(int aX, int aY, float aStep) {
		final float min = (aX < aY) ? aX : aY ;
		final float uMax = aX;
		final float vMax = aY;
			
		return BSplineSurfaceFactory.generateFromFunction(1.0f, uMax, 1.0f, vMax, aStep, 3, new Function() {
					@Override
					public float getValue(float u, float v) {
						return min/3 - (float)Math.sqrt(Math.pow(v - vMax/3, 2) + Math.pow(u - uMax/3, 2));
					}
				}).normalizeCoefficients();
	}
	
	@Override
	protected boolean showDialog() {
		// TODO: this data should be handled in dialog window, hard-coded in a meantime
		
		// Should take values from 0..4 -> distance between control points is then 2**scale => 1..16
		iCoefficientStep = 5;
		
		return true;
	}

	@Override
	protected boolean setup(String aArgs) {
		return true;
	}

	@Override
	protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg) {
		segmentation(aOutputImg, aOrigImg);
	}

}
