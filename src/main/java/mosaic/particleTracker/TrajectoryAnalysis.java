package mosaic.particleTracker;

import java.util.ArrayList;
import java.util.List;

import mosaic.core.detection.Particle;
import mosaic.utils.math.LeastSquares;


/**
 *  This class is responsible for processing trajectories basing on methods presented in:
 *  I. F. Sbalzarini. Moments of displacement and their spectrum.
 *  ICoS technical report, Institute of Computational Science (ICoS), ETH Zürich, 2005.
 *
 *   @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
/**
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 *
 */
public class TrajectoryAnalysis {

    private final Particle[] iParticles;     // given trajectory's particles
    private int[] iMomentOrders;             // requested moment orders to be calculated
    private int[] iFrameShifts;              // requested frame shift (deltas)
    private double[][] iMSDs;                // moments of displacement for every moment order
    private double[] iGammasLogarithmic;     // vector of scaling coefficients (slopes) from logarithmic plot
    private double[] iGammasLogarithmicY0;   // vector of y-axis intercept for scaling coefficients
    private double[] iGammasLinear;          // vector of scaling coefficients (slopes) from linear plot
    private double[] iGammasLinearY0;        // vector of y-axis intercept for scaling coefficients
    private double[] iDiffusionCoefficients; // vector of diffusion coefficients
    private double iMSSlinear;               // slope of moments scaling spectrum for linear plot
    private double iMSSlinearY0;             // y-axis intercept of MSS for linear plot
    private double iMSSlogarithmic;          // slope of moments scaling spectrum for logarithmic plot
    private double iMSSlogarithmicY0;        // y-axis intercept of MSS for logarithmic plot
    private double iDX;                      // physical length of a pixel
    private double iDT;                      // physical time interval between frames
    private double iDistance;                // Net displacement
    private double iAvgDistance;             // Net displacement per frame
    private double iStraightness;            // Straighntes of movement
    private double iBending;                 // Bending of movement
    private double iBendingLinear;           // Bending in degrees (sum of all changes between steps)
    private double iEfficiency;              // Movement efficiency

    public static final boolean SUCCESS = true;
    public static final boolean FAILURE = false;

    /**
     * @param aTrajectory Trajectory to be analyzed
     */
    public TrajectoryAnalysis(final Trajectory aTrajectory) {
        this(aTrajectory != null ? aTrajectory.iParticles : null);
    }

    /**
     * @param aParticles Particles to be analyzed
     */
    TrajectoryAnalysis(final Particle[] aParticles) {
        iParticles = aParticles;
        
        // set some default data for calculations, it can be overwritten by user
        if (iParticles != null && iParticles.length > 0) {
            setFrameShifts(1, (iParticles[iParticles.length - 1].getFrame() - iParticles[0].getFrame() + 1)/3);
        }
        setMomentOrders(1, 10);

        iDX = 1.0;
        iDT = 1.0;
    }

    /**
     * Sets orders used to calculate mean displacements in range from [aMin, aMax]
     * @param aMin start index (included)
     * @param aMax stop index (included)
     */
    private void setMomentOrders(final int aMin, final int aMax) {
        if (aMax >= aMin) {
            iMomentOrders = generateArrayRange(aMin, aMax);
        }
    }

    /**
     * Sets user defined orders used to calculate mean displacements.
     * @param aOrders (values should be >= 1)
     */
    public void setMomentOrders(final int[] aOrders) {
        iMomentOrders = aOrders;
    }

    /**
     * @return currently set moment orders
     */
    public int[] getMomentOrders() {
        return iMomentOrders;
    }

    /**
     * Sets frame shifts (deltas) in range from [aMin, aMax]
     * @param aMin start index (included)
     * @param aMax stop index (included)
     */
    private void setFrameShifts(final int aMin, final int aMax) {
        if (aMax >= aMin) {
            iFrameShifts = generateArrayRange(aMin, aMax);
        }
    }

