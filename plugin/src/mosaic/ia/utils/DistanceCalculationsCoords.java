package mosaic.ia.utils;


import javax.vecmath.Point3d;

public class DistanceCalculationsCoords extends DistanceCalculations {

    public DistanceCalculationsCoords(Point3d[] X, Point3d[] Y, float[][][] mask, double xmin, double ymin, double zmin, double xmax, double ymax, double zmax, double gridSize, double kernelWeightq, int discretizationSize) {
        super(mask, gridSize, kernelWeightq, discretizationSize);
        this.X = X;
        this.Y = Y;
        x1 = xmin;
        y1 = ymin;
        z1 = zmin;
        y2 = ymax;
        x2 = xmax;
        z2 = zmax;
        
        calcDistances();
    }

    private final Point3d[] X, Y; // unfiltered points
    private final double x1, x2, y1, y2, z1, z2; // ask for users input, if no mask. currently, force mask for csv.

    private void calcDistances() {
        iParticlesX = getFilteredAndScaledCoordinates(X);
        iParticlesY = getFilteredAndScaledCoordinates(Y);
        stateDensity(x1, x2, y1, y2, z1, z2);
        calcDistancesOfXtoY();
    }
}
