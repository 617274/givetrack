package art.coded.rateraid;

import android.content.DialogInterface;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import art.coded.calibrater.Calibrater;
import art.coded.calibrater.TypeConverters;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
// TODO: Store field references to controller views and implement method to detach listeners by passing null to the attach method
/**
 * Library for specifying adjustments to the values of a percent series.
 * Users can bind predefined as well as custom behaviors to the views controlling adjustments
 * to optionally increment and decrement, remove or text edit the values of a percent series.
 * Single element adjustments automatically update all values in the series as needed to maintain
 * that their sum remain proportionate to the whole within range of the specified precision.
 */
public class Rateraid {

    /**
     * Wrapper interface for an object associated with a percent value.
     * @param <T> to specify the type of the implementing class for generating appropriately cast
     *            objects from {@link #getObject()}
     */
    public interface Rateable<T> {
        void setPercent(double percent);
        double getPercent();
        T getObject();
    }

    /*
     * Percent values which are updated during adjustments
     */
    private List<Double> mPercents;

    /**
     * Update the percent values.
     * @param percents {@code List} of {@code Double}
     */
    private void setPercents(List<Double> percents) { this.mPercents = percents; }

    /**
     * Update the percent values from a {@code float} array.
     * @param percents {@code float} array
     */
    private void setPercentsFromFloatArray(float[] percents) { this.mPercents = TypeConverters.arrayFloatToListDouble(percents); }

    /**
     * Update the percent values from a {@code double} array.
     * @param percents {@code double} array
     */
    private void setPercentsFromDoubleArray(double[] percents) { this.mPercents = TypeConverters.arrayDoubleToListDouble(percents); }

    /**
     * Update the percent values from a {@code List} of {@code Float}.
     * @param percents {@code List} of {@code Float}
     */
    private void setPercentsFromFloatList(List<Float> percents) { this.mPercents = TypeConverters.listFloatToListDouble(percents); }

    /**
     * Retrieve the percent values.
     * @return {@code List} of {@code Double}
     */
    public List<Double> getPercents() { return mPercents; }

    /**
     * Retrieve the percent values as a {@code float} array.
     * @return {@code float} array
     */
    public float[] getPercentsAsFloatArray() { return TypeConverters.listDoubleToArrayFloat(mPercents); }

    /**
     * Retrieve the percent values as a {@code double} array.
     * @return {@code double} array
     */
    public double[] getPercentsAsDoubleArray() { return TypeConverters.listDoubleToArrayDouble(mPercents); }

    /**
     * Retrieve the percent values as a {@code List} of {@code Float}.
     * @return {@code List} of {@code Float}
     */
    public List<Float> getPercentsAsListFloat() { return TypeConverters.listDoubleToListFloat(mPercents); }

    /**
     * Initialize the percent series for a {@code List} of {@code Double}
     * and define the attributes and behavior to be applied to all adjustments.
     * @param percents {@code List} of {@code Double} elements
     * @param magnitude amount of the adjustment; non-zero value should be between 1 and -1
     * @param precision number of decimal places to move the permitted error from the whole
     * @param clickListener additional behavior to be applied to all adjustments
     * @return {@link ValueSeries} from which to chain view binding method calls
     */
    // TODO: Convert from array to List
    public static ValueSeries withValues(
            List<Double> percents,
            double magnitude,
            int precision,
            @Nullable View.OnClickListener clickListener) {
        return new ValueSeries(percents, magnitude, precision,  clickListener);
    }

    /**
     * Initialize the percent series for a {@code List} of {@link Rateable}
     * and define the attributes and behavior to be applied to all adjustments.
     * @param objects {@code List} of {@link Rateable} elements
     * @param magnitude amount of the adjustment; non-zero value should be between 1 and -1
     * @param precision number of decimal places to move the permitted error from the whole
     * @param clickListener additional behavior to be applied to all adjustments
     * @return {@link ObjectSeries} from which to chain view binding method calls
     */
    public static <T extends Rateable> ObjectSeries<T> withObjects(
            List<T> objects,
            double magnitude,
            int precision,
            @Nullable View.OnClickListener clickListener) {
        return new ObjectSeries(objects, magnitude, precision,  clickListener);
    }

