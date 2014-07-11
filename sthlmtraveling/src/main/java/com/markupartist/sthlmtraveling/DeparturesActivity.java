/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.PlacesProvider.Place.Places;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departure;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departures;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

import java.io.IOException;
import java.util.ArrayList;


public class DeparturesActivity extends BaseFragmentActivity {
    static String EXTRA_SITE_NAME = "com.markupartist.sthlmtraveling.siteName";
    static String EXTRA_SITE = "com.markupartist.sthlmtraveling.site";

    private static final String STATE_GET_SITES_IN_PROGRESS =
            "com.markupartist.sthlmtraveling.getsites.inprogress";
    private static final String STATE_GET_DEPARTURES_IN_PROGRESS =
            "com.markupartist.sthlmtraveling.getdepartures.inprogress";
    private static final String STATE_SITE =
            "com.markupartist.sthlmtraveling.site";
    private static final String STATE_PREFERRED_TRANSPORT_MODE =
            "com.markupartist.sthlmtraveling.departures.transportmode";

    static String TAG = "DeparturesActivity";

    private static final int DIALOG_SITE_ALTERNATIVES = 0;
    private static final int DIALOG_GET_SITES_NETWORK_PROBLEM = 1;
    private static final int DIALOG_GET_DEPARTURES_NETWORK_PROBLEM = 3;

    private Site mSite;
    private static ArrayList<Site> mSiteAlternatives;

    private ProgressDialog mProgress;
    private GetSitesTask mGetSitesTask;
    private GetDeparturesTask mGetDeparturesTask;
    private String mSiteName;
    private int mSiteId;
    private Departures mDepartureResult;
    private Bundle mSavedState;

    //private DepartureAdapter mSectionedAdapter;

    private int mPreferredTrafficMode = TransportMode.METRO_INDEX;
    private int mPlaceId = -1;

