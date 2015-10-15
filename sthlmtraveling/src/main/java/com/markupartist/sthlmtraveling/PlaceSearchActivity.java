/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.adapter.GooglePlacesFilter;
import com.markupartist.sthlmtraveling.ui.adapter.PlaceItem;
import com.markupartist.sthlmtraveling.ui.adapter.PlaceSearchResultAdapter;
import com.markupartist.sthlmtraveling.ui.adapter.SiteFilter;
import com.markupartist.sthlmtraveling.ui.view.ItemClickSupport;
import com.markupartist.sthlmtraveling.utils.ViewHelper;
import com.yqritc.recyclerviewmultipleviewtypesadapter.DataBindAdapter;
import com.yqritc.recyclerviewmultipleviewtypesadapter.DataBinder;
import com.yqritc.recyclerviewmultipleviewtypesadapter.ListBindAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for searching for address, transit stop and or places.
 */
public class PlaceSearchActivity extends BaseFragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_ONLY_STOPS = "com.markupartist.sthlmtraveling.placesearch.only_stops";
    public static final String EXTRA_PLACE = "com.markupartist.sthlmtraveling.placesearch.stop";

    protected static final int REQUEST_CODE_POINT_ON_MAP = 0;
    private static final String STATE_SEARCH_FILTER = "STATE_SEARCH_FILTER";

    private static final int FILTER_TYPE_GOOGLE = 1;
    private static final int FILTER_TYPE_STHLM_TRAVELING = 2;
    private static final int MESSAGE_TEXT_CHANGED = 100;

    private static final int LOADER_HISTORY = 1;

    private static final boolean IS_GOOGLE_PLACE_SEARCH_ENABLED = true;

    private HistoryDbAdapter mHistoryDbAdapter;
    private RecyclerView mHistoryRecyclerView;
    private RecyclerView mSearchResultRecyclerView;
    private DefaultListAdapter mDefaultListAdapter;
    private EditText mSearchEdit;
    private PlaceSearchResultAdapter mSearchResultAdapter;
    private ImageButton mClearButton;
    private SiteFilter mSthlmTravelingSearchFilter;
    private GooglePlacesFilter mGoogleSearchFilter;
    private int mCurrentSearchFilterType = FILTER_TYPE_GOOGLE;
    private Handler mHandler;
    private boolean mShouldSearchGooglePlaces;
    private View mSearchFailed;
    private ContentLoadingProgressBar mProgressBar;
    private boolean mSearchOnlyStops;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.place_search_layout);

        registerScreen("Place Search");

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(ARG_ONLY_STOPS)) {
                mSearchOnlyStops = extras.getBoolean(ARG_ONLY_STOPS);
            }
        }

        if (shouldSearchGooglePlaces()) {
            initGoogleApiClient(false);
        }
        createSearchHandler();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_SEARCH_FILTER)) {
                mCurrentSearchFilterType = savedInstanceState.getInt(STATE_SEARCH_FILTER);
            }
        }

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        ImageButton backButton = (ImageButton) findViewById(R.id.search_back);
        ViewHelper.tint(backButton, getResources().getColor(R.color.primary_dark));
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        mSearchEdit = (EditText) findViewById(R.id.search_edit);
        mSearchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                        || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    mSearchResultAdapter.getFilter().filter(mSearchEdit.getText());
                }
                return false;
            }
        });
        mSearchEdit.requestFocus();

        mClearButton = (ImageButton) findViewById(R.id.search_clear);
        ViewHelper.tintIcon(mClearButton.getDrawable(), Color.GRAY);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchEdit.setText("");
                mSearchResultAdapter.clear();
            }
        });

        mSearchFailed = findViewById(R.id.search_result_error);
        mProgressBar = (ContentLoadingProgressBar) findViewById(R.id.search_progress_bar);

        setupHistoryViews();
        if (!mSearchOnlyStops) {
            getSupportLoaderManager().initLoader(LOADER_HISTORY, null, this);
        }
        setupSearchResultViews();

        if (!shouldSearchGooglePlaces()) {
            mShouldSearchGooglePlaces = false;
            setSearchFilter(FILTER_TYPE_STHLM_TRAVELING);
        }
    }

    boolean shouldSearchGooglePlaces() {
        if (mSearchOnlyStops) {
            return false;
        }
        return IS_GOOGLE_PLACE_SEARCH_ENABLED;
    }

    public void createSearchHandler() {
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_TEXT_CHANGED:
                        mSearchResultAdapter.getFilter().filter((String) msg.obj);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_SEARCH_FILTER, mCurrentSearchFilterType);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mSearchEdit.addTextChangedListener(new AutoCompleter());
    }

    public void setSearchFilter(int filterType) {
        PlaceSearchResultAdapter.SearchFooterItem footerItem = new PlaceSearchResultAdapter.SearchFooterItem();
        switch (filterType) {
            case FILTER_TYPE_GOOGLE:
                mSearchResultAdapter.setFilter(mGoogleSearchFilter);
                footerItem.text1 = getString(R.string.search_try_with_app_name);
                footerItem.iconResource = R.drawable.powered_by_google_light;
                break;
            case FILTER_TYPE_STHLM_TRAVELING:
                mSearchResultAdapter.setFilter(mSthlmTravelingSearchFilter);
                footerItem.text1 = getString(R.string.search_try_with_google);
                footerItem.iconResource = -1;
                break;
        }

        if (mShouldSearchGooglePlaces) {
            mSearchResultAdapter.setFooterData(footerItem);
        }
        mCurrentSearchFilterType = filterType;
        if (!TextUtils.isEmpty(mSearchEdit.getText())) {
            mSearchResultAdapter.getFilter().filter(mSearchEdit.getText());
        }
    }

    void setupSearchResultViews() {
        mSearchResultRecyclerView = (RecyclerView) findViewById(R.id.search_results);
        mSearchResultRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mSearchResultRecyclerView.setLayoutManager(layoutManager);
        mSearchResultRecyclerView.addOnScrollListener(new HideKeyboardOnScroll(this, mSearchEdit));

        mSearchResultAdapter = new PlaceSearchResultAdapter(this);

        mSearchResultRecyclerView.setAdapter(mSearchResultAdapter);

        // Search result filters.
        AutocompleteResultCallback autocompleteResultCallback = new AutocompleteResultCallback();
        mSthlmTravelingSearchFilter = new SiteFilter(mSearchResultAdapter, this, mSearchOnlyStops);
        mSthlmTravelingSearchFilter.setFilterResultCallback(autocompleteResultCallback);
        mGoogleSearchFilter = new GooglePlacesFilter(mSearchResultAdapter, getGoogleApiClient());
        mGoogleSearchFilter.setFilterResultCallback(autocompleteResultCallback);

        ItemClickSupport.addTo(mSearchResultRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                if (position >= mSearchResultAdapter.getContentItemCount()) {
                    // We end up here if picking one of the footer views. Only supporting one for now.
                    if (mCurrentSearchFilterType == FILTER_TYPE_GOOGLE) {
                        setSearchFilter(FILTER_TYPE_STHLM_TRAVELING);
                    } else {
                        setSearchFilter(FILTER_TYPE_GOOGLE);
                    }
                } else {
                    PlaceItem item = mSearchResultAdapter.getItem(position);
                    mSearchResultAdapter.getFilter().setResultCallback(item, new PlaceSearchResultAdapter.PlaceFilter.PlaceItemResultCallback() {
                        @Override
                        public void onResult(Site site) {
                            deliverResult(site);
                        }

                        @Override
                        public void onError() {
                            Toast.makeText(PlaceSearchActivity.this,
                                    R.string.planner_error_title, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    void setupHistoryViews() {
        mHistoryRecyclerView = (RecyclerView) findViewById(R.id.search_history_list);
        mHistoryRecyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mHistoryRecyclerView.setLayoutManager(layoutManager);
        mHistoryRecyclerView.addOnScrollListener(new HideKeyboardOnScroll(this, mSearchEdit));

        // For now, skip the extras if we only searching for stops.
        if (mSearchOnlyStops) {
            return;
        }

        mDefaultListAdapter = new DefaultListAdapter(this);
        List<SimpleItemBinder.Item> options = new ArrayList<>();
        options.add(new SimpleItemBinder.Item(R.drawable.ic_my_location_24dp, getText(R.string.my_location)));
        options.add(new SimpleItemBinder.Item(R.drawable.ic_map_24dp, getText(R.string.point_on_map)));
        mDefaultListAdapter.setOptions(options);
        mHistoryRecyclerView.setAdapter(mDefaultListAdapter);

        ItemClickSupport.addTo(mHistoryRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                int viewType = mDefaultListAdapter.getItemViewType(position);
                DataBinder binder = mDefaultListAdapter.getDataBinder(viewType);
                int binderPosition = mDefaultListAdapter.getBinderPosition(position);

                if (binder instanceof HistoryBinder) {
                    Site site = ((HistoryBinder) binder).getItem(binderPosition);
                    deliverResult(site);
                } else if (binder instanceof SimpleItemBinder) {
                    switch (binderPosition) {
                        case 0:
                            Site currentLocation = new Site();
                            currentLocation.setName(Site.TYPE_MY_LOCATION);
                            deliverResult(currentLocation);
                            break;
                        case 1:
                            Intent i = new Intent(PlaceSearchActivity.this, PointOnMapActivity.class);
                            i.putExtra(PointOnMapActivity.EXTRA_STOP, new Site());
                            i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                                    getString(R.string.tap_your_start_point_on_map));
                            startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP);
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mHistoryDbAdapter.close();
        super.onDestroy();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_HISTORY:
                return new HistoryLoader(this, mHistoryDbAdapter);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ArrayList<Site> historyList = new ArrayList<Site>();
        data.moveToFirst();
        for (int i = 0; i < data.getCount(); i++) {
            Site site = HistoryDbAdapter.mapToSite(data);
            historyList.add(site);
            data.moveToNext();
        }
        mDefaultListAdapter.setHistoryData(historyList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);

        mShouldSearchGooglePlaces = true;
        setSearchFilter(mCurrentSearchFilterType);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mShouldSearchGooglePlaces = false;
        setSearchFilter(FILTER_TYPE_STHLM_TRAVELING);
    }

    /**
     * Returns the selected place to the caller.
     *
     * @param place
     */
    public void deliverResult(Site place) {
        setResult(RESULT_OK, (new Intent()).putExtra(EXTRA_PLACE, place));
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_POINT_ON_MAP:
                if (resultCode != Activity.RESULT_CANCELED) {
                    Site place = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                    if (!place.hasName()) {
                        place.setName(getString(R.string.point_on_map));
                    }
                    deliverResult(place);
                }
                break;
        }
    }

    private class AutoCompleter implements TextWatcher {
        private static final long AUTO_COMPLETE_DELAY = 600;

        public AutoCompleter() {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            final String searchTerm = s.toString();

            if (searchTerm.length() > 0) {
                if (mHistoryRecyclerView != null) {
                    mHistoryRecyclerView.setVisibility(View.GONE);
                }
                mSearchResultRecyclerView.setVisibility(View.VISIBLE);
            } else {
                mHandler.removeMessages(MESSAGE_TEXT_CHANGED);
                if (mHistoryRecyclerView != null) {
                    mHistoryRecyclerView.setVisibility(View.VISIBLE);
                }
                mSearchResultRecyclerView.setVisibility(View.GONE);
                mProgressBar.hide();
            }

            if (TextUtils.getTrimmedLength(searchTerm) <= 1) {
                if (TextUtils.getTrimmedLength(searchTerm) == 0) {
                    mSearchResultAdapter.clear();
                }
                mProgressBar.hide();
                return;
            }

            mProgressBar.show();
            mHandler.removeMessages(MESSAGE_TEXT_CHANGED);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(
                    MESSAGE_TEXT_CHANGED, searchTerm.trim()), AUTO_COMPLETE_DELAY);
        }
    }

    private class AutocompleteResultCallback implements PlaceSearchResultAdapter.PlaceFilter.FilterResultCallback {

        @Override
        public void onSuccess() {
            mProgressBar.hide();
        }

        @Override
        public void onError() {
            mProgressBar.hide();
        }
    }

    private static class HistoryLoader extends CursorLoader {

        private HistoryDbAdapter mHistoryDbAdapter;

        public HistoryLoader(Context context, HistoryDbAdapter historyDbAdapter) {
            super(context);
            mHistoryDbAdapter = historyDbAdapter;
        }

        @Override
        public Cursor loadInBackground() {
            return mHistoryDbAdapter.fetchLatest();
        }

    }


    private static class HideKeyboardOnScroll extends RecyclerView.OnScrollListener {
        private final Context mContext;
        private final EditText mEditText;

        public HideKeyboardOnScroll(Context context, EditText editText) {
            mContext = context;
            mEditText = editText;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                hideKeyboard();
            }
        }

        private void hideKeyboard() {
            mEditText.clearFocus();
            InputMethodManager imm = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }

    }


    public class DefaultListAdapter extends ListBindAdapter {

        public DefaultListAdapter(Context context) {
            addAllBinder(
                    new SimpleItemBinder(this, R.layout.row_place_search),
                    new SimpleItemBinder(this, R.layout.row_section,
                            new SimpleItemBinder.Item(
                                    -1, context.getText(R.string.history_label))),
                    new HistoryBinder(this, context));
        }

        public void setHistoryData(List<Site> dataSet) {
            ((HistoryBinder) getDataBinder(2)).replaceAll(dataSet);
        }

        public void setOptions(List<SimpleItemBinder.Item> options) {
            ((SimpleItemBinder) getDataBinder(0)).addItems(options);
        }
    }

    public static class SimpleItemBinder extends DataBinder<SimpleItemBinder.ViewHolder> {
        // TODO: Make it into a simple item binder with a custom item with title and icon...
        private final int mLayoutRes;
        private List<Item> mData = new ArrayList<>();

        public SimpleItemBinder(DataBindAdapter dataBindAdapter, @LayoutRes int layoutRes) {
            super(dataBindAdapter);
            mLayoutRes = layoutRes;
        }

        public SimpleItemBinder(DataBindAdapter dataBindAdapter, @LayoutRes int layoutRes, Item item) {
            super(dataBindAdapter);
            mLayoutRes = layoutRes;
            mData.add(item);
        }

        @Override
        public ViewHolder newViewHolder(ViewGroup parent) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(mLayoutRes, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            Item item = mData.get(position);
            ViewHelper.setText(holder.text1, item.title);
            if (holder.text2 != null) {
                holder.text2.setVisibility(View.GONE);
            }
            if (holder.icon != null) {
                holder.icon.setImageResource(item.icon);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void addItems(List<Item> items) {
            mData.clear();
            mData.addAll(items);
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;
            TextView distance;
            ImageView icon;

            public ViewHolder(View view) {
                super(view);
                text1 = (TextView) view.findViewById(R.id.text1);
                text2 = (TextView) view.findViewById(R.id.text2);
                distance = (TextView) view.findViewById(R.id.distance);
                icon = (ImageView) view.findViewById(R.id.icon);
            }
        }

        public static class Item {
            @DrawableRes public int icon;
            public CharSequence title;

            public Item(@DrawableRes int icon, CharSequence title) {
                this.icon = icon;
                this.title = title;
            }
        }
    }

    public static class HistoryBinder extends DataBinder<HistoryBinder.ViewHolder> {
        private final List<Site> mData = new ArrayList<>();
        private final Context mContext;

        public HistoryBinder(DataBindAdapter dataBindAdapter, Context context) {
            super(dataBindAdapter);
            mContext = context;
        }


        @Override
        public ViewHolder newViewHolder(ViewGroup parent) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_place_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void bindViewHolder(ViewHolder holder, int position) {
            Site place = mData.get(position);
            String title = place.getName();
            String subtitle = place.getLocality();

            // Use this for adding an astrix for google places results and to show attribution at the end.
//            title = title + "*";
//            CharacterStyle colorStyle = new ForegroundColorSpan(mContext.getResources().getColor(R.color.body_text_2));
//            CharacterStyle fontSize = new RelativeSizeSpan(0.8f);
//            CharacterStyle superScript = new SuperscriptSpan();
//            CharSequence entryTitle = SpanUtils.createSpannable(title, Pattern.compile("\\*"), colorStyle, fontSize, superScript);
//            ViewHelper.setText(holder.text1, entryTitle);
            ViewHelper.setText(holder.text1, title);
            if (subtitle != null) {
                ViewHelper.setText(holder.text2, subtitle);
                holder.text2.setVisibility(View.VISIBLE);
            } else {
                holder.text2.setVisibility(View.GONE);
            }
            holder.icon.setImageResource(R.drawable.ic_history_24dp);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public void replaceAll(List<Site> all) {
            mData.clear();
            mData.addAll(all);
            notifyDataSetChanged();
        }

        public Site getItem(int position) {
            return mData.get(position);
        }


        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;
            TextView distance;
            ImageView icon;

            public ViewHolder(View view) {
                super(view);
                text1 = (TextView) view.findViewById(R.id.text1);
                text2 = (TextView) view.findViewById(R.id.text2);
                distance = (TextView) view.findViewById(R.id.distance);
                icon = (ImageView) view.findViewById(R.id.icon);
            }
        }
    }

}