    /**
     * Class for chaining method calls for defining behaviors of views
     * associated with this percent series for a {@code List} of {@code Double}.
     */
    public static class ValueSeries {

        // Instance fields of this class
        private Rateraid mRateraid;
        private List<Double> mPercents;
        private double mMagnitude;
        private int mPrecision;
        private View.OnClickListener mClickListener;

        /**
         * Initialize the instance fields of this class from the parent class accessor.
         * @param percents {@code List} of {@code Double} elements
         * @param magnitude amount of the adjustment; non-zero value should be no more than 1 or -1
         * @param precision number of decimal places to move the allowed error from the whole
         * @param clickListener additional behavior to be applied to all adjustments
         */
        private ValueSeries(
                List<Double> percents,
                double magnitude,
                int precision,
                @Nullable View.OnClickListener clickListener) {
            mMagnitude = magnitude;
            mPrecision = precision;
            mClickListener = clickListener;
            mPercents = percents;
            Calibrater.recalibrateRatings(mPercents, false, precision);
        }

        /**
         * Define the controllers by which the specified element of the percent series
         * is incremented and decremented.
         * @param incrementButton view that, when clicked, should increment the targeted value
         * @param decrementButton view that, when clicked, should decrement the targeted value
         * @param index location of the value to be adjusted
         * @return {@link ValueSeries} from which to chain view binding method calls
         */
        public ValueSeries addShifters(View incrementButton, View decrementButton, int index) {
            incrementButton.setOnClickListener(clickedView -> {
                Calibrater.shiftRatings(mPercents, index, mMagnitude, mPrecision);
                if (mClickListener != null) mClickListener.onClick(incrementButton);
            });
            decrementButton.setOnClickListener(clickedView -> {
                Calibrater.shiftRatings(mPercents, index, -mMagnitude, mPrecision);
                if (mClickListener != null) mClickListener.onClick(decrementButton);
            });
            return this;
        }

        /**
         * Define the controller by which the specified element of the percent series is invalidated.
         * @param removeButton view that, when clicked, should remove the targeted element
         * @param index location of the value to be invalidated
         * @param dialog removal message to be dismissed
         * @return {@link ObjectSeries} from which to chain view binding method calls
         */
        public ValueSeries addRemover(
                View removeButton, int index, @Nullable DialogInterface dialog) {
            removeButton.setOnClickListener(clickedView -> {
                Calibrater.removeRating(mPercents, index, mPrecision);

                if (dialog != null) dialog.dismiss();
                if (mClickListener != null) mClickListener.onClick(removeButton);
            }); return this;
        }

        /**
         * Define the controller by which the specified element of the percent series is
         * replaced by text entry.
         * @param valueEditor editor that, when receiving entry, should replace the targeted value 
         * @param index location of the value to be replaced
         * @param imm input manager to be dismissed 
         * @return {@link ValueSeries} from which to chain view binding method calls
         */
        public ValueSeries addEditor(
                EditText valueEditor, int index, @Nullable InputMethodManager imm) {
            valueEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);
            valueEditor.setInputType(EditorInfo.TYPE_CLASS_NUMBER|EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
            valueEditor.setOnEditorActionListener((onEditorActionView, onEditorActionId, onEditorActionEvent) -> {
                switch (onEditorActionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        final NumberFormat percentFormatter = NumberFormat.getPercentInstance();
                        try {
                            double percent = mPercents.get(index);
                            String viewText = onEditorActionView.getText().toString();
                            if (viewText.contains("%")) percent = percentFormatter.parse(viewText).doubleValue();
                            else if (!viewText.isEmpty()) percent = Double.parseDouble(viewText);
                            else {
                                onEditorActionView.setText(percentFormatter.format(percent));
                                return false;
                            }
                            if (percent < 0d || percent > 1d) return false;
                            double magnitude = percent - mPercents.get(index);
                            Calibrater.shiftRatings(mPercents, index, magnitude, mPrecision);
                            if (imm != null) imm.toggleSoftInput(0, 0);
                            if (mClickListener != null) mClickListener.onClick(valueEditor);
                        } catch (ParseException e) {
                            throw new NumberFormatException();
                        } return true;
                    default:
                        return false;
                }
            }); return this;
        }

