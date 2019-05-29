package com.github.rjbx.givetrack.view;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.github.rjbx.givetrack.AppUtilities;
import com.github.rjbx.givetrack.R;
import com.github.rjbx.givetrack.data.DatabaseContract;
import com.github.rjbx.givetrack.data.DatabaseManager;
import com.github.rjbx.givetrack.data.entry.User;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static com.github.rjbx.givetrack.data.DatabaseContract.LOADER_ID_USER;

// TODO: Configure to award additional uses of features beyond daily limit e.g. search for each ad view
/**
 * Provides a RewardedAd by which to augment balance toward donations.
 */
public class RewardActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        RewardedVideoAdListener,
        PurchasesUpdatedListener {

    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance();
    private static final float MULTIPLIER_REWARD_ESTIMATE = 0.01f;
    private RewardedVideoAd mRewardedAd;
    private BillingClient mBillingClient;
    private View mCreditButtonWrapper;
    private FloatingActionButton mCreditButton;
    private View mProgressBar;
    private View mToggleContainer;
    private TextView mRewardedView;
    private TextView mPaidView;
    private TextView mWalletView;
    private User mUser;
    private float mPurchaseAmount;
    private float mPaidAmount;
    private double mRewardedAmount;
    private double mWalletAmount;
    private int mUserGender;
    private boolean mShowAd = true;
    private static boolean sDialogShown;

    /**
     * Initializes the {@link RewardedVideoAd} and {@link BillingClient}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        Toolbar toolbar = findViewById(R.id.reward_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        mProgressBar = findViewById(R.id.ad_progress);
        mToggleContainer = findViewById(R.id.credit_toggle_container);

        if (!sDialogShown && mRewardedAmount == 0) {
            AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
            dialog.setMessage(getString(R.string.dialog_balance_preview));
            dialog.setButton(android.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_start),
                    (onClickDialog, onClickPosition) -> sDialogShown = true);
            dialog.setButton(android.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_later),
                    (onClickDialog, onClickPosition) -> dialog.dismiss());
            dialog.show();
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorConversionDark));
            dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorNeutralDark));
        }

    }

    private void initializeAds() {

        MobileAds.initialize(this, getString(R.string.am_app_id));

        String genderStr = mUser.getUserGender();
        mUserGender = genderStr != null ? Integer.parseInt(genderStr) : 0;

        String birthdate = mUser.getUserBirthdate();
        String[] birthdateParams = birthdate.split("/");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Integer.parseInt(birthdateParams[0]), Integer.parseInt(birthdateParams[1]), Integer.parseInt(birthdateParams[2]));

        final Date rewardedBirthday = calendar.getTime();

        // Create the RewardedAd and set the adUnitId (defined in values/strings.xml).
        mRewardedAd = MobileAds.getRewardedVideoAdInstance(RewardActivity.this);
        mRewardedAd.setRewardedVideoAdListener(RewardActivity.this);

        ImageButton toggle = findViewById(R.id.credit_toggle);
        View rewardFrame = findViewById(R.id.reward_balance_frame);
        View paymentFrame = findViewById(R.id.payment_balance_frame);

        toggle.setOnClickListener(clickedView -> {
            mShowAd = !mShowAd;
            toggle.setImageResource(mShowAd ? R.drawable.baseline_card_giftcard_24 : R.drawable.baseline_credit_card_24);
            if (mShowAd) {
                String response = getString(R.string.reward_response);
                toggle.setContentDescription(getString(R.string.description_credit_button, response));
                mCreditButton.setContentDescription(getString(R.string.description_credit_toggle_button, response));
                rewardFrame.setBackgroundColor(getResources().getColor(R.color.colorAttention));
                paymentFrame.setBackgroundColor(getResources().getColor(R.color.colorAttentionDark));
                mRewardedView.setBackgroundColor(getResources().getColor(R.color.colorSlate));
                mPaidView.setBackgroundColor(Color.BLACK);
            } else {
                String response = getString(R.string.payment_response);
                toggle.setContentDescription(getString(R.string.description_credit_toggle_button, response));
                mCreditButton.setContentDescription(getString(R.string.description_credit_button, response));
                rewardFrame.setBackgroundColor(getResources().getColor(R.color.colorAttentionDark));
                paymentFrame.setBackgroundColor(getResources().getColor(R.color.colorAttention));
                mRewardedView.setBackgroundColor(Color.BLACK);
                mPaidView.setBackgroundColor(getResources().getColor(R.color.colorSlate));
            }
        });

        // Create the next level button, which tries to show an rewarded when clicked.
        mCreditButtonWrapper = findViewById(R.id.ad_button_wrapper);
        mCreditButton = findViewById(R.id.ad_button);
        mCreditButton.setOnClickListener(clickedView -> {
            if (mShowAd) {
                updateRewardButton(true);
                mRewardedAd.loadAd(getString(R.string.am_ad_id), new AdRequest.Builder()
                        .setGender(mUserGender)
                        .setBirthday(rewardedBirthday)
                        .build());
                mProgressBar.setVisibility(View.VISIBLE);
                mToggleContainer.setPadding(0, (int) getResources().getDimension(R.dimen.toggle_padding), 0, 0);
            } else {
                String defaultProducttId = "android.test.purchased";
                List<String> skus = new ArrayList<>();
                skus.add(defaultProducttId);
                SkuDetailsParams skuParams = SkuDetailsParams.newBuilder()
                        .setSkusList(skus).setType(BillingClient.SkuType.INAPP).build();

                mBillingClient.querySkuDetailsAsync(skuParams, (skuDetailsResponseCode, skuDetailsList) -> {
                    if (skuDetailsResponseCode == BillingClient.BillingResponse.OK && skuDetailsList != null) {
                        for (SkuDetails skuDetails : skuDetailsList) {
                            if (skuDetails.getSku().equals(defaultProducttId)) {
                                BillingFlowParams billingParams = BillingFlowParams.newBuilder()
                                        .setSku(BillingClient.SkuType.INAPP).build();
                                mBillingClient.launchBillingFlow(RewardActivity.this, billingParams);
                                mPurchaseAmount = skuDetails.getPriceAmountMicros();
                            }
                        }
                    }
                });
            }
        });

        mRewardedAmount = mUser.getGiveImpact();
        mPaidAmount = /*mUser.getPaid(this)*/ 0;

        mPaidView = findViewById(R.id.payment_balance_text);
        mPaidView.setText(CURRENCY_FORMATTER.format(mPaidAmount));
        mPaidView.setContentDescription(getString(R.string.description_payment_text, CURRENCY_FORMATTER.format(mPaidAmount)));

        mRewardedView = findViewById(R.id.reward_balance_text);
        mRewardedView.setText(CURRENCY_FORMATTER.format(mRewardedAmount));
        mRewardedView.setContentDescription(getString(R.string.description_reward_text, CURRENCY_FORMATTER.format(mRewardedAmount)));

        mWalletAmount = mRewardedAmount + mPaidAmount;
        mWalletView = findViewById(R.id.wallet_balance_text);
        mWalletView.setText(CURRENCY_FORMATTER.format(mWalletAmount));
        mWalletView.setContentDescription(getString(R.string.description_wallet_text, CURRENCY_FORMATTER.format(mWalletAmount)));

        mBillingClient = BillingClient.newBuilder(this).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(int responseCode) {
                if (responseCode == BillingClient.BillingResponse.OK)
                    Timber.v(getString(R.string.message_billing_client));
            }

            @Override public void onBillingServiceDisconnected() {}
        });

        mRewardedAd.resume(this);
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

    /**
     * Defines the data to be returned from {@link androidx.loader.app.LoaderManager.LoaderCallbacks}.
     */
    @NonNull
    @Override public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        switch (id) {
            case LOADER_ID_USER: return new CursorLoader(this, DatabaseContract.UserEntry.CONTENT_URI_USER, null, DatabaseContract.UserEntry.COLUMN_USER_ACTIVE + " = ? ", new String[] { "1" }, null);
            default: throw new RuntimeException(this.getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Replaces old data that is to be subsequently released from the {@link androidx.loader.content.Loader}.
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

        float reward = rewardItem.getAmount() * MULTIPLIER_REWARD_ESTIMATE;

        mRewardedAmount += reward; // Converts points to cash amount
        mRewardedView.setText(CURRENCY_FORMATTER.format(mRewardedAmount));
        mRewardedView.setContentDescription(getString(R.string.description_reward_text, CURRENCY_FORMATTER.format(mRewardedAmount)));

        mWalletAmount += reward;
        mWalletView.setText(CURRENCY_FORMATTER.format(mWalletAmount));
        mWalletView.setContentDescription(getString(R.string.description_wallet_text, CURRENCY_FORMATTER.format(mWalletAmount)));

        mUser.setGiveImpact(mRewardedAmount);
        DatabaseManager.startActionUpdateUser(this, mUser);
    }

    @Override protected void onDestroy() {
        if (mRewardedAd != null) mRewardedAd.destroy(this);
        super.onDestroy();
    }
    @Override public void onRewardedVideoAdLoaded() {
        mRewardedAd.show();
    }
    @Override public void onRewardedVideoAdOpened() {}
    @Override public void onRewardedVideoStarted() {
        mProgressBar.setVisibility(View.GONE);
        mToggleContainer.setPadding(0,0,0,0);
    }
    @Override public void onRewardedVideoAdClosed() { updateRewardButton(false); }
    @Override public void onRewardedVideoAdLeftApplication() {

    }
    @Override public void onRewardedVideoAdFailedToLoad(int i) {
        updateRewardButton(false);
    }
    @Override public void onRewardedVideoCompleted() {

    }

    /**
     * Syncs the updated balance toward donations on completion of purchase.
     */
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
        if (responseCode ==  BillingClient.BillingResponse.OK) {
            if (purchases != null)
                for (Purchase purchase : purchases)
                    if (purchase.getSku().equals("android.test.purchased")) {

                        mPaidAmount += mPurchaseAmount;
                        mPaidView.setText(CURRENCY_FORMATTER.format(mPaidAmount));
                        mPaidView.setContentDescription(getString(R.string.description_payment_text, CURRENCY_FORMATTER.format(mPaidAmount)));

                        mWalletAmount += mPurchaseAmount;
                        mWalletView.setText(CURRENCY_FORMATTER.format(mWalletAmount));
                        mWalletView.setContentDescription(getString(R.string.description_wallet_text, CURRENCY_FORMATTER.format(mWalletAmount)));

//                        mUser.setPaid(this, mPaidAmount);
                        DatabaseManager.startActionUpdateUser(this, mUser);
                    }
        }
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
        if (id == R.id.action_settings) return true;
        return super.onOptionsItemSelected(item);
    }

    /**
     * Generates reward Button based on RewardedAd status.
     */
    public void updateRewardButton(boolean convert) {

        int wrapperColorResId = convert ? R.color.colorConversionDark : R.color.colorCheerDark;
        mCreditButtonWrapper.setBackgroundColor(getResources().getColor(wrapperColorResId));

        int buttonColorResId = convert ? R.color.colorConversion : R.color.colorAccent;
        mCreditButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(buttonColorResId)));
        mCreditButton.setEnabled(!convert);
    }
}
