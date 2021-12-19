package com.github.rjbx.calibrater;

import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Test methods for the {@link Calibrater} class.
 * Equals assertions are more precise as delta parameter approaches zero.
 */
public class CalibraterTest {
    
    private static int PRECISION = Calibrater.STANDARD_PRECISION;
    private static double ERROR = Math.pow(10, -PRECISION);

    /**
     * Asserts whether incrementing and decrementing a {@code List} of {@code Double} element resets all element values.
     */
    @Test public final void testShiftDoubleArrayResetSingle() {

        List<Double> percents = Arrays.asList(.25d, .25d, .25d, .25d);
        for (double magnitude = 0.01d; magnitude <= .1d; magnitude += 0.01d) {
            for (int index = 0; index < 4; index++) {
                Calibrater.shiftRatings(percents, index, magnitude, PRECISION);
                Calibrater.shiftRatings(percents, index, magnitude * -1d, PRECISION);
            }
            assertEquals(.25d, percents.get(0), ERROR);
            assertEquals(.25d, percents.get(1), ERROR);
            assertEquals(.25d, percents.get(2), ERROR);
            assertEquals(.25d, percents.get(3), ERROR);
        }
    }

    /**
     * Asserts whether incrementing and decrementing through the range of possible values for
     * two adjacent {@code List} of {@code Double}s assigns the expected value to each array element.
     */
    @Test public final void testShiftDoubleArrayRepeatRange() {

        int multiplier = 0;
        List<Double> percents = Arrays.asList(.25d, .25d, .25d, .25d);
        for (double magnitude = 0.01d; magnitude <= .1d; magnitude += 0.01d) {
            for (int index = 0; index < percents.size(); index++) {
                while (percents.get(index) <= .99d) {
                    Calibrater.shiftRatings(percents, index, magnitude, PRECISION);
                    multiplier++;
                }
                while (percents.get(index) >= .01d) {
                    Calibrater.shiftRatings(percents, index, magnitude * -1d, PRECISION);
                    multiplier++;
                }
                int nextIndex = index == 3 ? 0 : index + 1;
                while (percents.get(index) >= .01d) {
                    Calibrater.shiftRatings(percents, nextIndex, magnitude * -1d, PRECISION);
                    multiplier++;
                }
                while (percents.get(nextIndex) <= .99d) {
                    Calibrater.shiftRatings(percents, nextIndex, magnitude, PRECISION);
                    multiplier++;
                }
            }

            double error = ERROR + (ERROR * multiplier);

            assertEquals(1d, percents.get(0), error);
            assertEquals(0d, percents.get(1), error);
            assertEquals(0d, percents.get(2), error);
            assertEquals(0d, percents.get(3), error);

            double sum = 0d;
            Calibrater.recalibrateRatings(percents, false, PRECISION);
            for (double percent : percents) sum += percent;
            assertEquals(1d, sum, PRECISION); // Test recalibration precision
        }
    }

    /**
     * Asserts whether incrementing and decrementing through the range of possible values for
     * two adjacent {@code List} of {@code Double}s assigns the expected value to each array element.
     */
    @Test public final void testShiftDoubleArrayResetSingleRepeatRange() {

        List<Double> percents = Arrays.asList(.25d, .25d, .25d, .25d);
        for (double magnitude = 0.01d; magnitude < 0.1d; magnitude += 0.01d) {
            for (int index = 0; index < 4; index++) {
                double sum;


                for (int i = 0; percents.get(index) < 1d; i++) {
                    Calibrater.shiftRatings(percents, index, magnitude / 2, PRECISION);
                    sum = 0;
                    for (double percent : percents) sum += percent;
                    double error = ERROR + (ERROR * i++);
                    assertEquals(1d, sum, error);
                }
                Calibrater.shiftRatings(percents, index, -magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, -magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, magnitude / 2, PRECISION);
                assertEquals(1d, percents.get(index), ERROR);
                sum = 0;
                for (int i = 0; i < percents.size(); i++) {
                    sum += percents.get(i);
                    if (i == percents.size() - 1) assertEquals(1d, sum, ERROR);
                }

                while (percents.get(index) > 0d) {
                    Calibrater.shiftRatings(percents, index, -magnitude / 2, PRECISION);
                    sum = 0;
                    for (int i = 0; i < percents.size(); i++) {
                        sum += percents.get(i);
                        if (i == percents.size() - 1) assertEquals(1d, sum, ERROR);
                    }
                }

                Calibrater.shiftRatings(percents, index, magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, -magnitude / 2, PRECISION);
                Calibrater.shiftRatings(percents, index, -magnitude / 2, PRECISION);
                assertEquals(0d, percents.get(index), ERROR);
                sum = 0;
                for (int i = 0; i < percents.size(); i++) {
                    sum += percents.get(i);
                    if (i == percents.size() - 1) assertEquals(1d, sum, ERROR);
                }

                assertFalse(Calibrater.resetRatings(percents, false, PRECISION));
            }
        }
    }

