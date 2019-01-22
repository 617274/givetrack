package com.github.rjbx.givetrack.ui;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.bumptech.glide.Glide;
import com.github.rjbx.givetrack.R;

import com.github.rjbx.givetrack.data.DatabaseContract;
import com.github.rjbx.givetrack.data.UserPreferences;
import com.github.rjbx.givetrack.data.DatabaseService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;

/**
 * Presents a list of API request generated items, which when touched, arrange the list of items and
 * item details side-by-side using two vertical panes.
 */
public class RecordActivity extends AppCompatActivity implements
        DetailFragment.MasterDetailFlow, LoaderManager.LoaderCallbacks<Cursor>,
        DialogInterface.OnClickListener {


    public static final String ACTION_RECORD_INTENT = "com.github.rjbx.givetrack.ui.action.RECORD_INTENT";
    private static final String STATE_PANE = "com.github.rjbx.givetrack.ui.state.RECORD_PANE";
    private static boolean sDialogShown;
    private static boolean sDualPane;
    private ListAdapter mAdapter;
    private AlertDialog mRecordDialog;
    private String mSnackbar;
    @BindView(R.id.record_fab) FloatingActionButton mFab;
    @BindView(R.id.record_toolbar) Toolbar mToolbar;
    @BindView(R.id.record_list) RecyclerView mRecyclerView;
    @BindView(R.id.record_list_container) View mListContainer;
    @BindView(R.id.record_item_container) View mItemContainer;

    /**
     * Instantiates a swipeable RecyclerView and FloatingActionButton.
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ButterKnife.bind(this);

        getSupportLoaderManager().initLoader(DatabaseContract.LOADER_ID_RECORD, null, this);
        if (savedInstanceState != null) {
            sDualPane = savedInstanceState.getBoolean(STATE_PANE);
        } else sDualPane = mItemContainer.getVisibility() == View.VISIBLE;

        Bundle bundle = getIntent().getExtras();
        if (sDualPane) showDualPane(bundle);

        setSupportActionBar(mToolbar);
        mToolbar.setTitle(getTitle());

        mRecordDialog = new AlertDialog.Builder(this).create();
        mRecordDialog.setMessage(getString(R.string.dialog_filter_setup));
        mRecordDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dialog_option_start), this);
        mRecordDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.dialog_option_later), this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        assert mRecyclerView != null;
        mAdapter = new ListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.ACTION_STATE_IDLE,
                ItemTouchHelper.LEFT) {

            @Override public boolean onMove(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Bundle bundle = (Bundle) viewHolder.itemView.getTag();
                final String ein =  bundle.getString(DetailFragment.ARG_ITEM_EIN);
                if (direction == ItemTouchHelper.LEFT) DatabaseService.startActionRemoveSearch(getBaseContext(), ein);
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    /**
     * Saves Layout configuration state.
     */
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PANE, sDualPane);
    }

    /**
     * Generates an options Menu.
     */
    @Override public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.record, menu);
        return true;
    }

/*    *//**
     * Defines behavior onClick of each MenuItem.
     *//*
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case (android.R.id.home):
                navigateUpTo(new Intent(this, MainActivity.class));
                return true;
            case (R.id.action_filter):
                launchFilterPreferences(this);
        }
        return super.onOptionsItemSelected(item);
    }*/

    /**
     * Instantiates and returns a new {@link Loader} for the given ID.
     */
    @NonNull @Override public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle bundle) {
        switch (id) {
            case DatabaseContract.LOADER_ID_RECORD:
                Uri ratingUri = DatabaseContract.Entry.CONTENT_URI_RECORD;
                return new CursorLoader(
                        this, ratingUri,
                        null, null, null, null);
            default:
                throw new RuntimeException(getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Replaces old data that is to be subsequently released from the {@link Loader}.
     */
    @Override public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || (!cursor.moveToFirst() && !sDialogShown)) {
            mRecordDialog.show();
            mRecordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorConversionDark));
            mRecordDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorNeutralDark));
        }
        int id = loader.getId();
        switch (id) {
            case DatabaseContract.LOADER_ID_RECORD:
                if (cursor == null|| !cursor.moveToFirst()) return;
                ContentValues[] valuesArray = new ContentValues[cursor.getCount()];
                int i = 0;
                do {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cursor, values);
                    valuesArray[i++] = values;
                } while (cursor.moveToNext());
                mAdapter.swapValues(valuesArray);
