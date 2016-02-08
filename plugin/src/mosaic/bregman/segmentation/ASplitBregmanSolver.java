package mosaic.bregman.segmentation;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ij.IJ;
import mosaic.bregman.segmentation.SegmentationParameters.IntensityMode;
import mosaic.bregman.segmentation.SegmentationParameters.NoiseModel;
import mosaic.core.psf.psf;
import net.imglib2.type.numeric.real.DoubleType;


abstract class ASplitBregmanSolver {
    // Input parameters
    protected final SegmentationParameters iParameters;
    protected final double[][][] iImage;
    private final AnalysePatch iAnalysePatch;
    protected double iBetaMleOut;
    protected double iBetaMleIn;
    final double iRegularization;
    protected final psf<DoubleType> iPsf;

    // Internal data
    protected final int ni, nj, nz;
    protected final Tools iLocalTools;
    protected final ExecutorService executor;
    protected final double iEnergies[];
    // betaMleOut/betaMleIn are being updated in 2D case but not in 3D. In 3d only returned
    // getBetaMLE() is based on updated stuff
    final double[] betaMle = new double[2];

    // Segmentation masks
    protected final double[][][] w3k;
    protected final double[][][] w3kbest;
    
    // Used by superclasses and utils
    protected final NoiseModel iNoiseModel;
    protected final double[][][] w1k;
    protected final double[][][] w2xk;
    protected final double[][][] w2yk;
    protected final double[][][] b2xk;
    protected final double[][][] b2yk;
    protected final double[][][] b1k;
    protected final double[][][] b3k;
    protected double[][][] temp1;
    protected double[][][] temp2;
    protected double[][][] temp3;
    protected double[][][] temp4;
    
    
    ASplitBregmanSolver(SegmentationParameters aParameters, double[][][] aImage, double[][][] aMask, AnalysePatch aAnalazePatch, double aBetaMleOut, double aBetaMleIn, double aRegularization, psf<DoubleType> aPsf) {
        iParameters = aParameters;
        iImage = aImage;
        iAnalysePatch = aAnalazePatch;
        iBetaMleOut = aBetaMleOut;
        iBetaMleIn = aBetaMleIn;
        iRegularization = aRegularization;
        iPsf = aPsf;
        ni = aImage[0].length; 
        nj = aImage[0][0].length;
        nz = aImage.length; 

        w3k = new double[nz][ni][nj];
        Tools.copytab(w3k, aMask);
        w3kbest = new double[nz][ni][nj];
        
        iLocalTools = new Tools(ni, nj, nz);
        executor = Executors.newFixedThreadPool(iParameters.numOfThreads);
        iEnergies = new double[iParameters.numOfThreads];
        betaMle[0] = iBetaMleOut;
        betaMle[1] = iBetaMleIn;
        
        iNoiseModel = iParameters.noiseModel;
        w1k = new double[nz][ni][nj];
        b2xk = new double[nz][ni][nj];
        b2yk = new double[nz][ni][nj];
        b1k = new double[nz][ni][nj];
        b3k = new double[nz][ni][nj];
        w2xk = new double[nz][ni][nj];
        w2yk = new double[nz][ni][nj];
        temp1 = new double[nz][ni][nj];
        temp2 = new double[nz][ni][nj];
        temp3 = new double[nz][ni][nj];
        temp4 = new double[nz][ni][nj];
    }

    final double getBetaMleIn() {
        return betaMle[1];
    }

    final void first_run() {
        final int firstStepNumOfIterations = 151;
        try {
            run(true, firstStepNumOfIterations);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    final void second_run() {
        final int secondStepNumOfIterations = 101;
        try {
            run(false, secondStepNumOfIterations);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private final void run(boolean aFirstPhase, int aNumOfIterations) throws InterruptedException {
        int stepk = 0;
        final int modulo = 10;
        int bestIteration = 0;
        boolean stopFlag = false;
        double bestEnergy = Double.MAX_VALUE;
        double lastenergy = 0;
        double energy = 0;
        
        while (stepk < aNumOfIterations && !stopFlag) {
            final boolean lastIteration = (stepk == aNumOfIterations - 1);
            final boolean energyEvaluation = (stepk % iParameters.energyEvaluationModulo == 0);
            final boolean moduloStep = (stepk % modulo == 0 || lastIteration);

            step(energyEvaluation, lastIteration);

            if (energyEvaluation) {
                energy = 0;
                for (int nt = 0; nt < iParameters.numOfThreads; nt++) {
                    energy += iEnergies[nt];
                }
            }
            
            if (energy < bestEnergy) {
                Tools.copytab(w3kbest, w3k);
                bestIteration = stepk;
                bestEnergy = energy;
            }
            
            if (moduloStep && stepk != 0) {
                if (Math.abs((energy - lastenergy) / lastenergy) < iParameters.energySearchThreshold) {
                    stopFlag = true;
                }
            }
            lastenergy = energy;

            if (aFirstPhase) {
                if (moduloStep) {
                    if (iParameters.debug) {
                        IJ.log(String.format("Energy at step %d: %7.6e", stepk, energy));
                        if (stopFlag) IJ.log("energy stop");
                    }
                    IJ.showStatus("Computing segmentation  " + Tools.round((50 * stepk)/(aNumOfIterations - 1), 2) + "%");
                }
                IJ.showProgress(0.5 * (stepk) / (aNumOfIterations - 1));
            }
            else {
                if (iParameters.intensityMode == IntensityMode.AUTOMATIC && (stepk == 40 || stepk == 70)) {
                    iAnalysePatch.estimateIntensity(w3k);
                    betaMle[0] = Math.max(0, iAnalysePatch.cout);
                    betaMle[1] = Math.max(0.75 * iAnalysePatch.iNormalizedMinObjectIntensity, iAnalysePatch.cin);
                    init();
                    if (iParameters.debug) {
                        IJ.log("region" + iAnalysePatch.iInputRegion.iLabel + String.format(" Photometry :%n background %10.8e %n foreground %10.8e", iAnalysePatch.cout, iAnalysePatch.cin));
                    }
                }
            }

            stepk++;
        }

        if (bestIteration < 50) { // use what iteration threshold ?
            Tools.copytab(w3kbest, w3k);
            bestIteration = stepk - 1;
            bestEnergy = energy;
            
            if (aFirstPhase) {
                if (iParameters.debug) {
                    IJ.log("Warning : increasing energy. Last computed mask is then used for first phase object segmentation." + bestIteration);
                }
            }
        }
        if (aFirstPhase) { 
            if (iParameters.debug) {
                IJ.log("Best energy : " + Tools.round(bestEnergy, 3) + ", found at step " + bestIteration);
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    abstract protected void step(boolean aEvaluateEnergy, boolean aLastIteration) throws InterruptedException;
    abstract protected void init();
}