    /**
     * Asserts whether shifting {@code List} of {@code Double} elements to offset an element
     * within a certain margin past the lower limit does not over allocate the unused offset.
     */
    @Test public final void testShiftDoubleArrayLimitAllocation() {

        for (double magnitude = 0.001d; magnitude < 1d; magnitude *= 10) {
            List<Double> percents = Arrays.asList(1d, 0d, 0d, 0d, 0d);
            Calibrater.shiftRatings(percents, 1, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 1, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 2, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 2, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 3, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 3, magnitude, PRECISION);
            Calibrater.shiftRatings(percents, 4, magnitude, PRECISION);

            while (percents.get(3) > magnitude / 2)
                Calibrater.shiftRatings(percents, 1, magnitude, PRECISION);

            double sum = 0;
            for (double percent : percents) sum += percent;
            double error = ERROR + (ERROR * percents.size());
            assertEquals(1d, sum, error);
        }
    }

    /**
     * Asserts whether shifting a {@code List} of {@code Double} with different magnitudes returns
     * the expected boolean value.
     */
    @Test public final void testShiftDoubleArrayReturnValue() {

        List<Double> percents = Arrays.asList(0d, 0d, 0d, 0d);
        assertFalse(Calibrater.shiftRatings(percents, 0, -0.01d, PRECISION));
        assertFalse(Calibrater.shiftRatings(percents, 0, 0d, PRECISION));

        percents.set(0, 1d);
        assertFalse(Calibrater.shiftRatings(percents, 0, 0.02d, PRECISION));
        assertFalse(Calibrater.shiftRatings(percents, 0, 0d, PRECISION));

        for (int i = 0; i < percents.size(); i++) percents.set(i, .5d);
        assertTrue(Calibrater.shiftRatings(percents, 0, -0.1d, PRECISION));
        assertTrue(Calibrater.shiftRatings(percents, 0, 0.1d, PRECISION));

        for (int i = 0; i < percents.size(); i++) percents.set(i, .1d);
        assertTrue(Calibrater.shiftRatings(percents, 0, -0.2d, PRECISION));
        assertTrue(Calibrater.shiftRatings(percents, 0, 0.02d, PRECISION));
    }

    /**
     * Asserts whether resetting a {@code List} of {@code Double} assigns the expected value to each array element
     * and results in the expected sum of all elements.
     */
    @Test public final void testResetDoubleArrayEqualDistribution() {

        for (int i = 1; i < 20; i++) {
            List<Double> percents = new ArrayList<>();
            for (int j = 0; j < i; j++) percents.add(0d);
            Calibrater.resetRatings(percents, false, PRECISION);
            assertEquals(1d / i, percents.get(i - 1), 0d);
            double sum = 0d;
            for (Double percent : percents) sum += percent;
            assertEquals(1d, sum, PRECISION);
        }
    }

    /**
     * Asserts whether resetting {@code List} of {@code Double}s with different element values returns
     * the expected boolean value.
     */
    @Test public final void testResetDoubleArrayReturnValue() {

        List<Double> percents = Arrays.asList(1d, 0d, 0d, 0d);
        for (int i = 0; i < 2; i++) {
            assertFalse(Calibrater.resetRatings(percents, false, PRECISION));
            percents.set(0, percents.get(0) - .25d);
            percents.set(3 - i, percents.get(3 - i) +.25d);
        }

        percents.set(3, .5d);
        assertTrue(Calibrater.resetRatings(percents, false, PRECISION));

        for (int i = 0; i < percents.size() - 1; i++) percents.set(i, 0d);
        assertTrue(Calibrater.resetRatings(percents, false, PRECISION));

        percents.set(3, 0d);
        assertTrue(Calibrater.resetRatings(percents, false, PRECISION));
    }

    /**
     * Asserts whether shifting a {@code List} of {@code Double} with invalid magnitudes and precisions throws
     * the expected exception.
     */
    @Test public final void testShiftWithIllegalArguments() {

        List<Double> percents = Arrays.asList(0d, 0d, 0d, 0d);

        assertShiftThrows(IllegalArgumentException.class, percents, 0, 1.0001d, PRECISION);
        assertShiftThrows(IllegalArgumentException.class, percents, 0, -1.0001d, PRECISION);
        assertShiftThrows(IllegalArgumentException.class, percents, 0, 0d, 17);
        assertShiftThrows(IllegalArgumentException.class, percents, 0, 0d, -1);
    }

    /**
     * Asserts whether the parameter {@code Class} of type {@code Throwable} is thrown when
     * invoking {@link Calibrater#shiftRatings(List, int, double, int)} with the parameter args
     */
    private static <T extends Throwable>void assertShiftThrows(Class<T> throwableClass, Object... args) {

        try {

            Calibrater.shiftRatings(
                    (List<Double>) args[0], (int) args[1], (double) args[2], (int) args[3]
            ); fail(); // Exception not thrown

        } catch (Throwable e){ assertTrue(throwableClass.isInstance(e)); }
    }
}