package com.github.rjbx.givetrack.view;

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

import com.github.rjbx.givetrack.AppUtilities;
import com.github.rjbx.givetrack.BuildConfig;
import com.github.rjbx.givetrack.R;
import com.github.rjbx.givetrack.data.DatabaseContract;
import com.github.rjbx.givetrack.data.DatabaseManager;
import com.github.rjbx.givetrack.data.entry.User;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_MAIN;
import static com.github.rjbx.givetrack.data.DatabaseContract.LOADER_ID_USER;

/**
 * Provides a UI for and manages user authentication interfacing with {@link FirebaseAuth}.
 */
public class AuthActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        DialogInterface.OnClickListener {

    private static final int REQUEST_SIGN_IN = 0;

    private static final String USERS_STATE = "com.github.rjbx.givetrack.ui.state.AUTH_USERS";

    public static final String ACTION_SIGN_IN = "com.github.rjbx.givetrack.ui.action.SIGN_IN";
    public static final String ACTION_SIGN_OUT = "com.github.rjbx.givetrack.ui.action.SIGN_OUT";
    public static final String ACTION_DELETE_ACCOUNT = "com.github.rjbx.givetrack.ui.action.DELETE_ACCOUNT";

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

        if (savedInstanceState != null)
            mUsers = savedInstanceState.getParcelableArrayList(USERS_STATE);
        getSupportLoaderManager().initLoader(DatabaseContract.LOADER_ID_USER, null, this);
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
                boolean isPersisted = false;
                mActiveUser = AppUtilities.convertRemoteToLocalUser(mFirebaseAuth.getCurrentUser());
                AppUtilities.mapToSharedPreferences(mActiveUser.toParameterMap(), PreferenceManager.getDefaultSharedPreferences(this));
                mUsers.add(mActiveUser);
                DatabaseManager.startActionUpdateUser(this, mUsers.toArray(new User[0]));
                mProcessStage++;
            } else {
                IdpResponse response = IdpResponse.fromResultIntent(data);
                mProgressbar.setVisibility(View.VISIBLE);
                String message;
                if (response == null) message = getString(R.string.network_error_message);
                else
                    message = getString(R.string.provider_error_message, response.getProviderType());
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
                return new CursorLoader(this, DatabaseContract.UserEntry.CONTENT_URI_USER, null, null, null, null);
            default:
                throw new RuntimeException(this.getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Replaces old data that is to be subsequently released from the {@link Loader}.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (loader.getId() != DatabaseContract.LOADER_ID_USER) return;
        mUsers = AppUtilities.getEntryListFromCursor(data, User.class);
        switch (mProcessStage) {
            case 0:
                handleAction(getIntent().getAction());
                break;
            case 2:
                DatabaseManager.startActionFetchUser(this);
                mProcessStage++;
                break;
            case 3:
                mProcessStage++;
            case 4:
                mProcessStage++;
                startActivity(new Intent(AuthActivity.this, HomeActivity.class).setAction(ACTION_SIGN_IN));
                finish();
        }
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
            String password = ((EditText) mDialogView.findViewById(R.id.password)).getText().toString();
            switch (which) {
                case AlertDialog.BUTTON_NEGATIVE: dialog.dismiss(); break;
                case AlertDialog.BUTTON_POSITIVE:
                    if (mAction.equals(ACTION_DELETE_ACCOUNT)) {
                        DatabaseManager.startActionResetData(AuthActivity.this);
                        AppUtilities.completeTaskOnReauthentication(email, password, signedOutTask -> {
                            FirebaseUser refreshedUser = mFirebaseAuth.getCurrentUser();
                            if (refreshedUser != null) refreshedUser.delete()
                                .addOnSuccessListener(deleteTask -> {
                                    mReauthAttempts = 0;
                                    DatabaseManager.startActionRemoveUser(this, mActiveUser);
                                    AuthUI.getInstance().signOut(this);
                                    finish();
                                    startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                                    Toast.makeText(AuthActivity.this, getString(R.string.message_data_erase), Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(failTask -> {
                                    if (mReauthAttempts < 5) {
                                        launchAuthDialog();
                                        Toast.makeText(this, "Your credentials could not be validated.\nTry again.", Toast.LENGTH_LONG).show();
                                    } else {
                                        mReauthAttempts = 0;
                                        Toast.makeText(this, "Your credentials could not be validated.\n\nEnsure that you have a valid connection to the Internet and that your password is correct,\n\nIf so, the server may not be responding at the moment; please try again later.", Toast.LENGTH_LONG).show();
                                    }
                                });
                        });
                    }
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
            case ACTION_MAIN:
                if (firebaseUser == null) {
                    for (User u : mUsers) u.setUserActive(false);
                    DatabaseManager.startActionUpdateUser(this, mUsers.toArray(new User[0]));
                    List<AuthUI.IdpConfig> providers = new ArrayList<>();
                    providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
                    providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
                    providers.add(new AuthUI.IdpConfig.AnonymousBuilder().build());
                    Intent signIn = AuthUI.getInstance().createSignInIntentBuilder()
                            .setLogo(R.mipmap.ic_launcher_round)
                            .setTosAndPrivacyPolicyUrls("https://github.com/rjbx/Givetrack", "https://github.com/rjbx/Givetrack")
                            .setTheme(R.style.AppTheme_AuthOverlay)
                            .setIsSmartLockEnabled(false, true)
                            .setAvailableProviders(providers)
                            .build();
                    startActivityForResult(signIn, REQUEST_SIGN_IN);
                    mProcessStage++;
                } else {
                    finish();
                    Toast.makeText(this, getString(R.string.message_login), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class).setAction(AuthActivity.ACTION_SIGN_IN));
                }
                break;
            case ACTION_SIGN_OUT:
                if (firebaseUser == null) return;
                for (User u : mUsers) if (u.getUid().equals(firebaseUser.getUid())) mActiveUser = u;
                mActiveUser.setUserActive(false);
                DatabaseManager.startActionUpdateUser(this, mActiveUser);
                AuthUI.getInstance().signOut(this);
                finish();
                startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                Toast.makeText(AuthActivity.this, getString(R.string.message_data_erase), Toast.LENGTH_LONG).show();
                break;
            case ACTION_DELETE_ACCOUNT:
                if (firebaseUser == null) return;
                for (User u : mUsers) if (u.getUid().equals(firebaseUser.getUid())) mActiveUser = u;
                firebaseUser.delete()
                    .addOnSuccessListener(deleteTask -> {
                        mReauthAttempts = 0;
                        DatabaseManager.startActionRemoveUser(this, mActiveUser);
                        AuthUI.getInstance().signOut(this);
                        finish();
                        startActivity(new Intent(AuthActivity.this, AuthActivity.class).setAction(ACTION_MAIN));
                        Toast.makeText(AuthActivity.this, getString(R.string.message_data_erase), Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(failTask -> {
                        Toast.makeText(this, "Enter your credentials.", Toast.LENGTH_SHORT).show();
                        launchAuthDialog();
                    });
                break;
        }
    }

    private void launchAuthDialog() {
        mReauthAttempts++;
        mDialogView = getLayoutInflater().inflate(R.layout.dialog_reauth, null);
        mAuthDialog = new AlertDialog.Builder(this).create();
        mAuthDialog.setView(mDialogView);
        mAuthDialog.setMessage(getString(R.string.message_update_email));
        mAuthDialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_option_keep), this);
        mAuthDialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_option_change), this);
        mAuthDialog.show();
        mAuthDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorNeutralDark, null));
        mAuthDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorConversionDark, null));
    }
}