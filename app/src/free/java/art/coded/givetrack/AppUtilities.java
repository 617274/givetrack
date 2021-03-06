package art.coded.givetrack;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Parcelable;

import art.coded.givetrack.data.entry.Entry;
import art.coded.givetrack.data.entry.User;

import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public final class AppUtilities {

    public static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance();
    public static final NumberFormat PERCENT_FORMATTER = NumberFormat.getPercentInstance();
    public static final DateFormat DATE_FORMATTER = DateFormat.getDateInstance(DateFormat.SHORT);

    public static <T extends Parcelable> T[] getTypedArrayFromParcelables(Parcelable[] parcelables, Class<T> arrayType) {
        T[] typedArray = (T[]) Array.newInstance(arrayType, parcelables.length);
        System.arraycopy(parcelables, 0, typedArray, 0, parcelables.length);
        return typedArray;
    }

    public static void mapToSharedPreferences(Map<String, Object> map, SharedPreferences sp) {

        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            Object value = entry.getValue();
            if (value instanceof String) sp.edit().putString(entry.getKey(), (String) value).apply();
            else if (value instanceof Boolean) sp.edit().putBoolean(entry.getKey(), (Boolean) value).apply();
            else if (value instanceof Integer) sp.edit().putString(entry.getKey(), String.valueOf(value)).apply();
            else if (value instanceof Long) sp.edit().putLong(entry.getKey(), (Long) value).apply();
            else if (value instanceof Float) sp.edit().putFloat(entry.getKey(), (Float) value).apply();
        }
    }

    public static Object preferenceValueToNumerical(Object value, Class type) {
        if (value instanceof String) {
            if (type == Boolean.class) return Boolean.valueOf((String) value);
            else if (type == Integer.class) return Integer.valueOf((String) value);
            else if (type == Long.class) return Long.valueOf((String) value);
            else if (type == Float.class) return Float.valueOf((String) value);
            else if (type == Double.class) return Double.valueOf((String) value);
        } return value;
    }

    public static String[] getArgs(String... strings) {
        int arrayLength = strings.length;
        String[] stringArray = new String[arrayLength];
        System.arraycopy(strings, 0, stringArray, 0, arrayLength);
        return stringArray;
    }

    /**
     * Generates a {@link User} from {@link SharedPreferences} and {@link FirebaseUser} attributes.
     */
    public static User convertRemoteToLocalUser(FirebaseUser firebaseUser) {

        String uid = "";
        String email = "";
        if (firebaseUser != null) {
            uid = firebaseUser.getUid();
            String firebaseEmail = firebaseUser.getEmail();
            if (firebaseEmail != null) email = firebaseUser.getEmail();
        }

        User user = User.getDefault();
        user.setUid(uid);
        user.setUserEmail(email);
        user.setUserActive(true);
        return user;
    }

    public static <T extends Entry> void cursorRowToEntry(Cursor cursor, T entry) {
        ContentValues values = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(cursor, values);
        entry.fromContentValues(values);
    }

    public static <T extends Entry> List<T> getEntryListFromCursor(Cursor cursor, Class<T> type) {
        List<T> entries = new ArrayList<>();
        if (cursor == null || !cursor.moveToFirst()) return entries;
        entries.clear();
        int i = 0;
        do {
            try {
                entries.add(type.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                Timber.e(e);
            }
            cursorRowToEntry(cursor, entries.get(i++));
        } while (cursor.moveToNext());
        return entries;
    }

    public static boolean dateIsCurrent(long dateStamp) {
        Calendar anchorCalendar = Calendar.getInstance();
        Calendar currentCalendar = Calendar.getInstance();
        anchorCalendar.setTimeInMillis(dateStamp);
        currentCalendar.setTimeInMillis(System.currentTimeMillis());
        return anchorCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                anchorCalendar.get(Calendar.DAY_OF_MONTH) == currentCalendar.get(Calendar.DAY_OF_MONTH) &&
                anchorCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR);
    }
}