        /**
         * Retrieve a new reference to a {@link ValueSeries} with values initialized.
         * @return new reference to a {@link ValueSeries}
         */
        public Rateraid instance() {
            mRateraid = new Rateraid();
            mRateraid.setPercents(mPercents);
            return mRateraid;
        }
    }
    
    /**
     * Class for chaining method calls for defining behaviors of views
     * associated with this percent series for a {@code List} of {@link Rateable}.
     */
    public static class ObjectSeries<T extends Rateable> {

        // Instance fields of this class
        private Rateraid mRateraid;
        private List<T> mRateables;
        private double mMagnitude;
        private int mPrecision;
        private View.OnClickListener mClickListener;

        /**
         * Initialize the instance fields of this class from the parent class accessor.
         * @param objects {@code List} of {@link Rateable} elements
         * @param magnitude amount of the adjustment; non-zero value should be no more than 1 or -1
         * @param precision number of decimal places to move the allowed error from the whole
         * @param clickListener additional behavior to be applied to all adjustments
         */
        private ObjectSeries(
                List<T> objects,
                double magnitude,
                int precision,
                @Nullable View.OnClickListener clickListener) {
            mMagnitude = magnitude;
            mPrecision = precision;
            mClickListener = clickListener;
            mRateables = objects;
            recalibrateRatings(objects, false, precision);
        }

        /**
         * Define the controllers by which the specified element of the percent series
         * is incremented and decremented
         * @param incrementButton view that, when clicked, should increment the targeted value
         * @param decrementButton view that, when clicked, should decrement the targeted value
         * @param index location of the value to be adjusted
         * @return {@link ObjectSeries} from which to chain view binding method calls
         */
        public ObjectSeries addShifters(View incrementButton, View decrementButton, int index) {
            incrementButton.setOnClickListener(clickedView -> {
                shiftRatings(mRateables, index, mMagnitude, mPrecision);
                if (mClickListener != null) mClickListener.onClick(incrementButton);
            });
            decrementButton.setOnClickListener(clickedView -> {
                shiftRatings(mRateables, index, -mMagnitude, mPrecision);
                if (mClickListener != null) mClickListener.onClick(decrementButton);
            });
            return this;
        }

        /**
         * Define the controller by which the specified element of the percent series is removed.
         * @param removeButton view that, when clicked, should remove the targeted element
         * @param index location of the value to be invalidated
         * @param dialog removal message to be dismissed 
         * @return {@link ObjectSeries} from which to chain view binding method calls
         */
        public ObjectSeries addRemover(
                View removeButton, int index, @Nullable DialogInterface dialog) {
            removeButton.setOnClickListener(clickedView -> {
                removeRating(mRateables, index, mPrecision);

                if (dialog != null) dialog.dismiss();
                if (mClickListener != null) mClickListener.onClick(removeButton);
            }); return this;
        }

        /**
         * Define the controller by which the specified element of the percent series is
         * replaced by text entry.
         * @param valueEditor editor that, when receiving entry, should replace the targeted value 
         * @param index location of the value to be replaced
         * @param imm input manager to be dismissed 
         * @return {@link ObjectSeries} from which to chain view binding method calls
         */
        public ObjectSeries addEditor(
                EditText valueEditor, int index, @Nullable InputMethodManager imm) {
            valueEditor.setImeOptions(EditorInfo.IME_ACTION_DONE);
            valueEditor.setInputType(EditorInfo.TYPE_CLASS_NUMBER|EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
            valueEditor.setOnEditorActionListener((onEditorActionView, onEditorActionId, onEditorActionEvent) -> {
                switch (onEditorActionId) {
                    case EditorInfo.IME_ACTION_DONE:
                        final NumberFormat percentFormatter = NumberFormat.getPercentInstance();
                        try {
                            double percent = mRateables.get(index).getPercent();
                            String viewText = onEditorActionView.getText().toString();
                            if (viewText.contains("%")) percent = percentFormatter.parse(viewText).doubleValue();
                            else if (!viewText.isEmpty()) percent = Double.parseDouble(viewText);
                            else {
                                onEditorActionView.setText(percentFormatter.format(percent));
                                return false;
                            }
                            if (percent < 0d || percent > 1d) return false;
                            double magnitude = percent - mRateables.get(index).getPercent();
                            shiftRatings(mRateables, index, magnitude, mPrecision);
                            if (imm != null) imm.toggleSoftInput(0, 0);
                            if (mClickListener != null) mClickListener.onClick(valueEditor);
                        } catch (ParseException e) {
                            throw new NumberFormatException();
                        } return true;
                    default:
                        return false;
                }
            }); return this;
        }
        
        /**
         * Retrieve a new reference to a {@link ObjectSeries} with values initialized.
         * @return new reference to an {@link ObjectSeries}
         */
        public Rateraid instance() {
            mRateraid = new Rateraid();
            List<Double> percents = new ArrayList<>();
            for (Rateable object : mRateables) percents.add(object.getPercent());
            mRateraid.setPercents(percents);
            return mRateraid;
        }
    }

