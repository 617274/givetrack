package art.coded.givetrack.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

// import com.android.billingclient.api.BillingClient;
import art.coded.givetrack.AppUtilities;
import art.coded.givetrack.R;
import art.coded.givetrack.data.DatabaseContract;
import art.coded.givetrack.data.DatabaseManager;
import art.coded.givetrack.data.entry.User;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static art.coded.givetrack.data.DatabaseContract.LOADER_ID_USER;

/**
 * Provides a RewardedAd by which to augment balance toward donations.
 */
public class RewardActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        RewardedVideoAdListener {

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance();
    private static final float TEST_REWARD_MULTIPLIER = 5f;
    private RewardedVideoAd mRewardedAd;
    private User mUser;
    private int mRewardedAmount;
    @BindView(R.id.reward_toolbar) Toolbar mToolbar;
    @BindView(R.id.ad_button_wrapper) View mCreditButtonWrapper;
    @BindView(R.id.ad_progress) View mProgressBar;
    @BindView(R.id.credit_toggle_container) View mToggleContainer;
    @BindView(R.id.ad_button) FloatingActionButton mCreditButton;
    @BindView(R.id.reward_balance_text) TextView mRewardedView;
    @BindView(R.id.credit_toggle) View mCreditToggle;

    /**
     * Initializes the {@link RewardedVideoAd}.
     */
     // and should initialize {@link BillingClient}.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportLoaderManager().initLoader(DatabaseContract.LOADER_ID_USER, null, this);
    }

    private void initializeAds() {

        MobileAds.initialize(this, getString(R.string.am_app_id));

        // Create the RewardedAd and set the adUnitId (defined in values/strings.xml).
        mRewardedAd = MobileAds.getRewardedVideoAdInstance(RewardActivity.this);
        mRewardedAd.setRewardedVideoAdListener(RewardActivity.this);

        mRewardedAmount = mUser.getUserCredit();

        if (mRewardedAmount == 0) launchDialog(R.string.dialog_balance_preview);

        mRewardedView.setText(String.valueOf(mRewardedAmount + mUser.getIndexCount()));
        mRewardedAd.resume(this);
    }

    private void loadReward() {
        updateRewardButton(true);
        AdRequest request = new AdRequest.Builder().build();
        Timber.e(String.valueOf(request.isTestDevice(this)));
        mRewardedAd.loadAd(getString(R.string.am_rewarded_id), request);

        mProgressBar.setVisibility(View.VISIBLE);
        mToggleContainer.setPadding(0, (int) getResources().getDimension(R.dimen.toggle_padding), 0, 0);
    }

    private void launchDialog(int stringRes) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setMessage(getString(stringRes));
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_start),
                (onClickDialog, onClickPosition) -> dialog.dismiss());
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorConversionDark));
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorNeutralDark));
    }

    /**
     * Resumes the RewardedAd.
     */
    @Override
    protected void onResume() {
        if (mUser != null) mRewardedAd.resume(this);
        super.onResume();
    }

    /**
     * Pauses the RewardedAd.
     */
    @Override
    protected void onPause() {
        if (mUser != null) mRewardedAd.pause(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        startActivity(new Intent(this, HomeActivity.class));
    }

    @OnClick(R.id.ad_button) void clickButton() { if (mRewardedAd != null) loadReward(); }

    @OnClick(R.id.credit_toggle) void clickToggle() { if (mRewardedAd != null) loadReward(); }

    /**
     * Defines the data to be returned from {@link LoaderManager.LoaderCallbacks}.
     */
    @NonNull
    @Override public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case LOADER_ID_USER: return new CursorLoader(this, DatabaseContract.UserEntry.CONTENT_URI_USER, null, DatabaseContract.UserEntry.COLUMN_USER_ACTIVE + " = ? ", new String[] { "1" }, null);
            default: throw new RuntimeException(this.getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Replaces old data that is to be subsequently released from the {@link Loader}.
     */
    @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            do {
                User user = User.getDefault();
                AppUtilities.cursorRowToEntry(data, user);
                if (user.getUserActive()) {
                    mUser = user;
                    initializeAds();
                    break;
                }
            } while (data.moveToNext());
        }
    }

    /**
     * Tells the application to remove any stored references to the {@link android.content.Loader} data.
     */
    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) {  }

    /**
     * Syncs the updated balance toward donations on completion of RewardedAd.
     */
    @Override public void onRewarded(RewardItem rewardItem) {

        int reward = (int) (rewardItem.getAmount() * TEST_REWARD_MULTIPLIER);

        mRewardedAmount += reward; // Converts points to cash amount
        mRewardedView.setText(String.valueOf(mRewardedAmount));
        mRewardedView.setContentDescription(getString(R.string.description_reward_text, CURRENCY_FORMATTER.format(mRewardedAmount)));

        mUser.setUserCredit(mRewardedAmount);
        DatabaseManager.startActionUpdateUser(this, mUser);
    }

    @Override protected void onDestroy() {
        if (mRewardedAd != null) mRewardedAd.destroy(this);
        super.onDestroy();
    }
    @Override public void onRewardedVideoAdLoaded() { mRewardedAd.show(); }
    @Override public void onRewardedVideoAdOpened() {}
    @Override public void onRewardedVideoStarted() {
        mProgressBar.setVisibility(View.GONE);
        mToggleContainer.setPadding(0,0,0,0);
    }
    @Override public void onRewardedVideoAdClosed() { updateRewardButton(false); }
    @Override public void onRewardedVideoAdLeftApplication() { }
    @Override public void onRewardedVideoAdFailedToLoad(int i) {
        updateRewardButton(false);
        mRewardedAmount += 1; // Converts points to cash amount
        mRewardedView.setText(String.valueOf(mRewardedAmount));
        mRewardedView.setContentDescription(getString(R.string.description_reward_text, CURRENCY_FORMATTER.format(mRewardedAmount)));

        mUser.setUserCredit(mRewardedAmount);
        DatabaseManager.startActionUpdateUser(this, mUser);
        launchDialog(R.string.error_reward);

    }
    @Override public void onRewardedVideoCompleted() {
        recreate();
    }

    /**
     * Generates an options Menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reward, menu);
        return true;
    }

    /**
     * Defines behavior onClick of each MenuItem.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            case R.id.action_settings:
                launchDialog(R.string.dialog_balance_preview);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates reward Button based on RewardedAd status.
     */
    public void updateRewardButton(boolean convert) {

        int wrapperColorResId = convert ? R.color.colorConversionDark : R.color.colorCheerDark;
        mCreditButtonWrapper.setBackgroundColor(getResources().getColor(wrapperColorResId));

        int buttonColorResId = convert ? R.color.colorConversion : R.color.colorCheer;
        mCreditButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(buttonColorResId)));
        mCreditButton.setEnabled(!convert);
    }
}