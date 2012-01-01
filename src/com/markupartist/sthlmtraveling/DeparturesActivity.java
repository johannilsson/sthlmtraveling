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

import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.google.ads.AdView;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.actionbar.R;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.PlacesProvider.Place.Places;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departure;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore.Departures;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;
import com.markupartist.sthlmtraveling.utils.AdRequestFactory;


public class DeparturesActivity extends BaseListActivity {
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

    private DepartureAdapter mSectionedAdapter;

    private int mPreferredTrafficMode = TransportMode.METRO_INDEX;
    private int mPlaceId = -1;

    private ActionBar mActionBar;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.departures_list);

        registerEvent("Departures");

        Bundle extras = getIntent().getExtras();
        
        if (extras.containsKey(EXTRA_SITE)) {
            mSite = extras.getParcelable(EXTRA_SITE);
        } else if (extras.containsKey(EXTRA_SITE_NAME)) {
            mSiteName = extras.getString(EXTRA_SITE_NAME);
        } else {
            Log.e(TAG, "Could not open activity, missing site id or name.");
            finish();
        }

        mSectionedAdapter = new DepartureAdapter(this);

        mActionBar = initActionBar(R.menu.actionbar_departures);
        mActionBar.setTitle(R.string.departures);

        mAdView = (AdView) findViewById(R.id.ad_view);
        if (AppConfig.ADS_ENABLED) {
            mAdView.loadAd(AdRequestFactory.createRequest());
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionBar.setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onRotationChange(newConfig);

        super.onConfigurationChanged(newConfig);
    }

    private void onRotationChange(Configuration newConfig) {
        if (newConfig.orientation == newConfig.ORIENTATION_LANDSCAPE) {
            if (mAdView != null) {
                mAdView.setVisibility(View.GONE);
            }
        } else {
            if (mAdView != null) {
                mAdView.setVisibility(View.VISIBLE);
            }
        }        
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_refresh:
            new GetDeparturesTask().execute(mSite);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * We need to call loadDeapartures after restoreLocalState that's
     * why we need to override this method. Only needed for 1.5 devices though.
     * @see android.app.Activity#onPostCreate(android.os.Bundle)
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        loadDepartures();

        super.onPostCreate(savedInstanceState);
    }

    OnCheckedChangeListener mOnTransportModeChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                handleCheckedTransportMode(buttonView.getId());
            }
        }
    };

    private void handleCheckedTransportMode(int id) {
        switch (id) {
        case R.id.radio_metros:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    TransportMode.METRO_INDEX);
            mPreferredTrafficMode = TransportMode.METRO_INDEX;
            break;
        case R.id.radio_buses:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    TransportMode.BUS_INDEX);
            mPreferredTrafficMode = TransportMode.BUS_INDEX;
            break;
        case R.id.radio_trains:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    TransportMode.TRAIN_INDEX);
            mPreferredTrafficMode = TransportMode.TRAIN_INDEX;
            break;
        case R.id.radio_trams:
            mSectionedAdapter.fillDepartures(mDepartureResult,
                    TransportMode.LOKALBANA_INDEX);
            mPreferredTrafficMode = TransportMode.LOKALBANA_INDEX;
            break;
        }

        setListAdapter(mSectionedAdapter);
        //getListView().getEmptyView().setVisibility(View.GONE);
    }

    public void setupFilterButtons() {
        // TODO: Fix hard coded values for preferred traffic mode.
        RadioButton radioMetros = (RadioButton) findViewById(R.id.radio_metros);
        radioMetros.setOnCheckedChangeListener(mOnTransportModeChange);
        radioMetros.setEnabled(true);
        radioMetros.setChecked(mPreferredTrafficMode == TransportMode.METRO_INDEX ? true : false);
        RadioButton radioBuses = (RadioButton) findViewById(R.id.radio_buses);
        radioBuses.setOnCheckedChangeListener(mOnTransportModeChange);
        radioBuses.setEnabled(true);
        radioBuses.setChecked(mPreferredTrafficMode == TransportMode.BUS_INDEX ? true : false);
        RadioButton radioTrains = (RadioButton) findViewById(R.id.radio_trains);
        radioTrains.setOnCheckedChangeListener(mOnTransportModeChange);
        radioTrains.setEnabled(true);
        radioTrains.setChecked(mPreferredTrafficMode == TransportMode.TRAIN_INDEX ? true : false);
        RadioButton radioTrams = (RadioButton) findViewById(R.id.radio_trams);
        radioTrams.setOnCheckedChangeListener(mOnTransportModeChange);
        radioTrams.setEnabled(true);
        radioTrams.setChecked(mPreferredTrafficMode == TransportMode.LOKALBANA_INDEX ? true : false);
    }

    private void loadDepartures() {
        @SuppressWarnings("unchecked")
        final Departures departureResult =
            (Departures) getLastNonConfigurationInstance();
        if (departureResult != null) {
            fillData(departureResult);
        } else if (mSite != null) {
            mGetDeparturesTask = new GetDeparturesTask();
            mGetDeparturesTask.execute(mSite);
        } else {
            mGetSitesTask = new GetSitesTask();
            mGetSitesTask.execute(mSiteName);
        }
    }

    private void fillData(Departures result) {
        // TODO: Get the selected type...
        mDepartureResult = result;

        RadioGroup transportGroup = (RadioGroup) findViewById(R.id.transport_group);

        setupFilterButtons();

        int checkedId = transportGroup.getCheckedRadioButtonId();
        handleCheckedTransportMode(checkedId);

        setTitle(mSite.getName());

        Time now = new Time();
        now.setToNow();

        // Adjust the empty view.
        /*
        LinearLayout emptyView = (LinearLayout) getListView().getEmptyView();
        TextView text = (TextView) emptyView.findViewById(R.id.search_progress_text);
        text.setText(R.string.no_departures_for_transport_type);
        ProgressBar progressBar = (ProgressBar) emptyView.findViewById(R.id.search_progress_bar);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP|Gravity.LEFT;
        emptyView.setLayoutParams(params);
        getListView().setEmptyView(emptyView);
        */
        View emptyView = getListView().getEmptyView();
        TextView text = (TextView) emptyView.findViewById(R.id.search_progress_text);
        text.setText(R.string.no_departures_for_transport_type);
        ProgressBar progressBar = (ProgressBar) emptyView.findViewById(R.id.search_progress_bar);
        progressBar.setVisibility(View.GONE);
        getListView().setEmptyView(emptyView);

        /*
        PullToRefreshListView listView = (PullToRefreshListView) getListView();
        listView.setLastUpdated("Updated at " + now.format("%H:%M"));
        listView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                new RefreshDeparturesTask().execute(mSite);
            }

        });
        */
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
                        new String[] {String.valueOf(mSite.getId())});
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDepartureResult;
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
            mGetDeparturesTask= null;
        }
    }

    /**
     * Restores the {@link GetDeparturesTask}.
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
        switch(id) {
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
        if (mActionBar != null) {
            mActionBar.setProgressBarVisibility(View.VISIBLE);
        }
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        if (mActionBar != null) {
            mActionBar.setProgressBarVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }

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
                return SitesStore.getInstance().getSite(mSearchQuery);
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

                DeparturesStore departures = new DeparturesStore();
                Departures result = departures.find(params[0]);

                if (mPreferredTrafficMode < 1 && !result.servesTypes.isEmpty()) {
                    String transportMode = result.servesTypes.get(0);
                    mPreferredTrafficMode = TransportMode.getIndex(transportMode);
                }

                if (mPlaceId == -1) {
                    String[] projection = new String[] {
                                                 Places._ID,
                                                 Places.NAME,
                                                 Places.PREFERRED_TRANSPORT_MODE,
                                                 Places.SITE_ID
                                              };
                    Uri sitesUri =  Places.CONTENT_URI;
                    Cursor sitesCursor = managedQuery(sitesUri, projection,
                            Places.SITE_ID + "= ?",
                            new String[] {String.valueOf(mSite.getId())},
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
                fillData(result);
            } else {
                try {
                    showDialog(DIALOG_GET_DEPARTURES_NETWORK_PROBLEM);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                }
            }
        }
    }
}