    private HistoryDbAdapter mHistoryDbAdapter;
    private ViewPager mPager;
    private PageFragmentAdapter mPageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.departures);

        registerScreen("Departures");

        Bundle extras = getIntent().getExtras();

        if (extras.containsKey(EXTRA_SITE)) {
            mSite = extras.getParcelable(EXTRA_SITE);
            setTitle(mSite.getName());
        } else if (extras.containsKey(EXTRA_SITE_NAME)) {
            mSiteName = extras.getString(EXTRA_SITE_NAME);
            setTitle(mSiteName);
        } else {
            Log.e(TAG, "Could not open activity, missing site id or name.");
            finish();
        }

        mPageAdapter = new PageFragmentAdapter(this, getSupportFragmentManager());
        mPageAdapter.setCount(4);

        Bundle metroArg = new Bundle();
        metroArg.putInt(DepartureFragment.ARG_TRANSPORT_TYPE, TransportMode.METRO_INDEX);
        mPageAdapter.addPage(new PageInfo(getString(R.string.departure_metro), DepartureFragment.class, metroArg));

        Bundle busArg = new Bundle();
        busArg.putInt(DepartureFragment.ARG_TRANSPORT_TYPE, TransportMode.BUS_INDEX);
        mPageAdapter.addPage(new PageInfo(getString(R.string.departure_bus), DepartureFragment.class, busArg));

        Bundle trainArg = new Bundle();
        trainArg.putInt(DepartureFragment.ARG_TRANSPORT_TYPE, TransportMode.TRAIN_INDEX);
        mPageAdapter.addPage(new PageInfo(getString(R.string.departures), DepartureFragment.class, trainArg));

        Bundle tramArg = new Bundle();
        tramArg.putInt(DepartureFragment.ARG_TRANSPORT_TYPE, TransportMode.LOKALBANA_INDEX);
        mPageAdapter.addPage(new PageInfo(getString(R.string.deviations_label), DepartureFragment.class, tramArg));

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMarginDrawable(R.color.light_grey);
        mPager.setPageMargin(25);  // TODO: Compensate with denisity to get it right on all screens
        mPager.setAdapter(mPageAdapter);
        mPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mPreferredTrafficMode = position;
                        getSupportActionBar().setSelectedNavigationItem(position);
                    }
                }
        );

        initActionBar();

        // Not ideal.
        mHistoryDbAdapter = new HistoryDbAdapter(this).open();
    }

    @Override
    protected ActionBar initActionBar() {
        ActionBar ab = super.initActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayUseLogoEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                mPager.setCurrentItem(tab.getPosition());
                mPreferredTrafficMode = tab.getPosition();
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            }
        };

        ab.addTab(ab.newTab().setIcon(R.drawable.transport_metro).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.transport_bus).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.transport_train).setTabListener(tabListener));
        ab.addTab(ab.newTab().setIcon(R.drawable.transport_tram_car).setTabListener(tabListener));

        return ab;
    }

    private void selectPreferredTransport() {
        int selectedItem = 0;
        switch (mPreferredTrafficMode) {
            case TransportMode.METRO_INDEX:
                selectedItem = 0;
                break;
            case TransportMode.BUS_INDEX:
                selectedItem = 1;
                break;
            case TransportMode.TRAIN_INDEX:
                selectedItem = 2;
                break;
            case TransportMode.LOKALBANA_INDEX:
                selectedItem = 3;
                break;
        }
        mPager.setCurrentItem(selectedItem, true);
        getSupportActionBar().setSelectedNavigationItem(selectedItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.actionbar_departures, menu);
        return true;
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.actionbar_item_refresh:
                new GetDeparturesTask().execute(mSite);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * We need to call loadDeapartures after restoreLocalState that's
     * why we need to override this method. Only needed for 1.5 devices though.
     *
     * @see android.app.Activity#onPostCreate(android.os.Bundle)
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        loadDepartures();

        super.onPostCreate(savedInstanceState);
    }

    private void loadDepartures() {
        @SuppressWarnings("unchecked")
//        final Departures departureResult =
//                (Departures) getLastNonConfigurationInstance();
        Departures departureResult = null;
        if (departureResult != null) {
            onData(departureResult);
        } else if (mSite != null && mSite.getId() > 0) {
            mGetDeparturesTask = new GetDeparturesTask();
            mGetDeparturesTask.execute(mSite);
        } else {
            // Handle legacy history that does not have the site id set.
            if (mSite != null) {
                mSiteName = mSite.getName();
            }
            mGetSitesTask = new GetSitesTask();
            mGetSitesTask.execute(mSiteName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSavedState != null) restoreLocalState(mSavedState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        onCancelGetDeparturesTask();
        onCancelGetSitesTask();

        dismissProgress();

        int preferredTransportType = mPreferredTrafficMode;

        // TODO: Do in background thread.
        if (mSite != null) {
            if (mPlaceId == -1) {
                ContentValues values = new ContentValues();
                values.put(Places.NAME, mSite.getName());
                values.put(Places.SITE_ID, mSite.getId());
                values.put(Places.PREFERRED_TRANSPORT_MODE, preferredTransportType);
                Uri uri = getContentResolver().insert(Places.CONTENT_URI, values);
            } else {
                ContentValues values = new ContentValues();
                values.put(Places.NAME, mSite.getName());
                values.put(Places.SITE_ID, mSite.getId());
                values.put(Places.PREFERRED_TRANSPORT_MODE, preferredTransportType);
                int updated = getContentResolver().update(Places.CONTENT_URI, values,
                        Places.SITE_ID + "= ?",
                        new String[]{String.valueOf(mSite.getId())});
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSite != null) {
            outState.putParcelable(STATE_SITE, mSite);
        }

        outState.putInt(STATE_PREFERRED_TRANSPORT_MODE, mPreferredTrafficMode);

        saveGetSitesTask(outState);
        saveGetDeparturesTask(outState);

        mSavedState = outState;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        mSavedState = null;
    }

    /**
     * Restores any local state, if any.
     *
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(STATE_SITE)) {
            mSite = savedInstanceState.getParcelable(STATE_SITE);
        }
        if (savedInstanceState.containsKey(STATE_PREFERRED_TRANSPORT_MODE)) {
            mPreferredTrafficMode = savedInstanceState.getInt(
                    STATE_PREFERRED_TRANSPORT_MODE);
        }

        restoreGetSitesTask(savedInstanceState);
        restoreGetDeparturesTask(savedInstanceState);
    }

    /**
     * Cancels the {@link GetSitesTask} if it is running.
     */
    private void onCancelGetSitesTask() {
        if (mGetSitesTask != null &&
                mGetSitesTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels GetSitesTask.");
            mGetSitesTask.cancel(true);
            mGetSitesTask = null;
        }
    }

    /**
     * Restores the {@link GetSitesTask}.
     *
     * @param savedInstanceState the saved state
     */
    private void restoreGetSitesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_SITES_IN_PROGRESS)) {
            mSiteName = savedInstanceState.getString(EXTRA_SITE_NAME);
            Log.d(TAG, "restoring getSitesTask");
            mGetSitesTask = new GetSitesTask();
            mGetSitesTask.execute(mSiteName);
        }
    }

    /**
     * Saves the state of {@link GetSitesTask}.
     *
     * @param outState
     */
    private void saveGetSitesTask(Bundle outState) {
        final GetSitesTask task = mGetSitesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetSitesTask");
            task.cancel(true);
            mGetSitesTask = null;
            outState.putBoolean(STATE_GET_SITES_IN_PROGRESS, true);
            outState.putString(EXTRA_SITE_NAME, mSiteName);
        }
    }

    /**
     * Cancels the {@link GetDeparturesTask} if it is running.
     */
    private void onCancelGetDeparturesTask() {
        if (mGetDeparturesTask != null/* &&
                mGetDeparturesTask.getStatus() == AsyncTask.Status.RUNNING*/) {
            Log.i(TAG, "Cancels GetDeparturesTask.");
            mGetDeparturesTask.cancel(true);
            mGetDeparturesTask = null;
        }
    }

    /**
     * Restores the {@link GetDeparturesTask}.
     *
     * @param savedInstanceState the saved state
     */
    private void restoreGetDeparturesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_DEPARTURES_IN_PROGRESS)) {
            Log.d(TAG, "restoring getSitesTask");
            mGetDeparturesTask = new GetDeparturesTask();
            mGetDeparturesTask.execute(mSite);
        }
    }

    /**
     * Saves the state of {@link GetDeparturesTask}.
     *
     * @param outState
     */
    private void saveGetDeparturesTask(Bundle outState) {
        final GetDeparturesTask task = mGetDeparturesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetDeparturesState");
            task.cancel(true);
            mGetSitesTask = null;
            outState.putBoolean(STATE_GET_DEPARTURES_IN_PROGRESS, true);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SITE_ALTERNATIVES:
                ArrayAdapter<Site> siteAdapter =
                        new ArrayAdapter<Site>(this, R.layout.simple_dropdown_item_1line,
                                mSiteAlternatives);
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.did_you_mean)
                        .setAdapter(siteAdapter, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new GetDeparturesTask().execute(
                                        mSiteAlternatives.get(which));
                            }
                        })
                        .setOnCancelListener(new OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                // We need a site to proceed, finish the activity here
                                // is most likely what the user intended.
                                finish();
                            }
                        })
                        .create();
            case DIALOG_GET_SITES_NETWORK_PROBLEM:
                return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mGetSitesTask = new GetSitesTask();
                        mGetSitesTask.execute(mSiteName);
                    }
                });
            case DIALOG_GET_DEPARTURES_NETWORK_PROBLEM:
                return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mGetDeparturesTask = new GetDeparturesTask();
                        mGetDeparturesTask.execute(mSite);
                    }
                });
        }
        return null;
    }

    /**
     * Show progress dialog.
     */
    private void showProgress() {
        setSupportProgressBarIndeterminateVisibility(true);
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancelGetSitesTask();
        onCancelGetDeparturesTask();

        dismissProgress();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    /**
     * Background job for getting {@link Site}s.
     */
    private class GetSitesTask extends AsyncTask<String, Void, ArrayList<Site>> {
        private boolean mWasSuccess = true;
        private String mSearchQuery;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Site> doInBackground(String... params) {
            mSearchQuery = params[0];
            try {
                return SitesStore.getInstance().getSite(DeparturesActivity.this, mSearchQuery);
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Site> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                if (result.size() == 1) {
                    new GetDeparturesTask().execute(result.get(0));
                } else {
                    // Has exact match?
                    for (Site site : result) {
                        if (site.getName().equals(mSearchQuery)) {
                            new GetDeparturesTask().execute(result.get(0));
                            return;
                        }
                    }
                    mSiteAlternatives = result;
                    try {
                        showDialog(DIALOG_SITE_ALTERNATIVES);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show sites dialog.");
                    }
                }
            } else if (!mWasSuccess) {
                try {
                    showDialog(DIALOG_GET_SITES_NETWORK_PROBLEM);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                }
            } else {
                //    onNoRoutesDetailsResult();
            }
        }
    }

    /**
     * Background job for getting {@link Departure}s.
     */
    private class GetDeparturesTask extends AsyncTask<Site, Void, Departures> {
        private boolean mWasSuccess = true;

        protected boolean wasSuccess() {
            return mWasSuccess;
        }

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Departures doInBackground(Site... params) {
            try {
                mSite = params[0];

                // Update history if site id is not set, we do this to patch old versions
                // that will end up here without site id set.
                if (mSite.getId() > 0) {
                    mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_DEPARTURE_SITE, mSite);
                }

                DeparturesStore departures = new DeparturesStore();
                Departures result = departures.find(DeparturesActivity.this, params[0]);

                if (mPreferredTrafficMode < 1 && !result.servesTypes.isEmpty()) {
                    String transportMode = result.servesTypes.get(0);
                    mPreferredTrafficMode = TransportMode.getIndex(transportMode);
                }

                if (mPlaceId == -1) {
                    String[] projection = new String[]{
                            Places._ID,
                            Places.NAME,
                            Places.PREFERRED_TRANSPORT_MODE,
                            Places.SITE_ID
                    };
                    Uri sitesUri = Places.CONTENT_URI;
                    Cursor sitesCursor = managedQuery(sitesUri, projection,
                            Places.SITE_ID + "= ?",
                            new String[]{String.valueOf(mSite.getId())},
                            Places.NAME + " asc");
                    if (sitesCursor.moveToFirst()) {
                        mPlaceId = sitesCursor.getInt(sitesCursor.getColumnIndex(Places._ID));
                        mPreferredTrafficMode = sitesCursor.getInt(sitesCursor.getColumnIndex(Places.PREFERRED_TRANSPORT_MODE));
                    }
                }

                return result;
            } catch (IllegalArgumentException e) {
                mWasSuccess = false;
                return null;
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Departures result) {
            dismissProgress();

            if (mWasSuccess) {
                onData(result);
            } else {
                try {
                    showDialog(DIALOG_GET_DEPARTURES_NETWORK_PROBLEM);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                }
            }
        }
    }

    private void onData(Departures result) {
        selectPreferredTransport();

        // TODO: Fix this mess.
        Bundle b0 = mPageAdapter.getPageArgs(0);
        b0.putSerializable(DepartureFragment.ARG_DEPARTURES, result);
        mPageAdapter.updatePageArgs(0, b0);

        Bundle b1 = mPageAdapter.getPageArgs(1);
        b1.putSerializable(DepartureFragment.ARG_DEPARTURES, result);
        mPageAdapter.updatePageArgs(1, b1);

        Bundle b2 = mPageAdapter.getPageArgs(2);
        b2.putSerializable(DepartureFragment.ARG_DEPARTURES, result);
        mPageAdapter.updatePageArgs(2, b2);

        Bundle b3 = mPageAdapter.getPageArgs(3);
        b3.putSerializable(DepartureFragment.ARG_DEPARTURES, result);
        mPageAdapter.updatePageArgs(3, b3);

        updateFragment(0, result);
        updateFragment(1, result);
        updateFragment(2, result);
        updateFragment(3, result);
    }

    private void updateFragment(int position, Departures departures) {
        Fragment f = this.getSupportFragmentManager().findFragmentByTag(getFragmentTag(position));
        if (f != null) {
            ((DepartureFragment) f).update(departures);
        }
    }

    /**
     * Get fragment by the same naming pattern that ViewPager use internally.
     * <p/>
     * We should not do this since might break when their api changes.
     *
     * @param pos position of the fragment tag
     * @return A fragment tag
     */
    private String getFragmentTag(int pos){
        return "android:switcher:"+R.id.pager+":"+pos;
    }

    public class PageFragmentAdapter extends FragmentPagerAdapter /*implements TitleProvider*/ {

        private ArrayList<PageInfo> mPages = new ArrayList<PageInfo>();
        private Context mContext;
        private int mCount;

        public PageFragmentAdapter(Context activity, FragmentManager fm) {
            super(fm);
            mContext = activity;
        }

        public void setCount(final int count) {
            mCount = count;
        }

        public Bundle getPageArgs(int position) {
            return mPages.get(position).mArgs;
        }

        public void updatePageArgs(int position, Bundle args) {
            mPages.get(position).mArgs = args;
        }

        public void addPage(PageInfo page) {
            mPages.add(page);
        }

        @Override
        public Fragment getItem(int position) {
            PageInfo page = mPages.get(position);
            return Fragment.instantiate(mContext,
                    page.getFragmentClass().getName(), page.getArgs());
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPages.get(position).getTextResource().toUpperCase();
        }

    }

    public class PageInfo {
        private String mTextResource;
        private Class<?> mFragmentClass;
        private Bundle mArgs;

        public PageInfo(String textResource, Class<?> fragmentClass,
                        Bundle args) {
            mTextResource = textResource;
            mFragmentClass = fragmentClass;
            mArgs = args;
        }

        public String getTextResource() {
            return mTextResource;
        }

        public Class<?> getFragmentClass() {
            return mFragmentClass;
        }

        public Bundle getArgs() {
            return mArgs;
        }
    }

    public static class DepartureFragment extends SherlockListFragment {
        public static String ARG_DEPARTURES = "ARG_DEPARTURES";
        public static String ARG_TRANSPORT_TYPE = "ARG_TRANSPORT_TYPE";

        private DepartureAdapter mSectionedAdapter;
        private Departures mDepartureResult;

        public DepartureFragment() {
            setRetainInstance(true);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            restoreDepartures(savedInstanceState);

            updateViews();
        }

        private void restoreDepartures(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                mDepartureResult = (Departures) savedInstanceState.getSerializable(ARG_DEPARTURES);
            }
            if (mDepartureResult == null) {
                Bundle args = getArguments();
                if (args != null) {
                    mDepartureResult = (Departures) args.getSerializable(ARG_DEPARTURES);
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.departures_list, container, false);

            return view;
        }

        private void updateViews() {
            mSectionedAdapter = new DepartureAdapter(getActivity());
            View emptyView = getListView().getEmptyView();
            getListView().setEmptyView(emptyView);

            if (mDepartureResult != null) {
                TextView text = (TextView) emptyView.findViewById(R.id.search_progress_text);
                text.setText(R.string.no_departures_for_transport_type);
                ProgressBar progressBar = (ProgressBar) emptyView.findViewById(R.id.search_progress_bar);
                progressBar.setVisibility(View.GONE);

                mSectionedAdapter.fillDepartures(mDepartureResult, getArguments().getInt(ARG_TRANSPORT_TYPE));
            }
            setListAdapter(mSectionedAdapter);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            if (mDepartureResult != null) {
                outState.putSerializable(ARG_DEPARTURES, mDepartureResult);
            }
        }

        public void update(Departures result) {
            mDepartureResult = result;
            if (isVisible()) {
                updateViews();
            }
        }
    }
}