    /**
     * Sets user defined frame shifts (deltas).
     * @param aFrameShifts (delta values should be >= 1)
     */
    public void setFrameShifts(final int[] aFrameShifts) {
        iFrameShifts = aFrameShifts;
    }

    /**
     * @return currently set frame shifts (deltas)
     */
    public int[] getFrameShifts() {
        return iFrameShifts;
    }

    /**
     * Calculates mean displacements, scaling coefficients and slope of moments scaling spectrum.
     * @return This method returns {@link #SUCCESS} or {@link #FAILURE}
     */
    public boolean calculateAll() {
        // It is impossible to calcualte MSS/MSD with less then 6 points
        //(delta is between 1 and numberOfPoints/3)
        // Also frame shifts and moment of orders must be provided.
        if (iFrameShifts != null && iFrameShifts.length >= 1 &&
                iMomentOrders != null && iMomentOrders.length >= 1 &&
                iParticles != null && iParticles.length >= 6) {

            return calculateTrajectoryMotionFeatures() &&
                   calculateMSDs() &&
                   calculateGammasAndDiffusionCoefficients() &&
                   calculateMSS();

        }
        return FAILURE;
    }

    /**
     * @return mean displacement for given index (according to given moment orders)
     */
    double[] getMSDforMomentIdx(final int aMomentIdx) {
        return iMSDs[aMomentIdx];
    }

    /**
     * @return vector of scaling coefficients (slopes) for logarithmic plot
     */
    public double[] getGammasLogarithmic() {
        return iGammasLogarithmic;
    }

    /**
     * @return y-axis intercept of scaling coefficients (slopes) for logarithmic plot
     */
    public double[] getGammasLogarithmicY0() {
        return iGammasLogarithmicY0;
    }

    /**
     * @return vector of scaling coefficients (slopes) for linear plot
     */
    public double[] getGammasLinear() {
        return iGammasLinear;
    }

    /**
     * @return y-axis intercept of scaling coefficients (slopes) for linear plot
     */
    public double[] getGammasLinearY0() {
        return iGammasLinearY0;
    }

    /**
     * @return Diffusion coefficients of all orders.
     *         D2 - corresponds to the regular diffusion constant (order=2 -> array index = 1)
     */
    public double[] getDiffusionCoefficients() {
        return iDiffusionCoefficients;
    }

    /**
     * @return slope of moments scaling spectrum
     */
    public double getMSSlinear() {
        return iMSSlinear;
    }

    /**
     * @return y-axis intercept of mss for linear plot
     */
    public double getMSSlinearY0() {
        return iMSSlinearY0;
    }

    /**
     * @return slope of moments scaling spectrum
     */
    public double getMSSlogarithmic() {
        return iMSSlogarithmic;
    }

    /**
     * @return y-axis intercept of mss for logarithmic plot
     */
    public double getMSSlogarithmicY0() {
        return iMSSlogarithmicY0;
    }

    /**
     * @return Net displacement in meters
     */
    public double getDistance() {
        return iDistance;
    }

    /**
     * @return Net displacement per one frame in meters
     */
    public double getAvgDistance() {
        return iAvgDistance;
    }
    
    /**
     * @return Straightness in range [-1, 1] 
     * @see http://mosaic.mpi-cbg.de/docs/Helmuth2007.pdf for definitions 
     */
    public double getStraightness() {
        return iStraightness;
    }
    
    /**
     * @return Bending in range [-1, 1] 
     * @see http://mosaic.mpi-cbg.de/docs/Helmuth2007.pdf for definitions 
     */
    public double getBending() {
        return iBending;
    }
    
    /**
     * @return Bending as a sum off all degree changes so in reult it gives change in degrees from first to last step. 
     */
    public double getBendingLinear() {
        return iBendingLinear;
    }

    /**
     * @return Efficiency of movement in range [0, 1]
     * @see http://mosaic.mpi-cbg.de/docs/Helmuth2007.pdf for definitions 
     */
    public double getEfficiency() {
        return iEfficiency;
    }
    
    /**
     * Sets a physical length of a pixel in meters. (default 1.0)
     * @param aLength Length of pixel in meters.
     */
    public void setLengthOfAPixel(final double aLength) {
        iDX = aLength;
    }

