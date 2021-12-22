package art.coded.calibrater;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting between decimal array types
 */
public final class TypeConverters {

    /**
     * Generates a {@code List} of {@code Double} from a {@code float} array.
     *
     */
    public static List<Double> arrayFloatToListDouble(float[] floatArray) {
        List<Double> doubleList = new ArrayList<>();
        for (float f: floatArray) doubleList.add((double) f);
        return doubleList;
    }

    /**
     * Generates a {@code List} of {@code Double} from a {@code List} of {@code Float}.
     */
    public static List<Double> listFloatToListDouble(List<Float> floatList) {
        List<Double> doubleList = new ArrayList<>();
        for (Float f : floatList) doubleList.add(f.doubleValue());
        return doubleList;
    }

    /**
     * Generates a {@code List} of {@code Double} from a {@code double} array.
     */
    public static List<Double> arrayDoubleToListDouble(double[] doubleArray) {
        List<Double> doubleList = new ArrayList<>();
        for (double d : doubleArray) doubleList.add(d);
        return doubleList;
    }

    /**
     * Generates a {@code float} array from a {@code List} of {@code Double}.
     */
    public static float[] listDoubleToArrayFloat(List<Double> doubleList) {
        Double[] doubleArray = doubleList.toArray(new Double[0]);
        float[] floatArray = new float[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) floatArray[i] = doubleArray[i].floatValue();
        return floatArray;
    }

    /**
     * Generates a {@code List} of {@code Float} from a {@code List} of {@code Double}.
     */
    public static List<Float> listDoubleToListFloat(List<Double> doubleList) {
        List<Float> floatList = new ArrayList<>();
        for (Double d : doubleList) floatList.add(d.floatValue());
        return floatList;
    }

    /**
     * Generates a {@code List} of {@code double} from a {@code List} of {@code Double}.
     */
    public static double[] listDoubleToArrayDouble(List<Double> doubleList) {
        Double[] doubleBoxedArray = doubleList.toArray(new Double[0]);
        double[] doublePrimativeArray = new double[doubleBoxedArray.length];
        for (int i = 0; i < doubleBoxedArray.length; i++) doublePrimativeArray[i] = doubleBoxedArray[i];
        return doublePrimativeArray;
    }
}
