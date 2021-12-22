package art.coded.rateraid;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.widget.Button;

import art.coded.calibrater.Calibrater;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test methods for the {@link Rateraid} class.
 * Equals assertions are more precise as delta parameter approaches zero.
 */
@androidx.test.filters.LargeTest
@RunWith(AndroidJUnit4.class)
public class RateraidInstrumentedTest {

    private Button mIncrementButton;
    private Button mDecrementButton;
    private Context mContext;
    private long mCount;
    private Rateraid.ValueSeries mValueSeries;
    private Rateraid.ObjectSeries mObjectSeries;

    private static int PRECISION = Calibrater.STANDARD_PRECISION;

    /**
     * Initializes controllers to be tested from the instrumentation context
     */
    @Before public final void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mIncrementButton = new Button(mContext);
        mDecrementButton = new Button(mContext);
    }

    /**
     * Asserts whether adding, then incrementing and decrementing from, controllers through the
     * range of possible values assigns the expected value to each array element associated with the
     * {@link Rateraid.ValueSeries}.
     */
    @Test public final void testPercentSeriesAddButtonSetClickListenerCallback() {
        List<Double> percents = Arrays.asList(.25d, .25d, .25d, .25d);
        for (double magnitude = 0.01d; magnitude < 0.1d; magnitude += 0.01d) {
            mValueSeries = Rateraid.withValues(percents, magnitude, PRECISION, null);
            for (int index = 0; index < 4; index++) {

                mValueSeries.addShifters(mIncrementButton, mDecrementButton, index);
                mValueSeries.instance();
                double sum;

                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                assertEquals(.25d, percents.get(index), magnitude);
                sum = 0;
                for (int i = 0; i < percents.size(); i++) {
                    sum += percents.get(i);
                    if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (percents.get(index) < 1d) {
                    mIncrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < percents.size(); i++) {
                        sum += percents.get(i);
                        if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                mIncrementButton.performClick();
                assertEquals(1d, percents.get(index), magnitude);
                sum = 0;
                for (int i = 0; i < percents.size(); i++) {
                    sum += percents.get(i);
                    if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (percents.get(index) > 0d) {
                    mDecrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < percents.size(); i++) {
                        sum += percents.get(i);
                        if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                mIncrementButton.performClick();
                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                assertEquals(0d, percents.get(index), magnitude);
                sum = 0;
                for (int i = 0; i < percents.size(); i++) {
                    sum += percents.get(i);
                    if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (percents.get(index) < .25d) {
                    mIncrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < percents.size(); i++) {
                        sum += percents.get(i);
                        if (i == percents.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                Calibrater.resetRatings(percents, true, PRECISION);
            }
        }
    }

    /**
     * Asserts whether a user-defined behavior of incrementing on each controller interaction through
     * the range of possible values assigns the expected value to each array element associated with the
     * {@link Rateraid.ValueSeries}.
     */
    @Test public final void testPercentSeriesClickListenerUserDefinedCallback() {
        List<Double> percents = Arrays.asList(.25d, .25d, .25d, .25d);
        for (double magnitude = 0.01d; magnitude < 0.1d; magnitude += 0.01d) {
            mValueSeries = Rateraid.withValues(percents, magnitude, PRECISION, clickedView -> mCount++);
            for (int index = 0; index < 4; index++) {

                mValueSeries.addShifters(mIncrementButton, mDecrementButton, index);

                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                while (percents.get(index) < 1d) mIncrementButton.performClick();

                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                mIncrementButton.performClick();
                while (percents.get(index) > 0d) mDecrementButton.performClick();

                mIncrementButton.performClick();
                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                while (percents.get(index) < .25d) mIncrementButton.performClick();

                Calibrater.resetRatings(percents, true, PRECISION);
            }
        }
        assertEquals(2872, mCount);
    }

    /**
     * Asserts whether adding, then incrementing and decrementing from, controllers through the
     * range of possible values assigns the expected value to each array element associated with the
     * {@link Rateraid.ObjectSeries}.
     */
    @Test public final void testRateableSeriessAddButtonSetClickListenerCallback() {
        List<Rateraid.Rateable<ClonableRateable>> rateables = new ArrayList<>(4);
        ClonableRateable rateable = new ClonableRateable();
        rateable.setPercent(.25d);
        for (int i = 0; i < 4; i++) rateables.add(rateable.clone());
        for (double magnitude = 0.01d; magnitude < 0.1d; magnitude += 0.01d) {
            mObjectSeries = Rateraid.withObjects(rateables, magnitude, PRECISION, null);
            for (int index = 0; index < 4; index++) {

                mObjectSeries.addShifters(mIncrementButton, mDecrementButton, index);
                mObjectSeries.instance();
                double sum;

                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                assertEquals(.25d, rateables.get(index).getPercent(), magnitude);
                sum = 0;
                for (int i = 0; i < rateables.size(); i++) {
                    sum += rateables.get(i).getPercent();
                    if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (rateables.get(index).getPercent() < 1d) {
                    mIncrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < rateables.size(); i++) {
                        sum += rateables.get(i).getPercent();
                        if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                mIncrementButton.performClick();
                assertEquals(1d, rateables.get(index).getPercent(), magnitude);
                sum = 0;
                for (int i = 0; i < rateables.size(); i++) {
                    sum += rateables.get(i).getPercent();
                    if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (rateables.get(index).getPercent() > 0d) {
                    mDecrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < rateables.size(); i++) {
                        sum += rateables.get(i).getPercent();
                        if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                mIncrementButton.performClick();
                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                assertEquals(0d, rateables.get(index).getPercent(), magnitude);
                sum = 0;
                for (int i = 0; i < rateables.size(); i++) {
                    sum += rateables.get(i).getPercent();
                    if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                }
                while (rateables.get(index).getPercent() < .25d) {
                    mIncrementButton.performClick();
                    sum = 0;
                    for (int i = 0; i < rateables.size(); i++) {
                        sum += rateables.get(i).getPercent();
                        if (i == rateables.size() - 1) assertEquals(1d, sum, magnitude);
                    }
                }

                Rateraid.resetRatings(rateables, true, PRECISION);
            }
        }
    }

    /**
     * Asserts whether a user-defined behavior of incrementing on each controller interaction through
     * the range of possible values assigns the expected value to each array element associated with the
     * {@link Rateraid.ObjectSeries}.
     */
    @Test public final void testRateableSeriesClickListenerUserDefinedCallback() {
        List<Rateraid.Rateable<ClonableRateable>> rateables = new ArrayList<>(4);
        ClonableRateable rateable = new ClonableRateable();
        rateable.setPercent(.25d);
        for (int i = 0; i < 4; i++) rateables.add(rateable.clone());for (double magnitude = 0.01d; magnitude < 0.1d; magnitude += 0.01d) {
            mObjectSeries = Rateraid.withObjects(rateables, magnitude, PRECISION, clickedView -> mCount++);
            for (int index = 0; index < 4; index++) {

                mObjectSeries.addShifters(mIncrementButton, mDecrementButton, index);

                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                while (rateables.get(index).getPercent() < 1d) mIncrementButton.performClick();

                mDecrementButton.performClick();
                mDecrementButton.performClick();
                mIncrementButton.performClick();
                mIncrementButton.performClick();
                while (rateables.get(index).getPercent() > 0d) mDecrementButton.performClick();

                mIncrementButton.performClick();
                mIncrementButton.performClick();
                mDecrementButton.performClick();
                mDecrementButton.performClick();
                while (rateables.get(index).getPercent() < .25d) mIncrementButton.performClick();

                Rateraid.resetRatings(rateables, true, PRECISION);
            }
        }
        assertEquals(2872, mCount);
    }

    /**
     * Implements {@link art.coded.rateraid.Rateraid.Rateable} and {@code Cloneable} interfaces
     * for populating a {@link Rateraid.ObjectSeries} with distinct references
     * to identically composed objects.
     */
    private class ClonableRateable implements Rateraid.Rateable<ClonableRateable>, Cloneable {
        
        private double percent;
        
        @Override public void setPercent(double percent) { this.percent = percent; }
        @Override public double getPercent() { return percent; }
        @Override public ClonableRateable getObject() { return this; }

        @Override public ClonableRateable clone() {
            ClonableRateable clone  = new ClonableRateable();
            clone.setPercent(this.percent);
            try { super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException("Class must implement Cloneable interface");
            }
            return clone;
        }
    }
}