    /**
     * Sets a physical time interval between frames (default 1.0)
     * @param aInterval Time interval in seconds
     */
    public void setTimeInterval(final double aInterval) {
        iDT = aInterval;
    }
    
    /**
     * @return a physical time interval between frames (default 1.0) in seconds
     */
    public double getTimeInterval() {
        return iDT;
    }

    /**
     * Converts array of double[] to log scale double[]
     * (value of each element is logged and put into output array)
     * @param aVector input array
     * @return Converted array
     */
    double[] toLogScale(final double[] aVector) {
        final double[] result = new double[aVector.length];
        for (int i = 0; i < aVector.length; ++i) {
            result[i] = Math.log(aVector[i]);
        }

        return result;
    }

    /**
     * Converts array of int[] to log scale double[]
     * (value of each element is logged and put into output array)
     * @param aVector input array
     * @return Converted array
     */
    double[] toLogScale(final int[] aVector) {
        final double[] result = new double[aVector.length];
        for (int i = 0; i < aVector.length; ++i) {
            result[i] = Math.log(aVector[i]);
        }

        return result;
    }

    /**
     * Converts array of int[] to double[]
     * @param aValues input array
     * @return Converted array
     */
    double[] toDouble(final int[] aValues) {
        final double[] result = new double[aValues.length];
        for (int i = 0; i < aValues.length; ++i) {
            result[i] = aValues[i];
        }

        return result;
    }

    @Override
    public String toString() {
        String str = String.format("Physical length unit(per pixel): %15.4f\n", iDX);
        str += String.format("Physical time unit(time between frames): %15.4f\n\n", iDT);


        str += "MSDs:\n";
        str += "-----------------------------------\n";

        for (final double[] m : iMSDs) {
            String line = "";
            for (final double d : m) {
                line += String.format("%15.4f ", d);
            }
            str += line + "\n";
        }

        str += "\nGAMMAs:\n";
        str += "-----------------------------------\n";
        String line = "";
        for (final double g : iGammasLogarithmic) {
            line += String.format("%15.4f ", g);
        }
        str += line + "\n";

        str += "\nDiffusion Coefficientss:\n";
        str += "-----------------------------------\n";
        line = "";
        for (final double g : iDiffusionCoefficients) {
            line += String.format("%15.4f ", g);
        }
        str += line + "\n";

        str += "\nMSS:\n";
        str += "-----------------------------------\n";
        str += String.format("%15.4f\n", iMSSlinear);

        return str;
    }

    // **************************************************************************

    /**
     * Calculates mean displacement of order 'aOrder' for a specific frame shift 'aDelta' for
     * a given trajectory 'aTrajectory'
     *
     * @param aDelta frame shift (should be >= 1)
     * @param aOrder order of mean moment. When aOrder=2 then this special case is called
     *               'mean square displacement'
     * @return
     */
    private double meanDisplacement(int aDelta, int aOrder) {
        final int noOfParticles = iParticles.length;

        if (noOfParticles < 2 || aDelta <= 0) {
            // In case when it is impossible to calculate mean moment of order 'aOrder' just
            // return 0

            return 0;
        }

        // Calculate mean moment
        double sum = 0;
        int noOfElements = 0;

        for (int i = 0; i < noOfParticles; ++i) {
            final Particle pi = iParticles[i];

            // It may happen that particle has not been discovered in each frame. Try to
            // find a particle in aDelta distance. For further information about this behavior
            // please refer to 'Link Range' parameter.
            for (int j = i + 1; j < noOfParticles; ++j) {
                final Particle pj = iParticles[j];
                if (pj.getFrame() == pi.getFrame() + aDelta) {
                    final double dx = (pj.iX - pi.iX);
                    final double dy = (pj.iY - pi.iY);

                    // Calculate Euclidean norm to get distance between particles
                    // (also convert distance from pixel based to physical units)
                    // and power it to aOrder
                    sum += Math.pow((dx*dx + dy*dy)*iDX*iDX, aOrder/2.0d);
                    ++noOfElements;

                    // No need to look further
                    break;
                }
                else if (pj.getFrame() > pi.getFrame() + aDelta) {
                    // Particle in aDelta frame-distance has not been found -> continue.
                    break;
                }
            }
        }

        return noOfElements == 0 ? 0 : sum/noOfElements;
    }

