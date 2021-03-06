package art.coded.givetrack.data;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.preference.PreferenceManager;

import androidx.core.util.Pair;

import art.coded.calibrater.Calibrater;
import art.coded.givetrack.AppExecutors;
import art.coded.givetrack.AppUtilities;
import art.coded.givetrack.data.entry.Company;
import art.coded.givetrack.data.entry.Spawn;
import art.coded.givetrack.data.entry.Target;
import art.coded.givetrack.data.entry.Record;
import art.coded.givetrack.data.entry.User;
import art.coded.rateraid.Rateraid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Interfaces with data requests from the UI thread and delegates them to a separate handler thread.
 */
public final class DatabaseManager extends IntentService {

    private static final Executor DISK_IO = AppExecutors.getInstance().getDiskIO();
    private static final Executor NETWORK_IO = AppExecutors.getInstance().getNetworkIO();
    private static final Executor MAIN_THREAD = AppExecutors.getInstance().getMainThread();

    private static final String ACTION_FETCH_SPAWN = "art.coded.givetrack.data.action.FETCH_SPAWN";
    private static final String ACTION_FETCH_TARGET = "art.coded.givetrack.data.action.FETCH_TARGET";
    private static final String ACTION_FETCH_RECORD = "art.coded.givetrack.data.action.FETCH_RECORD";
    private static final String ACTION_FETCH_USER = "art.coded.givetrack.data.action.FETCH_USER";
    private static final String ACTION_REMOVE_SPAWN = "art.coded.givetrack.data.action.REMOVE_SPAWN";
    private static final String ACTION_REMOVE_TARGET = "art.coded.givetrack.data.action.REMOVE_TARGET";
    private static final String ACTION_REMOVE_RECORD = "art.coded.givetrack.data.action.REMOVE_RECORD";
    private static final String ACTION_REMOVE_USER = "art.coded.givetrack.data.action.REMOVE_USER";
    private static final String ACTION_RESET_SPAWN = "art.coded.givetrack.data.action.RESET_SPAWN";
    private static final String ACTION_RESET_TARGET = "art.coded.givetrack.data.action.RESET_TARGET";
    private static final String ACTION_RESET_RECORD = "art.coded.givetrack.data.action.RESET_RECORD";
    private static final String ACTION_RESET_USER = "art.coded.givetrack.data.action.RESET_USER";
    private static final String ACTION_TARGET_SPAWN = "art.coded.givetrack.data.action.GIVE_SPAWN";
    private static final String ACTION_UNTARGET_COMPANY = "art.coded.givetrack.data.action.UNTARGET_COMPANY";
    private static final String ACTION_RECORD_TARGET = "art.coded.givetrack.data.action.RECORD_TARGET";
    private static final String ACTION_TARGET_RECORD = "art.coded.givetrack.data.action.TARGET_RECORD";
    private static final String ACTION_UPDATE_TARGET = "art.coded.givetrack.data.action.UPDATE_TARGET";
    private static final String ACTION_UPDATE_CONTACT = "art.coded.givetrack.data.action.UPDATE_CONTACT";
    private static final String ACTION_UPDATE_RECORD = "art.coded.givetrack.data.action.UPDATE_RECORD";
    private static final String ACTION_UPDATE_USER = "art.coded.givetrack.data.action.UPDATE_USER";
    private static final String ACTION_RESET_DATA = "art.coded.givetrack.data.action.RESET_DATA";
    private static final String EXTRA_API_REQUEST = "art.coded.givetrack.data.extra.API_REQUEST";
    private static final String EXTRA_ITEM_VALUES = "art.coded.givetrack.data.extra.ITEM_VALUES";
    private static final String EXTRA_LIST_VALUES = "art.coded.givetrack.data.extra.LIST_VALUES";
    private static final String EXTRA_ITEM_ID = "art.coded.givetrack.data.extra.ITEM_ID";


    /**
     * Creates an {@link IntentService} instance.
     */
    public DatabaseManager() {
        super(DatabaseManager.class.getSimpleName());
    }

