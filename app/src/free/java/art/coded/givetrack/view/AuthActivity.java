package art.coded.givetrack.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;

import art.coded.givetrack.AppUtilities;
import art.coded.givetrack.AppWidget;
import art.coded.givetrack.BuildConfig;
import art.coded.givetrack.R;
import art.coded.givetrack.data.DatabaseContract;
import art.coded.givetrack.data.DatabaseManager;
import art.coded.givetrack.data.entry.User;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;
import static art.coded.givetrack.data.DatabaseContract.LOADER_ID_USER;

/**
 * Provides a UI for and manages user authentication interfacing with {@link FirebaseAuth}.
 */
public class AuthActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        DialogInterface.OnClickListener {

    private static final int REQUEST_SIGN_IN = 0;

    private static final String USERS_STATE = "art.coded.givetrack.ui.state.AUTH_USERS";

    public static final String ACTION_SIGN_IN = "art.coded.givetrack.ui.action.SIGN_IN";
    public static final String ACTION_SIGN_OUT = "art.coded.givetrack.ui.action.SIGN_OUT";
    public static final String ACTION_DELETE_ACCOUNT = "art.coded.givetrack.ui.action.DELETE_ACCOUNT";

    private int mProcessStage = 0;
    private int mReauthAttempts;
    private List<User> mUsers;
    private User mActiveUser;
    private FirebaseAuth mFirebaseAuth;
    private AlertDialog mAuthDialog;
    private View mDialogView;
    private String mAction;
    @BindView(R.id.auth_progress) ProgressBar mProgressbar;