    /**
     * Calculates some features of trajectory basing on definitions from:
     * J. A. Helmuth, C. J. Burckhardt, P. Koumoutsakos, U. F. Greber, and I. F. Sbalzarini. 
     * A novel supervised trajectory segmentation algorithm identifies distinct types of human 
     * adenovirus motion in host cells, Journal of Structural Biology, 159(3):347-358, 2007
     * @return SUCCESS if calculations are valid
     */
    private boolean calculateTrajectoryMotionFeatures() {
        final int noOfParticles = iParticles.length;
        iAvgDistance = 0;
        iDistance = 0;
        if (noOfParticles < 2) {
            // In case when it is impossible to calculate just quit
            return SUCCESS;
        }
        
        List<Double> angles = new ArrayList<Double>();
        double prevAngl = 0;
        boolean prevAvaiable = false;
        double efficiencyDenominator = 0;
        Particle pj = iParticles[0];
        for (int i = 1; i < noOfParticles; ++i) {
            final Particle pi = iParticles[i];
            final double dx = (pi.iX - pj.iX);
            final double dy = (pi.iY - pj.iY);
            
            double squaredDist = (dx*dx + dy*dy)*iDX*iDX;
            efficiencyDenominator += squaredDist;
            iDistance += Math.sqrt(squaredDist);
            pj = pi;
            
            // Calculate angle change basing on deltas
            double angle = 0;
            boolean skip = false;
            if (dx == 0 && dy > 0) {
                angle = 0.5 * Math.PI;
            }
            else if (dx == 0 && dy < 0) {
                angle = 1.5 * Math.PI;
            }
            else if (dx > 0 && dy == 0) {
                angle = 0 * Math.PI;
            }
            else if (dx < 0 && dy == 0) {
                angle = 1.0 * Math.PI;
            }
            else if (dx > 0 && dy > 0) {
                angle = Math.atan(dy/dx);
            }
            else if (dx < 0 && dy > 0) {
                angle = Math.PI + Math.atan(dy/dx);
            }
            else if (dx < 0 && dy < 0) {
                angle = Math.PI + Math.atan(dy/dx);
            }
            else if (dx > 0 && dy < 0) {
                angle = 2*Math.PI + Math.atan(dy/dx);
            }
            else {
                // No move has been made comparing to last known position
                skip = true;
            }
            // At that point angle will be in range 0-2pi measured from x axis in counterclockwise direction
            if (!skip) {
                if (prevAvaiable) {
                    angles.add(prevAngl - angle);
                }
                prevAngl = angle;
                prevAvaiable = true;
            }
        }
        iAvgDistance = iDistance / (iParticles[noOfParticles - 1].getFrame() - iParticles[0].getFrame());
        
        // Bending, Straighness
        double straightness = 0;
        double bendingSin = 0;
        double bendingDegrees = 0;
        for (int i = 0; i < angles.size(); ++i) {
            if (angles.get(i) < -Math.PI) angles.set(i, angles.get(i) + 2 * Math.PI);
            else if (angles.get(i) > Math.PI) angles.set(i, angles.get(i) - 2*Math.PI);
            straightness += Math.cos(angles.get(i));
            bendingSin += Math.sin(angles.get(i));
            bendingDegrees += angles.get(i);
        }
        iStraightness = straightness / (angles.size());
        iBending = bendingSin / (angles.size());
        iBendingLinear = bendingDegrees / (angles.size());
        
        // Efficiency
        Particle firstP = iParticles[0];
        final Particle lastP = iParticles[noOfParticles - 1];
        final double dx = (lastP.iX - firstP.iX);
        final double dy = (lastP.iY - firstP.iY);
        double squaredDist = (dx*dx + dy*dy)*iDX*iDX;
        
        iEfficiency = squaredDist / ((noOfParticles - 1) * efficiencyDenominator);
        
        return SUCCESS;
    }
    
