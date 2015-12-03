package mosaic.ia.utils;


import java.util.Vector;

import javax.vecmath.Point3d;

import weka.estimators.KernelEstimator;


public abstract class DistanceCalculations {

    // Input parameters
    private final double iDeltaStepLenght;
    private final double iKernelWeight;
    private final int iNumberOfBins;
    private final float[][][] iMaskImage3d; //[z][x][y]

    // These guys should be set in derived classes
    protected Point3d[] iParticlesX;
    protected Point3d[] iParticlesY;
    protected double zscale = 1;
    protected double xscale = 1;
    protected double yscale = 1;

    // Internal data structures
    private double[] iDistances;
    private double[] iDistancesDistribution;
    private double[] iProbabilityDistribution;

    DistanceCalculations(float[][][] mask, double aDeltaStepLength, double aKernelWeight, int aNumberOfBins) {
        iDeltaStepLenght = aDeltaStepLength;
        iKernelWeight = aKernelWeight;
        iNumberOfBins = aNumberOfBins;
        iMaskImage3d = mask;
    }
    
    public double[] getProbabilityDistribution() {
        return iProbabilityDistribution;
    }
    
    public double[] getDistancesDistribution() {
        return iDistancesDistribution;
    }

    public double[] getDistancesOfX() {
        return iDistances;
    }

    protected void stateDensity(double aMinX, double aMaxX, double aMinY, double aMaxY, double aMinZ, double aMaxZ) {
        final int x_size = (int) Math.floor(Math.abs(aMinX - aMaxX) * xscale / iDeltaStepLenght) + 1;
        final int y_size = (int) Math.floor(Math.abs(aMinY - aMaxY) * yscale / iDeltaStepLenght) + 1;
        final int z_size = (int) Math.floor(Math.abs(aMinZ - aMaxZ) * zscale / iDeltaStepLenght) + 1;

        System.out.println("Number of grid points (x/y/z): " + x_size + " / " + y_size + " / " + z_size);

        final KDTreeNearestNeighbor nearestNeighbor = new KDTreeNearestNeighbor(iParticlesY);
        final KernelEstimator kernelEstimator = new KernelEstimator(0.01 /* precision */);
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;
        Point3d position = new Point3d();
        
        for (int i = 0; i < x_size; i++) {
            for (int j = 0; j < y_size; j++) {
                for (int k = 0; k < z_size; k++) {
                    try {
                        position.x = aMinX + i * iDeltaStepLenght;
                        position.y = aMinY + j * iDeltaStepLenght;
                        position.z = aMinZ + k * iDeltaStepLenght;
                        
                        // Skip points from outside of mask (if given)
                        if (iMaskImage3d != null && !isInsideMask(position)) continue;
                        
                        double distance = nearestNeighbor.getNearestNeighborDistance(position);
                        kernelEstimator.addValue(distance, iKernelWeight);
                        if (distance > max) max = distance;
                        if (distance < min) min = distance;
                    }
                    catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        final double binLength = (max - min) / iNumberOfBins;
        System.out.println("Maximum/Minimum distance found: " + max + " " + min);
        System.out.println("Bin lenght: " + binLength);

        iDistancesDistribution = new double[iNumberOfBins];
        iProbabilityDistribution = new double[iNumberOfBins];
        for (int i = 0; i < iNumberOfBins; i++) {
            iDistancesDistribution[i] = i * binLength;
            iProbabilityDistribution[i] = kernelEstimator.getProbability(i * binLength);
        }
    }

    protected void calcDistancesOfXtoY() {
        System.out.println("Number of coordinates (X/Y): " + iParticlesX.length + " / " + iParticlesY.length);
        iDistances = new KDTreeNearestNeighbor(iParticlesY).getNearestNeighborDistances(iParticlesX);
    }

    private boolean isInsideMask(Point3d coords) {
        try {
            if (iMaskImage3d[(int) coords.z][(int) coords.x][(int) coords.y] > 0) {
                return true;
            }
        }
        catch (final ArrayIndexOutOfBoundsException e) {
            System.out.println("FAULT: " + coords);
            // May happen if mask is applied to loaded coordinates. In that case checks are not done.
            return false;
        }

        return false;
    }
    
    protected Point3d[] getFilteredAndScaledCoordinates(Point3d[] points) {
        final Vector<Point3d> vectorPoints = new Vector<Point3d>();
        for (Point3d p : points) {
            if (iMaskImage3d == null || isInsideMask(p)) {
                vectorPoints.add(new Point3d(p.x * xscale, p.y * yscale, p.z * zscale));
            }
        }
        return vectorPoints.toArray(new Point3d[0]);
    }
}
