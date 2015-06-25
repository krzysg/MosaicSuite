package mosaic.math;

import mosaic.generalizedLinearModel.Glm;
import java.lang.Math;


/**
 * Region Statistics Solver class. Based on:
 * 
 * G. Paul, J. Cardinale, and I. F. Sbalzarini, 
 * “Coupling image restoration and segmentation: A generalized linear model/Bregman perspective,” 
 * Intl. J. Comp. Vis., pp. 1–25, 2013.
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class RegionStatisticsSolver {
    private Matrix iImage;
    private Matrix iMask;
    private Matrix iMu;
    private Glm iGlm;
    private int iNumOfIterations;
    
    // Calculated beta
    private double iBetaMLEout = 0.0;
    private double iBetaMLEin = 0.0;
    
    // Calculated model image
    private Matrix iModelImage;
    
    /**
     * RegionStatisticsSolver calculates maximum likelihood estimator (MLE) and resulting model image
     * @param aImage Input image
     * @param aMask Input mask
     * @param aGlm Generalized linear mode (Gauss, Poisson)
     * @param aMuInit Initial value of Model Image (usually equal to input image)
     * @param aNumOfIterations Number of iterations
     */
    public RegionStatisticsSolver(Matrix aImage, Matrix aMask, Glm aGlm, Matrix aMuInit, int aNumOfIterations) {
        iImage = aImage;
        iMask = aMask;
        iMu = aMuInit;
        iGlm = aGlm;
        iNumOfIterations = aNumOfIterations;
    }

    /**
     * Calculates beta MLE and model image
     * @return this
     */
    public RegionStatisticsSolver calculate() {
        // Precalculate some helpers
        
        // All ones with size of iMask
        Matrix ones = iMask.copy().ones();
        // (1 - iMask)
        Matrix maskSubFrom1 = ones.copy().sub(iMask);
        // iMask.^2
        Matrix maskPow2 = iMask.copy().pow2();
        // (1 - iMask).*iMask
        Matrix maskMul1SubMask = maskSubFrom1.copy().elementMult(iMask);
        // (1 - iMask).^2
        Matrix pow2Of1SubMask = maskSubFrom1.copy().pow2();

        Matrix mu = iMu.copy();
        Matrix Z = calcualteZ(mu);
        Matrix W = calculateW(mu);
        
        for (int i = 0; i < iNumOfIterations; ++i) {
            double K11 = W.copy().elementMult(pow2Of1SubMask).sum();
            double K12 = W.copy().elementMult(maskMul1SubMask).sum();
            double K22 = W.copy().elementMult(maskPow2).sum();
            
            double U1 = W.copy().elementMult(Z).elementMult(maskSubFrom1).sum();
            double U2 = W.copy().elementMult(Z).elementMult(iMask).sum();
            
            double detK = K11 * K22 - Math.pow(K12, 2);
            iBetaMLEout = (K22*U1-K12*U2)/detK;
            iBetaMLEin = (-K12*U1+K11*U2)/detK;

            mu = calculateModelImage();
            Z = calcualteZ(mu);
            W = calculateW(mu);
        }
        
        if (iBetaMLEout > iBetaMLEin) {
            double temp = iBetaMLEout;
            iBetaMLEout = iBetaMLEin;
            iBetaMLEin = temp;
        }
        
        mu = calculateModelImage();
        iModelImage = mu;
        
        return this;
    }

    public double getBetaMLEout() {
        return iBetaMLEout;
    }
    
    public double getBetaMLEin() {
        return iBetaMLEin;
    }
    
    public Matrix getModelImage() {
        return iModelImage;
    }

    private Matrix calculateW(Matrix mu) {
        // Matlab: priorWeights./(varFunction(mu).*linkDerivative(mu).^2+eps);
        return iGlm.priorWeights(iImage).elementDiv( iGlm.varFunction(mu).elementMult(iGlm.linkDerivative(mu).pow2()).add(Math.ulp(1.0)) );
    }

    private Matrix calcualteZ(Matrix mu) {
        // Matlab: link(mu)+(image-mu).*linkDerivative(mu);
        return iImage.copy().sub(mu).elementMult(iGlm.linkDerivative(mu)).add(iGlm.link(mu));
    }

    private Matrix calculateModelImage() {
        // Matlab: linearPredictor = (betaMLE_in-betaMLE_out)*KMask+betaMLE_out; 
        //         mu  = linkInverse(linearPredictor);
        Matrix linearPredictor = iMask.copy().scale(iBetaMLEin - iBetaMLEout).add(iBetaMLEout);
        return iGlm.linkInverse(linearPredictor);
    }
    
}
