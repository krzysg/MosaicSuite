package mosaic.psf2d;

import static org.junit.Assert.assertEquals;

import org.apache.commons.math3.special.BesselJ;
import org.junit.Test;

import mosaic.utils.math.MathOps;

/** Tests fast implementation of bessel funcitons with provided in commons-math3 (which are 
 * about 3.5x slower and currently this is the only reason to not change j0/j1 methods.
 * 
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class PsfBesselTest {

    @Test
    public void test0() {
        double epsilon = 1e-8;
        assertEquals(BesselJ.value(0, 0), MathOps.bessel0(0), epsilon);
        assertEquals(BesselJ.value(0, 1), MathOps.bessel0(1), epsilon);
        assertEquals(BesselJ.value(0, 1), MathOps.bessel0(-1), epsilon);
        assertEquals(BesselJ.value(0, 10), MathOps.bessel0(10), epsilon);
        assertEquals(BesselJ.value(0, 10), MathOps.bessel0(-10), epsilon);
    }

    @Test
    public void test1() {
        double epsilon = 1e-8;
        assertEquals(0.0, MathOps.bessel1(0), epsilon);
        assertEquals(BesselJ.value(1, 0), MathOps.bessel1(0), epsilon);
        assertEquals(BesselJ.value(1, 1), MathOps.bessel1(1), epsilon);
        assertEquals(-BesselJ.value(1, 1), MathOps.bessel1(-1), epsilon);
        assertEquals(BesselJ.value(1, 10), MathOps.bessel1(10), epsilon);
        assertEquals(-BesselJ.value(1, 10), MathOps.bessel1(-10), epsilon);
    }
}
