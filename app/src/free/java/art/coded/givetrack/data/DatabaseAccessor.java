package art.coded.givetrack.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import art.coded.givetrack.AppUtilities;
import art.coded.givetrack.AppWidget;
import art.coded.givetrack.R;
import art.coded.givetrack.data.DatabaseContract.*;
import art.coded.givetrack.data.entry.Company;
import art.coded.givetrack.data.entry.Entry;
import art.coded.givetrack.data.entry.Spawn;
import art.coded.givetrack.data.entry.Target;
import art.coded.givetrack.data.entry.Record;
import art.coded.givetrack.data.entry.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

/**
 * Accesses and simultaneously operates on local and remote databases to manage user requests.
 */
final class DatabaseAccessor {

    static void fetchSpawn(Context context) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        User user =  getActiveUserFromLocal(FirebaseAuth.getInstance(), local);

        Map<String, String> request = new HashMap<>();
        if (user.getIndexFocus()) request.put(DatasourceContract.PARAM_EIN, user.getIndexCompany());
        else {
            request.put(DatasourceContract.PARAM_SEARCH, user.getIndexTerm());
            request.put(DatasourceContract.PARAM_CITY, user.getIndexCity());
            request.put(DatasourceContract.PARAM_STATE, user.getIndexState());
            request.put(DatasourceContract.PARAM_ZIP, user.getIndexZip());
            request.put(DatasourceContract.PARAM_MIN_RATING, user.getIndexMinrating());
            request.put(DatasourceContract.PARAM_FILTER, user.getIndexFilter() ? "1" : "0");
            request.put(DatasourceContract.PARAM_RATED, user.getIndexRanked()? "1" : "0");
            request.put(DatasourceContract.PARAM_PAGE_NUM, user.getIndexPages());
            request.put(DatasourceContract.PARAM_PAGE_SIZE, user.getIndexRows());
        }

        Uri.Builder builder = Uri.parse(DatasourceContract.BASE_URL).buildUpon();
        builder.appendPath(DatasourceContract.API_PATH_ORGANIZATIONS);

        // Append required parameters
        builder.appendQueryParameter(DatasourceContract.PARAM_APP_ID, context.getString(R.string.cn_app_id));
        builder.appendQueryParameter(DatasourceContract.PARAM_APP_KEY, context.getString(R.string.cn_app_key));

        boolean single = request.containsKey(DatasourceContract.PARAM_EIN);
        if (single) builder.appendPath(request.get(DatasourceContract.PARAM_EIN));
        else {
            // Append optional parameters
            for (String param : DatasourceContract.OPTIONAL_PARAMS) {
                if (request.containsKey(param)) {
                    String value = request.get(param);
                    if (value != null && !value.equals("")) {
                        if (value.equals("0")) continue;
                        if (value.contains(" ")) value = value.replace(" ", "%20");
                        builder.appendQueryParameter(param, value);
                        if (param.equals(DatasourceContract.PARAM_SEARCH)) builder.appendQueryParameter(DatasourceContract.PARAM_SORT, "RELEVANCE:DESC");
                        if (param.equals(DatasourceContract.PARAM_RATED)) builder.appendQueryParameter(DatasourceContract.PARAM_SORT, "RATING:DESC");
                    }
                }
            }
        }
        URL url = DataUtilities.getUrl(builder.build());

        // Retrieve data
        String response = DataUtilities.requestResponseFromUrl(url, null);
        if (response == null) return;
        Spawn[] parsedResponse = DataUtilities.parseSpawns(response, user.getUid(), single);

