package mosaic.ia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.junit.Test;

import ij.macro.Interpreter;
import mosaic.ia.Analysis.Result;
import mosaic.ia.utils.IAPUtils;
import mosaic.ia.utils.ImageProcessUtils;
import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;

/**
 * Tests of IA
 * TODO: These are not 'real' tests. They just written to help refactor code and
 * finally *make* it testable. They cover around 3/4 of IA which should be enough
 * to quite safely start refactoring.
 */
public class AnalysisTest extends CommonBase {

    @Test
    public void testCalcDist() {
        ArrayList<Point3d> xl = new ArrayList<Point3d>();
        for (int i = 0; i < 5; i++) {
            xl.add(new Point3d(i, 2, 0));
        }
        Point3d[] x = xl.toArray(new Point3d[0]);
        ArrayList<Point3d> yl = new ArrayList<Point3d>();
        for (int i = 0; i < 5; i += 2) {
            yl.add(new Point3d(i, i, 0));
        }
        Point3d[] y = yl.toArray(new Point3d[0]);

        Analysis analysis = new Analysis(x, y);
        analysis.setKernelWeightq(0.001);
        analysis.setKernelWeightp(2.974);
        analysis.setX1(0);
        analysis.setX2(5);
        analysis.setY1(0);
        analysis.setY2(5);
        analysis.setZ1(0);
        analysis.setZ2(0);
        analysis.calcDist(0.5);
        
        double epsilon = 1e-10;
        assertEquals(0, analysis.getMinD(), epsilon);
        assertEquals(2, analysis.getMaxD(), epsilon);
        assertArrayEquals(new double[] {2, 1, 0, 1, 2}, analysis.getDistances(), epsilon);
    }

    @Test
    public void testCmaOptimizationHernquitst() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinD(), epsilon);
        assertEquals(112.924864, analysis.getMaxD(), epsilon);
        assertEquals(77.255365, IAPUtils.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(PotentialFunctions.HERNQUIST);
        analysis.setCmaReRunTimes(1);
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results);
        System.out.println(results);
        
        // Results may vary quite a lot - change epsilon to relax expectations
        epsilon = 0.01;
        assertEquals(36.791485, results.get(0).iStrength, epsilon);
        assertEquals(242.056460, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.001399, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypTest(100, 0.01);
    }

    private Analysis prepereIaForTest() {
        // Make IJ running in batch mode (no GUI)
        Interpreter.batchMode = true;
        
        // Define test data
        final String tcDirName           = "IA/VirusEndosome/";
        copyTestResources("Virus.csv", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        Point3d[] x = ImageProcessUtils.openCsvFile("X", "/tmp/" + "Virus.csv");
        copyTestResources("Endosome.csv", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        Point3d[] y = ImageProcessUtils.openCsvFile("Y", "/tmp/" + "Endosome.csv");
        
        Analysis analysis = new Analysis(x, y);
        analysis.setKernelWeightq(0.001);
        analysis.setKernelWeightp(35.9);
        analysis.setX1(0);
        analysis.setX2(385);
        analysis.setY1(0);
        analysis.setY2(511);
        analysis.setZ1(0);
        analysis.setZ2(0);
        analysis.calcDist(0.5);
        return analysis;
    }
    
    @Test
    public void testCmaOptimizationNONPARAM() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinD(), epsilon);
        assertEquals(112.924864, analysis.getMaxD(), epsilon);
        assertEquals(77.255365, IAPUtils.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(PotentialFunctions.NONPARAM);
        
        PotentialFunctions.NONPARAM_WEIGHT_SIZE = 41;
        PotentialFunctions.NONPARAM_SMOOTHNESS = 0.1;
        PotentialFunctions.initializeNonParamWeights(analysis.getMinD(), analysis.getMaxD());
        
        analysis.setCmaReRunTimes(1);
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results);
        System.out.println(results);
        
        // Results may vary quite a lot - change epsilon to relax expectations
        epsilon = 0.01;
        assertEquals(0.0, results.get(0).iStrength, epsilon);
        assertEquals(0.0, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.008895, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypTest(100, 0.01);
    }
    
    @Test
    public void testCmaOptimizationSTEP() {
        Analysis analysis = prepereIaForTest();
        
        double epsilon = 1e-6;
        assertEquals(0.418188, analysis.getMinD(), epsilon);
        assertEquals(112.924864, analysis.getMaxD(), epsilon);
        assertEquals(77.255365, IAPUtils.calcWekaWeights(analysis.getDistances()), epsilon);
        analysis.setPotentialType(PotentialFunctions.STEP);

        analysis.setCmaReRunTimes(1);
        List<Result> results = new ArrayList<Result>();
        analysis.cmaOptimization(results);
        System.out.println(results);
        
        // Results may vary quite a lot - change epsilon to relax expectations
        epsilon = 0.1;
        assertEquals(2.411145, results.get(0).iStrength, epsilon);
        assertEquals(6.472193, results.get(0).iThresholdScale, epsilon);
        assertEquals(0.002122, results.get(0).iResidual, epsilon);
        
        // No testing anything - just running to catch any unwanted null things..
        analysis.hypTest(100, 0.01);
    }
}