    /**
     * Starts this service to perform action FetchSpawn with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchSpawn(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_FETCH_SPAWN);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action FetchTarget with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchTarget(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_FETCH_TARGET);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action FetchRecord with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchRecord(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_FETCH_RECORD);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action FetchUser with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionFetchUser(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_FETCH_USER);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action TargetRecord with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionTargetRecord(Context context, Record record) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_TARGET_RECORD);
        intent.putExtra(EXTRA_ITEM_VALUES, record);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action UpdateTargetwith the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateTarget(Context context, Target... target) {
        // Update from list
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_UPDATE_TARGET);
        intent.putExtra(EXTRA_ITEM_VALUES, target);
        if (target.length == 1) intent.putExtra(EXTRA_ITEM_VALUES, target[0]);
        else intent.putExtra(EXTRA_LIST_VALUES, target);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RemoveTarget with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRemoveTarget(Context context, Target... targets) {
        // Update from list
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_REMOVE_TARGET);
        if (targets.length > 1) intent.putExtra(EXTRA_LIST_VALUES, targets);
        else intent.putExtra(EXTRA_ITEM_VALUES, targets[0]);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RecordTarget with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRecordTarget(Context context, Target... target) {
        if (context == null) return;
        // Update from list
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RECORD_TARGET);
        if (target.length > 1) intent.putExtra(EXTRA_LIST_VALUES, target);
        intent.putExtra(EXTRA_ITEM_VALUES, target[0]);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action GiveSpawn with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionTargetSpawn(Context context, Spawn spawn) {
        // Update from element
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_TARGET_SPAWN);
        intent.putExtra(EXTRA_ITEM_VALUES, spawn);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action UntargetCompany with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUntargetCompany(Context context, Company company) {
        // Update from element
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_UNTARGET_COMPANY);
        intent.putExtra(EXTRA_ITEM_VALUES, company.getEin());
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RemoveSpawn with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRemoveSpawn(Context context, Spawn... spawns) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_REMOVE_SPAWN);
        if (spawns.length > 1) intent.putExtra(EXTRA_LIST_VALUES, spawns);
        else intent.putExtra(EXTRA_ITEM_VALUES, spawns[0]);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RemoveRecord with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRemoveRecord(Context context, Record... record) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_REMOVE_RECORD);
        if (record.length > 1) intent.putExtra(EXTRA_LIST_VALUES, record);
        else intent.putExtra(EXTRA_ITEM_VALUES, record[0]);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action RemoveUser with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionRemoveUser(Context context, User... user) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_REMOVE_USER);
        if (user.length > 1) intent.putExtra(EXTRA_LIST_VALUES, user);
        else intent.putExtra(EXTRA_ITEM_VALUES, user[0]);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ResetSpawn with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetSpawn(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RESET_SPAWN);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ResetTargetwith the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetTarget(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RESET_TARGET);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ResetRecord with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetRecord(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RESET_RECORD);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action ResetUser with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetUser(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RESET_USER);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action UpdateRecord with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateRecord(Context context, Record... record) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_UPDATE_RECORD);
        intent.putExtra(EXTRA_ITEM_VALUES, record);
        if (record.length > 1) intent.putExtra(EXTRA_LIST_VALUES, record);
        else intent.putExtra(EXTRA_ITEM_VALUES, record[0]);

        context.startService(intent);
    }

    /**
     * Starts this service to perform action UpdateUser with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateUser(Context context, User... user) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_UPDATE_USER);
        intent.putExtra(EXTRA_ITEM_VALUES, user);
        if (user.length > 1) intent.putExtra(EXTRA_LIST_VALUES, user);
        else intent.putExtra(EXTRA_ITEM_VALUES, user[0]);

        context.startService(intent);
    }

    /**
     * Starts this service to perform action ResetData with the given parameters.
     * If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionResetData(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, DatabaseManager.class);
        intent.setAction(ACTION_RESET_DATA);
        context.startService(intent);
    }

    /**
     * Syncs data inside a worker thread on requests to process {@link Intent}
     * Enqueues and handles subsequent requests sequentially.
     *
     * @param intent launches this {@link IntentService}.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        DISK_IO.execute(() -> {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_FETCH_SPAWN:
                    handleActionFetchSpawn();
                    break;
                case ACTION_FETCH_TARGET:
                    handleActionFetchTarget();
                    break;
                case ACTION_FETCH_RECORD:
                    handleActionFetchRecord();
                    break;
                case ACTION_FETCH_USER:
                    handleActionFetchUser();
                    break;
                case ACTION_TARGET_SPAWN:
                    handleActionTargetSpawn(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_TARGET_RECORD:
                    handleActionTargetRecord(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_UNTARGET_COMPANY:
                    handleActionUntargetCompany(intent.getStringExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_RECORD_TARGET:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionRecordTarget(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Target.class));
                    else handleActionRecordTarget(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_REMOVE_SPAWN:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionRemoveSpawn(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Spawn.class));
                    else handleActionRemoveSpawn(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_REMOVE_TARGET:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionRemoveTarget(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Target.class));
                    else handleActionRemoveTarget(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_REMOVE_RECORD:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionRemoveRecord(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Record.class));
                    else handleActionRemoveRecord(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_REMOVE_USER:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionRemoveUser(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), User.class));
                    else handleActionRemoveUser(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_RESET_SPAWN:
                    handleActionResetSpawn();
                    break;
                case ACTION_RESET_TARGET:
                    handleActionResetTarget();
                    break;
                case ACTION_RESET_RECORD:
                    handleActionResetRecord();
                    break;
                case ACTION_RESET_USER:
                    handleActionResetUser();
                    break;
                case ACTION_UPDATE_CONTACT:
                    break;
                case ACTION_UPDATE_TARGET:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionUpdateTarget(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Target.class));
                    else handleActionUpdateTarget(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_UPDATE_RECORD:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionUpdateRecord(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), Record.class));
                    else handleActionUpdateRecord(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_UPDATE_USER:
                    if (intent.hasExtra(EXTRA_LIST_VALUES))
                        handleActionUpdateUser(AppUtilities.getTypedArrayFromParcelables(intent.getParcelableArrayExtra(EXTRA_LIST_VALUES), User.class));
                    else handleActionUpdateUser(intent.getParcelableExtra(EXTRA_ITEM_VALUES));
                    break;
                case ACTION_RESET_DATA:
                    handleActionResetData();
            }
        });
    }

    /**
     * Handles action FetchSpawn on the service worker thread.
     */
    private void handleActionFetchSpawn() { NETWORK_IO.execute(() -> DatabaseAccessor.fetchSpawn(this)); }