        // Store data
        long stamp = System.currentTimeMillis();
        removeEntriesFromLocal(local, Spawn.class, stamp);
        addEntriesToLocal(local, Spawn.class, stamp, parsedResponse);
        addEntriesToRemote(remote, Spawn.class, stamp, parsedResponse);
    }

    @SafeVarargs static List<Spawn> getSpawn(Context context, Pair<String, String>... where) {
        ContentResolver local = context.getContentResolver();

        User activeUser = getActiveUserFromLocal(FirebaseAuth.getInstance(), local);
        Uri contentUri = CompanyEntry.CONTENT_URI_SPAWN;

        String selection = CompanyEntry.COLUMN_UID + " = ? ";
        List<String> selectionArgList = new ArrayList<>();
        selectionArgList.add(activeUser.getUid());

        if (where != null) {
            for (Pair<String, String> p : where) {
                selection = selection.concat(", " + p.first);
                selectionArgList.add(p.second);
            }
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        Cursor cursor = local.query(
                contentUri, null, selection, selectionArgs, null
        );
        List<Spawn> entries = AppUtilities.getEntryListFromCursor(cursor, Spawn.class);
        if (cursor != null) cursor.close();
        return entries;
    }

    static void addSpawn(Context context, Spawn... entries) {
        ContentResolver local = context.getContentResolver();

        long stamp = System.currentTimeMillis();
        addEntriesToLocal(local, Spawn.class, stamp, entries);
    }

    static void removeSpawn(Context context, Spawn... spawns) {
        ContentResolver local = context.getContentResolver();

        long stamp = System.currentTimeMillis();
        removeEntriesFromLocal(local, Spawn.class, stamp, spawns);
    }

    static void fetchTarget(Context context) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        validateEntries(local, remote, Target.class);
        AppWidget.refresh(context);
    }

    @SafeVarargs static List<Target> getTarget(Context context, Pair<String, String>... where) {
        ContentResolver local = context.getContentResolver();

        User activeUser = getActiveUserFromLocal(FirebaseAuth.getInstance(), local);
        Uri contentUri = CompanyEntry.CONTENT_URI_TARGET;

        String selection = CompanyEntry.COLUMN_UID + " = ? ";
        List<String> selectionArgList = new ArrayList<>();
        selectionArgList.add(activeUser.getUid());

        if (where != null) {
            for (Pair<String, String> p : where) {
                selection = selection.concat("AND " + p.first);
                selectionArgList.add(p.second);
            }
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        Cursor cursor = local.query(
                contentUri, null, selection, selectionArgs, null
        );
        List<Target> entries = AppUtilities.getEntryListFromCursor(cursor, Target.class);
        if (cursor != null) cursor.close();
        return entries;
    }

    static void addTarget(Context context, Target... entries) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();
        addEntriesToLocal(local, Target.class, stamp, entries);
        addEntriesToRemote(remote, Target.class, stamp, entries);
        AppWidget.refresh(context);
    }

    static void removeTarget(Context context, Target... target) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();

        removeEntriesFromLocal(local, Target.class, stamp, target);
        removeEntriesFromRemote(remote, Target.class, stamp, target);
        AppWidget.refresh(context);
    }

    static void fetchRecord(Context context) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        validateEntries(local, remote, Record.class);
        AppWidget.refresh(context);
    }

    @SafeVarargs static List<Record> getRecord(Context context, Pair<String, String>... where) {
        ContentResolver local = context.getContentResolver();

        User activeUser = getActiveUserFromLocal(FirebaseAuth.getInstance(), local);
        Uri contentUri = CompanyEntry.CONTENT_URI_RECORD;

        String selection = CompanyEntry.COLUMN_UID + " = ? ";
        List<String> selectionArgList = new ArrayList<>();
        selectionArgList.add(activeUser.getUid());

        if (where != null) {
            for (Pair<String, String> p : where) {
                selection = selection.concat(", " + p.first);
                selectionArgList.add(p.second);
            }
        }
        String[] selectionArgs = selectionArgList.toArray(new String[0]);

        Cursor cursor = local.query(
                contentUri, null, selection, selectionArgs, null
        );
        List<Record> entries = AppUtilities.getEntryListFromCursor(cursor, Record.class);

        if (cursor != null) cursor.close();
        return entries;
    }

    static void addRecord(Context context, Record... entries) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();
        addEntriesToLocal(local, Record.class, stamp, entries);
        addEntriesToRemote(remote, Record.class, stamp, entries);
        AppWidget.refresh(context);
    }

    static void removeRecord(Context context, Record... record) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();
        removeEntriesFromLocal(local, Record.class, stamp, record);
        removeEntriesFromRemote(remote, Record.class, stamp,  record);
        AppWidget.refresh(context);
    }

    static void fetchUser(Context context) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        validateEntries(local, remote, User.class);
    }

    static List<User> getUser(Context context) {
        ContentResolver local = context.getContentResolver();

        User activeUser = getActiveUserFromLocal(FirebaseAuth.getInstance(), local);
        Uri contentUri = UserEntry.CONTENT_URI_USER;
        Cursor cursor = local.query(
                contentUri, null, CompanyEntry.COLUMN_UID + " = ? ", new String[] { activeUser.getUid() }, null
        );
        List<User> entries = AppUtilities.getEntryListFromCursor(cursor, User.class);
        if (cursor != null) cursor.close();
        return entries;
    }

    static void addUser(Context context, User... entries) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();
        addEntriesToLocal(local, User.class, stamp, entries);
        addEntriesToRemote(remote, User.class, stamp, entries);
    }

    static void removeUser(Context context, User... user) {
        ContentResolver local = context.getContentResolver();
        FirebaseDatabase remote = FirebaseDatabase.getInstance();

        long stamp = System.currentTimeMillis();
        removeEntriesFromLocal(local, User.class, stamp, user);
        removeEntriesFromRemote(remote, User.class, stamp, user);
    }

    @SafeVarargs private static <T extends Entry> void addEntriesToLocal(ContentResolver local, Class<T> entryType, long stamp, T... entries) {

        Uri contentUri = DataUtilities.getContentUri(entryType);

        String uid = "";
        if (entries == null || entries.length == 0) {
            uid = getActiveUserFromLocal(FirebaseAuth.getInstance(), local).getUid();
        } else if (entries[0] != null) uid = entries[0].getUid();


        if (uid == null || uid.isEmpty()) return;
//        if (reset) local.delete(contentUri, UserEntry.COLUMN_UID + " = ? ", new String[] { uid });

        if (entries != null && entries.length > 0) {
            ContentValues[] values = new ContentValues[entries.length];
            for (int i = 0; i < values.length; i++) {
                if (entries[i] == null) continue;
                values[i] = entries[i].toContentValues();
            }
            local.bulkInsert(contentUri, values);
            local.notifyChange(contentUri, null);
        }

        updateLocalTableTime(local, entryType, stamp, uid);
    }

    @SafeVarargs private static <T extends Entry> void addEntriesToRemote(FirebaseDatabase remote, Class<T> entryType, long stamp,  T... entries) {

        if (entryType == Spawn.class) return;

        String entryPath = entryType.getSimpleName().toLowerCase();
        DatabaseReference typeReference = remote.getReference(entryPath);

        String uid = "";
        if (entries == null || entries.length == 0) {
            uid = getActiveUserFromRemote(FirebaseAuth.getInstance(), remote).getUid();
        } else if (entries[0] != null) uid = entries[0].getUid();

        if (uid == null || uid.isEmpty()) return;

        DatabaseReference userReference = typeReference.child(uid);

//        if (reset) userReference.removeValue();

        DatabaseReference entryReference;
        if (entries != null && entries.length > 0) {
            for (T entry : entries) {
                if (entry == null) continue;
                entryReference = userReference;
                if (entry instanceof Company) entryReference = entryReference.child(entry.getId());
                entryReference.updateChildren(entry.toParameterMap());
            }
        }
        updateRemoteTableTime(remote, entryType, stamp, uid);
    }

    @SafeVarargs private static <T extends Entry> void removeEntriesFromLocal(ContentResolver local, Class<T> entryType, long stamp, T... entries) {

        Uri contentUri = DataUtilities.getContentUri(entryType);
        String uid = "";
        if (entries == null || entries.length == 0) {
            uid = getActiveUserFromLocal(FirebaseAuth.getInstance(), local).getUid();
            if (uid == null || uid.isEmpty()) return;
            local.delete(contentUri, UserEntry.COLUMN_UID + " = ?", new String[] { uid });
            local.notifyChange(contentUri, null);
        } else {
                if (entries[0] != null) uid = entries[0].getUid();
                for (Entry entry : entries) {
                    if (entry == null) continue;
                    Uri rowUri = contentUri.buildUpon().appendPath(String.valueOf(entry.getId())).build();
                    local.delete(rowUri, null, null);
                    local.notifyChange(rowUri, null);
            }
        }
        // Do not update user stamp to prevent recreating user entry on account deletion
        if (entryType != User.class) updateLocalTableTime(local, entryType, stamp, uid);
    }

    @SafeVarargs private static <T extends Entry> void removeEntriesFromRemote(FirebaseDatabase remote, Class<T> entryType, long stamp, T... entries) {

        if (entryType == Spawn.class) return;

        String entryPath = entryType.getSimpleName().toLowerCase();
        DatabaseReference typeReference = remote.getReference(entryPath);

        String uid = "";
        if (entries == null || entries.length == 0) {
            uid = getActiveUserFromRemote(FirebaseAuth.getInstance(), remote).getUid();
        } else if (entries[0] != null) uid = entries[0].getUid();

        DatabaseReference userReference = typeReference.child(uid);

        DatabaseReference entryReference;
        if (entries == null || entries.length == 0) {
            User user = getActiveUserFromRemote(FirebaseAuth.getInstance(), remote);
            uid = user.getUid();
            if (uid == null || uid.isEmpty()) return;
            userReference.removeValue();
        } else {
            for (T entry : entries) {
                if (entry == null) continue;
                entryReference = userReference;
                if (entry instanceof Company) entryReference = entryReference.child(entry.getId());
                entryReference.removeValue();
            }
        }
        // Do not update user stamp to prevent recreating user entry on account deletion
        if (entryType != User.class) updateRemoteTableTime(remote, entryType, stamp, uid);
    }

    private static User getActiveUserFromLocal(FirebaseAuth auth, ContentResolver local) {

        String uid = auth.getUid();
        User u = User.getDefault();
        Cursor data = local.query(UserEntry.CONTENT_URI_USER, null, UserEntry.COLUMN_UID + " = ?", new String[] { uid }, null);
        if (data == null) return u;
        if (data.moveToFirst()) AppUtilities.cursorRowToEntry(data, u);
        return u;
    }

    private static User getActiveUserFromRemote(FirebaseAuth auth, FirebaseDatabase remote) {

        String uid = auth.getUid();

        TaskCompletionSource<User> taskSource = new TaskCompletionSource<>();

        User u = User.getDefault();
        if (uid == null) return u;

        DatabaseReference entryReference = remote.getReference(User.class.getSimpleName().toLowerCase()).child(uid);
        entryReference.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User u = dataSnapshot.getValue(User.class);
                if (u == null) u = User.getDefault();
                taskSource.trySetResult(u);
                entryReference.removeEventListener(this);
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        Task<User> task = taskSource.getTask();
        try { Tasks.await(task, 2, TimeUnit.SECONDS); }
        catch (ExecutionException|InterruptedException|TimeoutException e) { task = Tasks.forException(e); }

        if (task.isSuccessful()) u = task.getResult();
        return u;
    }

    private static <T extends Entry> void updateLocalTableTime(ContentResolver local, Class<T> entryType, long stamp, String uid) {

        if (uid == null || uid.isEmpty()) return;
        Uri uri = UserEntry.CONTENT_URI_USER.buildUpon().appendPath(uid).build();

        ContentValues companyValues = new ContentValues();
        companyValues.put(DataUtilities.getTimeTableColumn(entryType), stamp);
        local.update(uri, companyValues, null,null);
        local.notifyChange(uri, null);
    }

    private static <T extends Entry> void updateRemoteTableTime(FirebaseDatabase remote, Class<T> entryType, long stamp, String uid) {

        if (entryType.equals(Spawn.class) || uid == null || uid.isEmpty()) return;
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put(DataUtilities.getTimeTableColumn(entryType), stamp);
        DatabaseReference userReference = remote.getReference(User.class.getSimpleName().toLowerCase());
        userReference.child(uid).updateChildren(entryMap);
    }

    private static <T extends Entry> void pullLocalToRemoteEntries(ContentResolver local, FirebaseDatabase remote, Class<T> entryType, long stamp, String uid) {
        if (uid == null || uid.isEmpty()) return;
        Uri contentUri = DataUtilities.getContentUri(entryType);
        Cursor cursor = local.query(contentUri, null, UserEntry.COLUMN_UID + " = ? ", new String[]{ uid }, null);
        if (cursor == null) return;
        List<T> entryList = AppUtilities.getEntryListFromCursor(cursor, entryType);
        cursor.close();
        if (entryList.isEmpty()) return;
        if (entryType == User.class) {
            ((User) entryList.get(0)).setUserActive(true);
            local.update(UserEntry.CONTENT_URI_USER.buildUpon().appendPath(uid).build(), entryList.get(0).toContentValues(), null, null);
        }
        removeEntriesFromRemote(remote, entryType, stamp);
        if (entryList.isEmpty()) return;
        addEntriesToRemote(remote, entryType, stamp, entryList.toArray((T[]) Array.newInstance(entryType, entryList.size())));
    }

    private static <T extends Entry> void pullRemoteToLocalEntries(ContentResolver local, FirebaseDatabase remote, Class<T> entryType, long stamp, String uid) {

        String path = entryType.getSimpleName().toLowerCase();
        DatabaseReference pathReference = remote.getReference(path).child(uid);
        pathReference.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<T> entryList = new ArrayList<>();
                if (entryType == User.class) {
                    T entry = dataSnapshot.getValue(entryType);
                    if (entry instanceof User) {
                        ((User) entry).setRecordStamp(0);     // Resets User stamps
                        ((User) entry).setTargetStamp(0);     // Resets User stamps
                        ((User) entry).setUserActive(true);
                    }
                    entryList.add(entry);
                } else {
                    Iterable<DataSnapshot> iterable = dataSnapshot.getChildren();
                    for (DataSnapshot s : iterable) {
                        T entry = s.getValue(entryType);
                        entryList.add(entry);
                    }
                }
                removeEntriesFromLocal(local, entryType, stamp);
                if (entryList.isEmpty()) return;
                addEntriesToLocal(local, entryType, stamp, entryList.toArray((T[]) Array.newInstance(entryType, entryList.size())));
                pathReference.removeEventListener(this);
                if (entryType == User.class) pathReference.child("userActive").setValue(true);
            }
            @Override public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

   private static <T extends Entry> void validateEntries(@NonNull ContentResolver local, @NonNull FirebaseDatabase remote, Class<T> entryType) {

        User localUser = getActiveUserFromLocal(FirebaseAuth.getInstance(), local);
        User remoteUser = getActiveUserFromRemote(FirebaseAuth.getInstance(), remote);

        long localTableStamp = DataUtilities.getTableTime(entryType, localUser);
        long remoteTableStamp = DataUtilities.getTableTime(entryType, remoteUser);
        int compareLocalToRemote = Long.compare(localTableStamp, remoteTableStamp);

        if (compareLocalToRemote < 0) pullRemoteToLocalEntries(local, remote, entryType, remoteTableStamp, remoteUser.getUid());
        else {
            if (compareLocalToRemote > 0 || (localTableStamp != 0 && entryType == User.class))
                pullLocalToRemoteEntries(local, remote, entryType, localTableStamp, localUser.getUid()); // Ensures user active status is set to true where databases are initialized and equivalent
            local.notifyChange(DataUtilities.getContentUri(entryType), null);
        }
    }
}