    /**
     * Increments or decrements an element of a {@code List} of {@link Rateable} by the specified
     * magnitude while calibrating other {@code List} of {@link Rateable} elements to maintain
     * proportionality to the whole within the range of the specified precision.
     * @param objects {@link Rateable} {@code List} elements to be adjusted if not proportionate
     * @param index index of the array element to be adjusted
     * @param magnitude amount of the adjustment; non-zero value should be no more than 1 or -1
     * @param precision number of decimal places to move the allowed error from the whole
     * @return true if percent was adjusted and false otherwise
     */
    public static <T extends Rateable> boolean shiftRatings(
            List<T> objects, int index, double magnitude, int precision) {
        boolean result;
        List<Double> percents = new ArrayList<>();
        for (Rateable object : objects) percents.add(object.getPercent());
        result = Calibrater.shiftRatings(percents, index, magnitude, precision);
        for (int i = 0; i < percents.size(); i++) objects.get(i).setPercent(percents.get(i));
        return result;
    }

    /**
     * Assigns equivalent percents to each {@code List} of {@link Rateable} element.
     * @param objects {@code List} of {@link Rateable} elements to be reset if not equivalent
     * @param forceReset applies reset even if sum of array elements is as precise as specified
     * @param precision number of decimal places to move the permitted error from the whole
     * @return true if values were adjusted; false otherwise
     */
    public static <T extends Rateable> boolean resetRatings(
            List<T> objects, boolean forceReset, int precision) {
        boolean result;
        List<Double> percents = new ArrayList<>();
        for (Rateable object : objects) percents.add(object.getPercent());
        result = Calibrater.resetRatings(percents, forceReset, precision);
        for (int i = 0; i < percents.size(); i++) objects.get(i).setPercent(percents.get(i));
        return result;
    }

    /**
     * Removes {@link Rateable} from {@code List} at the specified index.
     * The whole is then distributed among the remaining elements
     * in accordance with {@link Calibrater#recalibrateRatings(List, boolean, int)}.
     * @param objects {@code List} of {@link Rateable} elements from which to remove the specified element
     * @param index location of the object to be removed
     * @param precision number of decimal places to move the permitted error from the whole
     * @return true if values were adjusted; false otherwise
     */
    public static <T extends Rateable> boolean removeRating(
            List<T> objects, int index, int precision) {
        objects.remove(index);
        return recalibrateRatings(objects, false, precision);
    }

    /**
     * Equally distributes to each {@code List} of {@link Rateable} element the difference between
     * the whole and the sum of all array elements.
     * @param objects {@code List} of {@link Rateable} to be calibrated closer to the whole
     * @param forceReset applies reset even if sum of array elements is as precise as specified
     * @param precision number of decimal places to move the permitted error from the whole
     * @return true if values were adjusted; false otherwise
     */
    public static <T extends Rateable> boolean recalibrateRatings(
            List<T> objects, boolean forceReset, int precision) {
        boolean result;
        List<Double> percents = new ArrayList<>();
        for (Rateable object : objects) percents.add(object.getPercent());
        result = Calibrater.recalibrateRatings(percents, forceReset, precision);
        for (int i = 0; i < percents.size(); i++) objects.get(i).setPercent(percents.get(i));
        return result;
    }
}