    /**
     * Handles action FetchTarget on the service worker thread.
     */
    private void handleActionFetchTarget() { DatabaseAccessor.fetchTarget(this); }

    /**
     * Handles action FetchRecord on the service worker thread.
     */
    private void handleActionFetchRecord() { DatabaseAccessor.fetchRecord(this); }

    /**
     * Handles action FetchUser on the service worker thread.
     */
    private void handleActionFetchUser() { DatabaseAccessor.fetchUser(this); }

    /**
     * Handles action GiveSpawn on the service worker thread.
     */
    private void handleActionTargetSpawn(Spawn spawn) {

        float impact = 0f;
        int frequency = 0;

//        DatabaseAccessor.addSpawn(this, spawn);

        List<Record> records = DatabaseAccessor.getRecord(this);
        for (Record record : records) {
            if (record.getEin().equals(spawn.getEin())) {
                impact += record.getImpact();
                frequency++;
            }
        }

        List<Target> targets = DatabaseAccessor.getTarget(this);
        int size = targets.size();
        double percent = size == 0 ? 1d : 0d;
        Target target = Target.fromSuper(spawn);
        target.setFrequency(frequency);
        target.setPercent(percent);
        target.setImpact(impact);

        String phoneNumber = DataUtilities.urlToPhoneNumber(target);
        target.setPhone(phoneNumber);

        String emailAddress = DataUtilities.urlToEmailAddress(target);
        target.setEmail(emailAddress);

        String socialHandle = DataUtilities.urlToSocialHandle(target);
        target.setSocial(socialHandle);

        List<User> users = DatabaseAccessor.getUser(this);
        for (User u : users) if (u.getUserActive()) {
            double totalImpact = u.getGiveImpact();
            float threshold = (size + 1) * .3f;
            if (totalImpact < threshold) {
                u.setGiveImpact(threshold);
                DatabaseAccessor.addUser(this, u);
            }
        }

        DatabaseAccessor.addTarget(this, target);
    }