    /**
     * Handles sign in, sign out, and account deletion launch Intent actions.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        ButterKnife.bind(this);
        if (BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());

        mFirebaseAuth = FirebaseAuth.getInstance();

        if (savedInstanceState != null) {
            mUsers = savedInstanceState.getParcelableArrayList(USERS_STATE);
            savedInstanceState.clear();
        }
        if (mFirebaseAuth.getCurrentUser() != null) getSupportLoaderManager().initLoader(LOADER_ID_USER, null, this);
        else handleAction(getIntent().getAction());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(USERS_STATE, (ArrayList<User>) mUsers);
        super.onSaveInstanceState(outState);
    }

    /**
     * Hides {@link ProgressBar} when launching AuthUI
     * and unregisters this Activity from listening to Preference changes
     * in order to prevent relaunching HomeActivity.
     */
    @Override
    protected void onStop() {
        mProgressbar.setVisibility(View.GONE);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) getSupportLoaderManager().destroyLoader(DatabaseContract.LOADER_ID_USER);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /**
     * Defines behavior on user submission of login credentials.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN) {
            // If FirebaseAuth signin successful; FirebaseUser with UID available (irrespective of FirebaseDatabase content)
            if (resultCode == RESULT_OK) {
                FirebaseUser user = mFirebaseAuth.getCurrentUser();
                if (user == null) return;
                getSupportLoaderManager().initLoader(LOADER_ID_USER, null, this);
            } else {
                // Block sign-in to prevent overwriting existing remote with default data
                IdpResponse response = IdpResponse.fromResultIntent(data);
                mProgressbar.setVisibility(View.VISIBLE);
                String message;
                if (response == null) message = getString(R.string.network_error_message);
                else message = getString(R.string.provider_error_message, response.getProviderType());
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Defines the data to be returned from {@link LoaderManager.LoaderCallbacks}.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case LOADER_ID_USER:
                return new CursorLoader(this, DatabaseContract.UserEntry.CONTENT_URI_USER, null, DatabaseContract.UserEntry.COLUMN_UID + " = ?", new String[]{mFirebaseAuth.getCurrentUser().getUid()}, null);
            default: throw new RuntimeException(this.getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Replaces old data that is to be subsequently released from the {@link Loader}.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() != DatabaseContract.LOADER_ID_USER) return;
        if (mProcessStage == -1) {
            if (mActiveUser != null) {
                FirebaseUser user = mFirebaseAuth.getCurrentUser();
                if (user == null) return;
                if (mAction.equals(ACTION_DELETE_ACCOUNT)) {
                    FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                    if (firebaseUser == null) return;
                    List<String> providers = new ArrayList<>();
                    for (UserInfo uInfo : firebaseUser.getProviderData()) providers.add(uInfo.getProviderId());
                    if (providers.contains("password")) {
                        Toast.makeText(this, "Enter your credentials.", Toast.LENGTH_SHORT).show();
                        launchAuthDialog();
                    } else if (providers.contains("google.com")) {
                        AuthCredential credential = null;
                        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(AuthActivity.this);
                        if (account != null) {
                            String token = account.getIdToken();
                            credential = GoogleAuthProvider.getCredential(/*id, token*/token, null);
                        }
                        if (credential == null) return;
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnSuccessListener(signedOutTask -> {
                                mAction = ACTION_SIGN_IN;
                                DatabaseManager.startActionRemoveUser(this, mActiveUser);
                                Toast.makeText(this, "Your app data has been erased.", Toast.LENGTH_SHORT).show();
                                mActiveUser = null;
                            })
                            .addOnFailureListener(signedOutTask -> {
                                Toast.makeText(this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                                if (mReauthAttempts < 5) launchAuthDialog();
                                else mReauthAttempts = 0;
                            });
                    } else {
                        mAction = ACTION_SIGN_IN;
                        DatabaseManager.startActionRemoveUser(this, mActiveUser);
                        Toast.makeText(this, "Your app data has been erased.", Toast.LENGTH_SHORT).show();
                        mActiveUser = null;
                    }
                } else if (mAction.equals(ACTION_SIGN_OUT)) {
                    if (!mActiveUser.getUid().equals(user.getUid())) return;
                    mAction = ACTION_SIGN_IN;
                    mFirebaseAuth.signOut();
                    mUsers = null;
                    mActiveUser = null;
                    mProcessStage = 0;
                    finish();
                    startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                    ViewUtilities.centerToastMessage(Toast.makeText(AuthActivity.this, getString(R.string.message_logout), Toast.LENGTH_LONG)).show();
                }
            } else {
                FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (refreshedUser != null) refreshedUser.delete()
                        .addOnSuccessListener(retryDeleteTask -> {
                            mProcessStage = 0;
                            mReauthAttempts = 0;
                            finish();
                            startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                            Toast.makeText(AuthActivity.this, getString(R.string.message_data_erase), Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(retryFailTask -> {
                            Timber.e(retryFailTask);
                            Toast.makeText(AuthActivity.this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                        });
            }
            AppWidget.refresh(this);
        } else {
            mUsers = AppUtilities.getEntryListFromCursor(data, User.class);
            FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
            switch (mProcessStage) {
                case 0:
                    handleAction(getIntent().getAction());
                    break;
                case 1:
                    FirebaseUser user = mFirebaseAuth.getCurrentUser();
                    if (user == null) return;
                    mProcessStage++;
                    mActiveUser = AppUtilities.convertRemoteToLocalUser(user);
                    DatabaseManager.startActionFetchUser(this);
                    break;
                case 2:
                    // Reached from validation guaranteed callback
                    boolean isPersisted = false;
                    for (int i = 0; i < mUsers.size(); i++) {
                        isPersisted = mUsers.get(i).getUid().equals(mActiveUser.getUid());

                        if (isPersisted) {
                            mActiveUser = mUsers.get(i);
                            ViewUtilities.centerToastMessage(Toast.makeText(this, getGreeting(firebaseUser), Toast.LENGTH_SHORT)).show();
                            finish();
                            startActivity(new Intent(AuthActivity.this, HomeActivity.class).setAction(ACTION_SIGN_IN));
                            break;
                        }
                    }
                    if (!isPersisted) {
                        mUsers.add(mActiveUser);
                        DatabaseManager.startActionUpdateUser(this, mUsers.toArray(new User[0]));
                        mProcessStage++;
                    }
                    AppUtilities.mapToSharedPreferences(mActiveUser.toParameterMap(), PreferenceManager.getDefaultSharedPreferences(this));
                    break;
                case 3:
                    finish();
                    startActivity(new Intent(AuthActivity.this, HomeActivity.class).setAction(ACTION_SIGN_IN));
                    ViewUtilities.centerToastMessage(Toast.makeText(this, getGreeting(firebaseUser), Toast.LENGTH_SHORT)).show();
            }
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    /**
     * Tells the application to remove any stored references to the {@link Loader} data.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mUsers = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mAuthDialog) {
            String email = ((EditText) mDialogView.findViewById(R.id.reauth_user)).getText().toString();
            String password = ((EditText) mDialogView.findViewById(R.id.reauth_password)).getText().toString();
            switch (which) {
                case AlertDialog.BUTTON_NEUTRAL:
                    mReauthAttempts = 0;
                    dialog.dismiss();
                    finish();
                    startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    if (email.isEmpty() || password.isEmpty() || !email.equals(mActiveUser.getUserEmail())) {
                        launchAuthDialog();
                        Toast.makeText(this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    AuthCredential credential = EmailAuthProvider.getCredential(email, password);
                    FirebaseUser retryUser = mFirebaseAuth.getCurrentUser();
                    if (retryUser == null) return;
                    retryUser.reauthenticate(credential)
                        .addOnSuccessListener(signedOutTask -> {
                            mAction = ACTION_SIGN_IN;
                            DatabaseManager.startActionRemoveUser(this, mActiveUser);
                            Toast.makeText(this, "Your app data has been erased.", Toast.LENGTH_SHORT).show();
                            mActiveUser = null;
                            FirebaseUser refreshedUser = mFirebaseAuth.getCurrentUser();
                            if (refreshedUser != null) refreshedUser.delete()
                                    .addOnSuccessListener(deleteTask -> {
                                        mReauthAttempts = 0;
                                        finish();
                                        startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                                        Toast.makeText(AuthActivity.this, getString(R.string.message_data_erase), Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(failTask -> {
                                        Toast.makeText(this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                                        if (mReauthAttempts < 5) launchAuthDialog();
                                        else {
                                            mReauthAttempts = 0;
                                            startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                                        }
                                    });
                        })
                        .addOnFailureListener(signedOutTask -> {
                            Toast.makeText(this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                            if (mReauthAttempts < 5) launchAuthDialog();
                            else {
                                mReauthAttempts = 0;
                                startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                            }
                        });
                    break;
                default:
            }
        }
    }

    /**
     * Processes actions defined by the source Intent.
     */
    private void handleAction(String action) {
        if (action == null) return;
        mAction = action;
        FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
        switch (action) {
            case ACTION_SIGN_OUT:
                if (firebaseUser == null) return;
                for (User u : mUsers) if (u.getUid().equals(firebaseUser.getUid())) mActiveUser = u;
                mActiveUser.setUserActive(false);
                DatabaseManager.startActionUpdateUser(this, mActiveUser);
    //                DatabaseManager.startActionFetchTarget(this);
    //                DatabaseManager.startActionFetchRecord(this);
                mProcessStage = -1;
                break;
            case ACTION_DELETE_ACCOUNT:
                if (firebaseUser == null) return;
                for (User u : mUsers) if (u.getUid().equals(firebaseUser.getUid())) {
                    mActiveUser = u;
                    DatabaseManager.startActionUpdateUser(this, u);
                }
                mProcessStage = -1;
                break;
            default:
                if (firebaseUser == null) {
                    List<AuthUI.IdpConfig> providers = new ArrayList<>();
                    providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
                    providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
                    providers.add(new AuthUI.IdpConfig.AnonymousBuilder().build());
                    Intent signIn = AuthUI.getInstance().createSignInIntentBuilder()
                            .setLogo(R.drawable.logo)
                            .setTosAndPrivacyPolicyUrls("https://coded.art/givetrack/terms", "https://coded.art/givetrack/privacy")
                            .setTheme(R.style.AppTheme_AuthOverlay)
                            .setIsSmartLockEnabled(false, true)
                            .setAvailableProviders(providers)
                            .build();
                    startActivityForResult(signIn, REQUEST_SIGN_IN);
                    mProcessStage++;
                } else {
                    finish();
                    ViewUtilities.centerToastMessage(Toast.makeText(this, getGreeting(firebaseUser), Toast.LENGTH_SHORT)).show();
                    startActivity(new Intent(this, HomeActivity.class).setAction(AuthActivity.ACTION_SIGN_IN));
                }
        }
    }

    private void launchAuthDialog() {
        mReauthAttempts++;
        mDialogView = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        mAuthDialog = new AlertDialog.Builder(this).create();
        mAuthDialog.setView(mDialogView);
        mAuthDialog.setCanceledOnTouchOutside(false);
        mAuthDialog.setMessage(getString(R.string.message_update_email));
        mAuthDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_option_cancel), this);
        mAuthDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_option_confirm), this);
        mAuthDialog.show();
        mAuthDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorNeutralDark, null));
        mAuthDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorConversion, null));
        mAuthDialog.setOnCancelListener((dialog) -> {
            mReauthAttempts = 0;
            dialog.dismiss();
            finish();
            startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));

        });
    }

    private String getGreeting(FirebaseUser firebaseUser) {
        String userIdentifier = "";
        if (firebaseUser != null) userIdentifier = firebaseUser.getEmail();
        if (userIdentifier == null || userIdentifier.isEmpty()) userIdentifier = "guest";

        return getString(R.string.message_login, userIdentifier);
    }
}