//                if (mSnackbar == null || mSnackbar.isEmpty()) mSnackbar = getString(R.string.message_record_refresh);
                Snackbar sb = Snackbar.make(mFab, mSnackbar, Snackbar.LENGTH_LONG);
                sb.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                sb.show();
                break;
            default:
                throw new RuntimeException(getString(R.string.loader_error_message, id));
        }
    }

    /**
     * Tells the application to remove any stored references to the {@link Loader} data.
     */
    @Override public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapValues(null);
    }

    /**
     * Indicates whether the MasterDetailFlow is in dual pane mode.
     */
    @Override public boolean isDualPane() { return sDualPane; }

    /**
     * Presents the list of items and item details side-by-side using two vertical panes.
     */
    @Override public void showDualPane(Bundle args) {
        if (args != null) RecordActivity.this.getSupportFragmentManager().beginTransaction()
                .replace(R.id.record_item_container, DetailFragment.newInstance(args))
                .commit();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (width * .5f), ViewGroup.LayoutParams.MATCH_PARENT);
        mListContainer.setLayoutParams(params);
        mItemContainer.setVisibility(View.VISIBLE);
        mItemContainer.setLayoutParams(params);
    }

    /**
     * Presents the list of items in a single vertical pane, hiding the item details.
     */
    @Override public void showSinglePane() {
        mListContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sDualPane = false;
    }

    /**
     * Defines behaviors on click of DialogInterface buttons.
     */
    @Override public void onClick(DialogInterface dialog, int which) {
        if (dialog == mRecordDialog) {
            switch (which) {
                case AlertDialog.BUTTON_NEUTRAL:
                    mRecordDialog.dismiss();
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    sDialogShown = true;
//                    launchFilterPreferences(this);
                    break;
                default:
            }
        }
    }

    /**
     * Populates {@link RecordActivity} {@link RecyclerView}.
     */
    @OnClick(R.id.record_fab) public void refreshResults() {
        Context context = RecordActivity.this;
        HashMap<String, String> hashMap = new HashMap<>();
        if (UserPreferences.getFocus(context)) hashMap.put(DatabaseService.FetchContract.PARAM_EIN, UserPreferences.getEin(context));
        else {
//            hashMap.put(DatabaseService.FetchContract.PARAM_RECORD, UserPreferences.getTerm(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_CITY, UserPreferences.getCity(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_STATE, UserPreferences.getState(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_ZIP, UserPreferences.getZip(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_MIN_RATING, UserPreferences.getMinrating(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_FILTER, UserPreferences.getFilter(context) ? "1" : "0");
            hashMap.put(DatabaseService.FetchContract.PARAM_SORT, UserPreferences.getSort(context) + ":" + UserPreferences.getOrder(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_PAGE_NUM, UserPreferences.getPages(context));
            hashMap.put(DatabaseService.FetchContract.PARAM_PAGE_SIZE, UserPreferences.getRows(context));
        }
        DatabaseService.startActionFetchSearch(getBaseContext(), hashMap);
//        mSnackbar = getString(R.string.message_record_refresh);
    }

    /**
     * Defines and launches Intent for displaying {@link ConfigActivity.RecordPreferenceFragment}.
     *//*

    private static void launchFilterPreferences(Context context) {
        Intent filterIntent = new Intent(context, ConfigActivity.class);
        filterIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, ConfigActivity.RecordPreferenceFragment.class.getName());
        filterIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
        filterIntent.setAction(ACTION_RECORD_INTENT);
        context.startActivity(filterIntent);
    }
*/

    /**
     * Populates {@link RecordActivity} {@link RecyclerView}.
     */
    class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

        private ContentValues[] mValuesArray;

        private View mLastClicked;

        /**
         * Generates a Layout for the ViewHolder based on its Adapter position and orientation
         */
        @Override public @NonNull ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_record, parent, false);
            return new ViewHolder(view);
        }

        /**
         * Updates contents of the {@code ViewHolder} to displays movie data at the specified position.
         */
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            if (mValuesArray == null || mValuesArray.length == 0) return;

            ContentValues values = mValuesArray[position];
            String ein = values.getAsString(DatabaseContract.Entry.COLUMN_EIN);
            String name = values.getAsString(DatabaseContract.Entry.COLUMN_CHARITY_NAME);
            String city = values.getAsString(DatabaseContract.Entry.COLUMN_LOCATION_CITY);
            String state = values.getAsString(DatabaseContract.Entry.COLUMN_LOCATION_STATE);
            String zip = values.getAsString(DatabaseContract.Entry.COLUMN_LOCATION_ZIP);
            String homepage = values.getAsString(DatabaseContract.Entry.COLUMN_HOMEPAGE_URL);
            String url = values.getAsString(DatabaseContract.Entry.COLUMN_NAVIGATOR_URL);

            holder.mNameView.setText(name);
            holder.mIdView.setText(String.format("EIN: %s", ein));
            holder.mAddressView.setText(String.format("%s, %s %s", city, state, zip));

            Glide.with(RecordActivity.this).load("https://logo.clearbit.com/" + homepage)
                    .into(holder.mLogoView);

            Bundle arguments = new Bundle();
            arguments.putString(DetailFragment.ARG_ITEM_NAME, name);
            arguments.putString(DetailFragment.ARG_ITEM_EIN, ein);
            arguments.putString(DetailFragment.ARG_ITEM_URL, url);

            holder.itemView.setTag(arguments);
        }

        /**
         * Returns the number of items to display.
         */
        @Override
        public int getItemCount() {
            return mValuesArray != null ? mValuesArray.length : 0;
        }

        /**
         * Swaps the Cursor after completing a load or resetting Loader.
         */
        private void swapValues(ContentValues[] valuesArray) {
            mValuesArray = valuesArray;
            notifyDataSetChanged();
        }

        /**
         * Provides ViewHolders for binding Adapter list items to the presentable area in {@link RecyclerView}.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            @BindView(R.id.record_item_primary) TextView mNameView;
            @BindView(R.id.record_item_secondary) TextView mIdView;
            @BindView(R.id.record_item_tertiary) TextView mAddressView;
            @BindView(R.id.record_item_logo) ImageView mLogoView;

            /**
             * Constructs this instance with the list item Layout generated from Adapter onCreateViewHolder.
             */
            ViewHolder(View view) {
                super(view);
                ButterKnife.bind(this, view);
            }

            /**
             * Provides ViewHolders for binding Adapter list items to the presentable area in {@link RecyclerView}.
             */
            @OnClick(R.id.record_item_view) void togglePane(View v) {
                if (mLastClicked != null && mLastClicked.equals(v)) sDualPane = !sDualPane;
                else sDualPane = true;

                mLastClicked = v;
                if (sDualPane) showDualPane((Bundle) v.getTag());
                else showSinglePane();
            }
        }
    }
}