    /**
     * Handles action TargetRecord on the service worker thread.
     */
    private void handleActionTargetRecord(Record record) {

        double impact = 0d;
        String ein = record.getEin();
        List<Record> recordList = DatabaseAccessor.getRecord(this);

        for (Record r : recordList)
            if (r.getEin().equals(ein)) impact += r.getImpact();

        List<Target> targetList = DatabaseAccessor.getTarget(this);
        for (Target t : targetList)
            if (t.getEin().equals(ein)) {
                t.setImpact(impact);
                DatabaseAccessor.addTarget(this, t);
                return;
            }
    }

    /**
     * Handles action RecordTarget on the service worker thread.
     */
    private void handleActionRecordTarget(Target... target) {

        User activeUser;
        List<User> userList = DatabaseAccessor.getUser(this);
        if (userList.size() > 1) { startActionFetchUser(this); return; }
        else activeUser = userList.get(0);

        if (activeUser.getGiveTiming() == 0 && !AppUtilities.dateIsCurrent(activeUser.getGiveAnchor())) {
            activeUser.setGiveAnchor(System.currentTimeMillis());
            DatabaseManager.startActionUpdateUser(this, activeUser);
        }

        double giveImpact = activeUser.getGiveImpact();

        for (Target t : target) {
            if (t.getPercent() == 0d) continue;
            t.setFrequency(t.getFrequency() + 1);
            double transactionImpact = t.getPercent() * giveImpact;
            double totalImpact = t.getImpact() + transactionImpact;
            int round = activeUser.getGiveRounding();
            String impactStr;
            switch (round) {
                case 0: impactStr = String.valueOf((Math.floor(totalImpact * 100)) / 100); break;
                case 1: impactStr = String.format(Locale.getDefault(), "%.2f", totalImpact); break;
                default: impactStr = String.valueOf(totalImpact);
            }
            t.setImpact(Double.parseDouble(impactStr));
        } DatabaseAccessor.addTarget(this, target);

        List<Record> records = new ArrayList<>();
        for (int i = 0; i < target.length; i++) {
            if (target[i].getPercent() == 0d) continue;
            double transactionImpact = target[i].getPercent() * giveImpact;
            activeUser.setGiveAnchor(activeUser.getGiveAnchor() + 1);
            long time = activeUser.getGiveAnchor();
            Record record = Record.fromSuper(target[i].getSuper());
            record.setStamp(System.currentTimeMillis() + i);
            record.setTime(time);
            int round = activeUser.getGiveRounding();
            String impactStr;
            switch (round) {
                case 0: impactStr = String.valueOf((Math.floor(transactionImpact * 100)) / 100); break;
                case 1: impactStr = String.format(Locale.getDefault(), "%.2f", transactionImpact); break;
                default: impactStr = String.valueOf(transactionImpact);
            }
            record.setImpact(Double.parseDouble(impactStr));
            records.add(record);
        } DatabaseAccessor.addRecord(this, records.toArray(new Record[0]));

        if (activeUser.getGiveTiming() == 1) {
            activeUser.setGiveAnchor(System.currentTimeMillis());
            activeUser.setGiveTiming(0);
        } DatabaseAccessor.addUser(this, activeUser);
    }

    /**
     * Handles action UntargetCompany on the service worker thread.
     */
    private void handleActionUntargetCompany(String ein) {

        Pair<String, String> where = new Pair<>(DatabaseContract.CompanyEntry.COLUMN_EIN + " = ? ", ein);
        List<Target> untargetList = DatabaseAccessor.getTarget(this, where);

        if (!untargetList.isEmpty()) {

            Target untarget = untargetList.get(0);

            List<Target> targetList = DatabaseAccessor.getTarget(this);
            int untargetIndex = 0;
            for (int i = 0; i < targetList.size(); i++)
                if (targetList.get(i).getEin().equals(ein)) untargetIndex = i;
            targetList.remove(untargetIndex);

            Rateraid.recalibrateRatings(targetList, false, Calibrater.STANDARD_PRECISION);

            DatabaseAccessor.removeTarget(this, untarget);
            DatabaseAccessor.addTarget(this, targetList.toArray(new Target[0]));
        }
    }

    /**
     * Handles action RemoveSpawn on the service worker thread.
     */
    private void handleActionRemoveSpawn(Spawn... spawns) {
        DatabaseAccessor.removeSpawn(this, spawns);
    }

