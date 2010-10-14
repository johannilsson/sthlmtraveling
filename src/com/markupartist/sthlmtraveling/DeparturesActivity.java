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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.provider.departure.Departure;
import com.markupartist.sthlmtraveling.provider.departure.DepartureList;
import com.markupartist.sthlmtraveling.provider.departure.DeparturesStore;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;


public class DeparturesActivity extends BaseListActivity {
    static String EXTRA_SITE_NAME = "com.markupartist.sthlmtraveling.siteName";

    private static final String STATE_GET_SITES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getsites.inprogress";
    private static final String STATE_GET_DEPARTURES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getdepartures.inprogress";
    private static final String STATE_SITE =
        "com.markupartist.sthlmtraveling.site";

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
    private HashMap<String, DepartureList> mDepartureResult;
    private Bundle mSavedState;

    SectionedAdapter mSectionedAdapter = new SectionedAdapter() {
        protected View getHeaderView(Section section, int index, 
                View convertView, ViewGroup parent) {
            TextView result = (TextView) convertView;

            if (convertView == null)
                result = (TextView) getLayoutInflater().inflate(R.layout.header, null);

            result.setText(section.caption);
            return (result);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.departures_list);

        registerEvent("Departures");

        Bundle extras = getIntent().getExtras();
        mSiteName = extras.getString(EXTRA_SITE_NAME);
        
        //loadDepartures();
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

    private void loadDepartures() {
        @SuppressWarnings("unchecked")
        final HashMap<String, DepartureList> departureResult =
            (HashMap<String, DepartureList>) getLastNonConfigurationInstance();
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

    private void fillData(HashMap<String,DepartureList> result) {
        TextView emptyResultView = (TextView) findViewById(R.id.departures_empty_result);
        //TextView resultUpdatedView = (TextView) findViewById(R.id.result_updated);
        if (result.isEmpty()) {
            Log.d(TAG, "is empty");
            emptyResultView.setVisibility(View.VISIBLE);
            return;
        }
        emptyResultView.setVisibility(View.GONE);
        
        //Time time = new Time();
        //time.setToNow();
        //resultUpdatedView.setVisibility(View.VISIBLE);
        //resultUpdatedView.setText("Updated at " + time.format("%H:%M"));

        //DepartureFilter filter = new DepartureFilter("BUS", "47");
        //DepartureFilter filter = null;

        mSectionedAdapter.clear();
        for (Entry<String, DepartureList> entry : result.entrySet()) {
            DepartureList departureList = entry.getValue();
            if (!departureList.isEmpty()) {
                mSectionedAdapter.addSection(0,
                        transportModeToString(entry.getKey()),
                        createAdapter(departureList));
            }
        }

        mDepartureResult = result;

        setTitle(getString(R.string.departures_for, mSite.getName()));

        mSectionedAdapter.notifyDataSetChanged();
        setListAdapter(mSectionedAdapter);
    }

    private SimpleAdapter createAdapter(DepartureList departureList) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        Time now = new Time();
        now.setToNow();
        for (Departure departure : departureList) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("line", departure.getLineNumber());
            map.put("destination", departure.getDestination());
            //map.put("timeSlDisplay", departure.getDisplayTime());
            //map.put("timeTabled", departure.getTimeTabledDateTime().format("%H:%M"));
            //map.put("timeExpected", departure.getExpectedDateTime().format("%H:%M"));
            map.put("timeToDisplay", humanTimeUntil(now, departure.getExpectedDateTime()));
            map.put("groupOfLine", departure.getGroupOfLine());
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, 
                R.layout.departures_row,
                new String[] { "line", "destination", /*"timeSlDisplay", "timeTabled", "timeExpected",*/ "timeToDisplay", "groupOfLine"},
                new int[] { 
                    R.id.departure_line,
                    R.id.departure_destination,
                    //R.id.departure_timeSlDisplayTime,
                    //R.id.departure_timeTabled,
                    //R.id.departure_timeExpected,
                    R.id.departure_timeToDisplay,
                    R.id.departure_color
                }
        );

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                switch (view.getId()) {
                case R.id.departure_line:
                case R.id.departure_destination:
                //case R.id.departure_timeTabled:
                //case R.id.departure_timeSlDisplayTime:
                //case R.id.departure_timeExpected:
                case R.id.departure_timeToDisplay:
                    ((TextView)view).setText(textRepresentation);
                    return true;
                case R.id.departure_color:
                    if (!TextUtils.isEmpty(textRepresentation)) {
                        view.setBackgroundColor(
                                groupOfLineToColor(textRepresentation));
                    } else {
                        view.setVisibility(View.GONE);
                    }
                    return true;
                }
                return false;
            }
        });

        return adapter;
    }

    private int groupOfLineToColor(String groupOfLine) {
        if ("tunnelbanans gröna linje".equals(groupOfLine)) {
            return 0xFF228B22;
        } else if ("tunnelbanans röda linje".equals(groupOfLine)) {
            return 0xFFEE4000;
        } else if ("tunnelbanans blå linje".equals(groupOfLine)) {
            return 0xFF104E8B;
        }
        return Color.GRAY;
    }

    private String transportModeToString(String transport) {
        if ("METROS".equals(transport)) {
            return getString(R.string.metros);
        } else if ("BUSES".equals(transport)) {
            return getString(R.string.buses);
        } else if ("TRAINS".equals(transport)) {
            return getString(R.string.trains);
        } else if ("TRAMS".equals(transport)) {
            return getString(R.string.trams);
        }
        throw new IllegalArgumentException("Unknown transport");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_departures, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                new GetDeparturesTask().execute(mSite);
                return true;
            case R.id.menu_journey_planner:
                Intent i = new Intent(this, StartActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    private String humanTimeUntil(Time start, Time end) {
        long distanceInMillis = Math.round(end.toMillis(true) - start.toMillis(true));
        long distanceInSeconds = Math.round(distanceInMillis / 1000);
        long distanceInMinutes = Math.round(distanceInSeconds / 60);

        if (distanceInMinutes <= 0.0) {
            return getString(R.string.now);
        }
        return String.format("%s min", distanceInMinutes);
    }
    
    /**
     * Show progress dialog.
     */
    private void showProgress() {
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage(getText(R.string.loading));
            mProgress.show();   
        }
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        if (mProgress != null) {
            try {
                mProgress.dismiss();
            } catch (Exception e) {
                Log.d(TAG, "Could not dismiss progress; " + e.getMessage());
            }
            mProgress = null;
        }
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

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Site> doInBackground(String... params) {
            try {
                return SitesStore.getInstance().getSite(params[0]);
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
                    mSiteAlternatives = result;
                    showDialog(DIALOG_SITE_ALTERNATIVES);
                }
            } else if (!mWasSuccess) {
                showDialog(DIALOG_GET_SITES_NETWORK_PROBLEM);
            } else {
            //    onNoRoutesDetailsResult();
            }
        }
    }

    /**
     * Background job for getting {@link Departure}s.
     */
    private class GetDeparturesTask extends AsyncTask<Site, Void, HashMap<String,DepartureList>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected HashMap<String,DepartureList> doInBackground(Site... params) {
            try {
                mSite = params[0];
                DeparturesStore departures = new DeparturesStore();
                return departures.find(params[0], null);
            } catch (IllegalArgumentException e) {
                mWasSuccess = false;
                return null;
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(HashMap<String,DepartureList> result) {
            dismissProgress();

            if (mWasSuccess) {
                fillData(result);
            } else {
                showDialog(DIALOG_GET_DEPARTURES_NETWORK_PROBLEM);
            }
        }
    }
}
