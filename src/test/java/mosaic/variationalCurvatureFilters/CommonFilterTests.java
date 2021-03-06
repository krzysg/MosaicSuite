package mosaic.variationalCurvatureFilters;

import java.util.Random;

import mosaic.test.framework.CommonBase;

import org.junit.Assert;
import org.junit.Test;

public class CommonFilterTests extends CommonBase {

    @Test
    public void testFilterGcSplitVsNoSplitMethod() {
        testEqualitySplitVsNoSplit(new FilterKernelGc());
    }

    @Test
    public void testFilterMcSplitVsNoSplitMethod() {
        testEqualitySplitVsNoSplit(new FilterKernelMc());
    }

    @Test
    public void testFilterTvSplitVsNoSplitMethod() {
        testEqualitySplitVsNoSplit(new FilterKernelTv());
    }

    /**
     * Test helper. For given FilterKernel it tests equality between
     * split and non split version of filter. It compares different sizes of
     * tested image from 1x1 to 20x20.
     *
     * @param aFk Tested FilterKernel
     */
    void testEqualitySplitVsNoSplit(FilterKernel aFk) {
        final int numOfIterations = 10;
        final float expectedPrecision = 0.000001f;
        final int maxSizeX = 20;
        final int maxSizeY = 20;

        final Random randomGenerator = new Random();

        for (int xSize = 1; xSize < maxSizeX; xSize++) {
            for (int ySize = 1; ySize < maxSizeY; ySize++) {
                final float[][] testImgNoSplit = new float[ySize][xSize];
                final float[][] testImgSplit = new float[ySize][xSize];
                for (int y = 0; y < ySize; ++y) {
                    for (int x = 0; x < xSize; ++x) {
                        final float val = randomGenerator.nextFloat();
                        testImgNoSplit[y][x] = val;
                        testImgSplit[y][x] = val;
                    }
                }
                final NoSplitFilter nsf = new NoSplitFilter(aFk);
                nsf.runFilter(testImgNoSplit, numOfIterations);

                final SplitFilter sf = new SplitFilter(aFk);
                sf.runFilter(testImgSplit, numOfIterations);

                for (int y = 0; y < ySize; ++y) {
                    Assert.assertArrayEquals("Pixels should have same values!",
                            testImgSplit[y], testImgNoSplit[y], expectedPrecision);
                }
            }
        }
    }

}
