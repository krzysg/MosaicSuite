package mosaic.regresionAnalysis;

import mosaic.math.Matrix;

public class GlmGaussian implements Glm {

	@Override
	public double link(double aX) {
		return aX;
	}

	@Override
	public double linkDerivative(double aX) {
		return 1;
	}

	@Override
	public double linkInverse(double aX) {
		return aX;
	}

	@Override
	public double varFunction(double aX) {
		return 1;
	}

	@Override
	public double nllMean(Matrix aImage, Matrix aMu, Matrix aWeights) {
		// nll = weights.*(image-mu).^2;
		// snll = sum(nll(:));
		Matrix snll = new Matrix(aWeights).elementMult( (new Matrix(aImage).sub(aMu)).pow2() );
		return snll.sum();
	}

	@Override
	public NoiseType flag() {
		return NoiseType.GAUSSIAN;
	}
}