    /**
     * Handles action RemoveTarget on the service worker thread.
     */
    private void handleActionRemoveTarget(Target... targets) {
        DatabaseAccessor.removeTarget(this, targets);
    }

    /**
     * Handles action RemoveRecord on the service worker thread.
     */
    private void handleActionRemoveRecord(Record... records) {

        DatabaseAccessor.removeRecord(this, records);

        List<Target> targets = DatabaseAccessor.getTarget(this);
        for (Target target : targets) {
            for (Record record : records) {
                if (record.getEin().equals(target.getEin())) {
                    target.setFrequency(target.getFrequency() - 1);
                    double impact = target.getImpact() - record.getImpact();
                    target.setImpact(impact);
                    DatabaseAccessor.addTarget(this, target);
                    break;
                }
            }
        }
    }

    /**
     * Handles action RemoveUser on the service worker thread.
     */
    private void handleActionRemoveUser(User... users) {

        List<Spawn> spawns = DatabaseAccessor.getSpawn(this);
        List<Target> targets = DatabaseAccessor.getTarget(this);
        List<Record> records = DatabaseAccessor.getRecord(this);

        for (User user : users) {
            for (Spawn spawn : spawns)
                if (spawn.getUid().equals(user.getUid()))
                    DatabaseAccessor.removeSpawn(this, spawn);
            for (Target target : targets)
                if (target.getUid().equals(user.getUid()))
                    DatabaseAccessor.removeTarget(this, target);
            for (Record record : records)
                if (record.getUid().equals(user.getUid()))
                    DatabaseAccessor.removeRecord(this, record);
        }
        DatabaseAccessor.removeUser(this, users);
    }

    /**
     * Handles action ResetSpawn on the service worker thread.
     */
    private void handleActionResetSpawn() {
        DatabaseAccessor.removeSpawn(this);
    }

    /**
     * Handles action ResetTarget on the service worker thread.
     */
    private void handleActionResetTarget() {
        DatabaseAccessor.removeTarget(this);
    }

    /**
     * Handles action ResetRecord in the provided background.
     */
    private void handleActionResetRecord() {

        DatabaseAccessor.removeRecord(this);
        List<Target> targets = DatabaseAccessor.getTarget(this);
        for (Target target : targets) {
            target.setImpact(0);
            target.setFrequency(0);
        }
        DatabaseAccessor.addTarget(this, targets.toArray(new Target[0]));
    }

    /**
     * Handles action ResetUser on the service worker thread.
     */
    private void handleActionResetUser() {

        DatabaseAccessor.removeSpawn(this);
        DatabaseAccessor.removeTarget(this);
        DatabaseAccessor.removeRecord(this);
        DatabaseAccessor.removeUser(this);
    }

    /**
     * Handles action UpdatePercent on the service worker thread.
     */
    private void handleActionUpdateTarget(Target... targets) {
        int offset = 0;
        int offsetIndex = 0;
        // If parameter list is identical to persisted list short one element, remove the element from the persisted lists
        List<Target> persistedList = DatabaseAccessor.getTarget(this);
        List<Target> updatedList = Arrays.asList(targets);
        if (persistedList.size() - 1 == targets.length)
            for (int i = 0; i < persistedList.size(); i++) {
                if (!updatedList.contains(persistedList.get(i))) {
                    offset++;
                    offsetIndex = i;
                }
            }

        if (targets.length == 0) DatabaseAccessor.removeTarget(this);
        else if (offset == 1) {
            Target removedTarget = persistedList.get(offsetIndex);
            DatabaseAccessor.removeTarget(this, removedTarget);
            DatabaseAccessor.addTarget(this, targets);
        }
        else DatabaseAccessor.addTarget(this, targets);
    }

    /**
     * Handles action UpdateRecord on the service worker thread.
     */
    private void handleActionUpdateRecord(Record... records) {
        DatabaseAccessor.addRecord(this, records);
    }

    /**
     * Handles action UpdateUser on the service worker thread.
     */
    private void handleActionUpdateUser(User... user) {
        DatabaseAccessor.addUser(this, user);
    }

    /**
     * Handles action ResetData on the service worker thread.
     */
    private void handleActionResetData() {

        DatabaseAccessor.removeSpawn(this);
        DatabaseAccessor.removeTarget(this);
        DatabaseAccessor.removeRecord(this);
        DatabaseAccessor.removeUser(this);
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
    }
}