    private boolean calculateMSDs() {
        iMSDs = new double[iMomentOrders.length][iFrameShifts.length];

        int orderIdx = 0;
        for (final int order : iMomentOrders) {
            int deltaIdx = 0;
            for (final int delta : iFrameShifts) {
                final double displacement = meanDisplacement(delta, order);
                iMSDs[orderIdx][deltaIdx] = displacement;
                deltaIdx++;
            }
            orderIdx++;
        }

        return SUCCESS;
    }
    
    private boolean calculateGammasAndDiffusionCoefficients() {
        final LeastSquares ls = new LeastSquares();

        final int noOfMoments = iMomentOrders.length;
        iGammasLogarithmic = new double[noOfMoments];
        iGammasLinear = new double[noOfMoments];
        iGammasLogarithmicY0 = new double[noOfMoments];
        iGammasLinearY0 = new double[noOfMoments];
        iDiffusionCoefficients = new double[noOfMoments];

        int gammaIdx = 0;
        for (final double[] m : iMSDs) {
            // Get rid of MSDs equal to 0 (could happen when trajectory has not enough points).
            final double[] moments = new double[m.length];
            final double[] deltas  = new double[m.length];
            int count = 0;
            for (int i = 0; i < iFrameShifts.length; ++ i) {
                if (m[i] != 0.0d) {
                    moments[count] = m[i];
                    deltas[count] = iFrameShifts[i];
                    count++;
                }
            }
            if (count < 2) {
                // it is not possible to do linear regression with less than 2 points.
                return FAILURE;
            }
            final double[] tmpMoments = new double[count];
            final double[] tmpDeltas  = new double[count];
            for (int i = 0; i < count; ++i) {
                tmpMoments[i] = moments[i];
                // Convert it to physical time units
                tmpDeltas[i] = deltas[i] * iDT;
            }


            final double[] mLog = toLogScale(tmpMoments); // moments in log scale
            final double[] dLog = toLogScale(tmpDeltas);  // deltas in log scale
            ls.calculate(dLog, mLog);
            if (Double.isNaN(ls.getAlpha()) || Double.isNaN(ls.getBeta())) {
                // Usually it is a result of not enough number of points in trajectory or
                // missing detections in frames delta*n (for n=0...trajectoryLenght/3)
                return FAILURE;
            }
            iGammasLogarithmic[gammaIdx] = ls.getBeta();
            iGammasLogarithmicY0[gammaIdx] = ls.getAlpha();
            iDiffusionCoefficients[gammaIdx] = 0.25 * Math.exp(ls.getAlpha());
            ls.calculate(tmpDeltas, tmpMoments);
            iGammasLinear[gammaIdx] = ls.getBeta();
            iGammasLinearY0[gammaIdx] = ls.getAlpha();
            gammaIdx++;
        }

        return SUCCESS;
    }

    private boolean calculateMSS() {
        final LeastSquares ls = new LeastSquares();

        ls.calculate(toDouble(iMomentOrders), iGammasLogarithmic);
        iMSSlinear = ls.getBeta();
        iMSSlinearY0 = ls.getAlpha();

        ls.calculate(toLogScale(iMomentOrders), toLogScale(iGammasLogarithmic));
        iMSSlogarithmic = ls.getBeta();
        iMSSlogarithmicY0 = ls.getAlpha();

        return SUCCESS;
    }


    /**
     * Generates int[] with values from aMin to aMax (included) with step 1
     * @param aMin
     * @param aMax
     * @return array of requested values
     */
    private static int[] generateArrayRange(int aMin, int aMax) {
        final int[] range = new int[aMax - aMin + 1];
        int idx = 0;
        for (int m = aMin; m <= aMax; ++m) {
            range[idx] = m;
            idx++;
        }

        return range;
    }
}
