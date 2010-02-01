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
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Route;
import com.markupartist.sthlmtraveling.provider.planner.Stop;

public class RouteDetailActivity extends ListActivity {
    public static final String TAG = "RouteDetailActivity";
    public static final String EXTRA_START_POINT = "com.markupartist.sthlmtraveling.start_point";
    public static final String EXTRA_END_POINT = "com.markupartist.sthlmtraveling.end_point";
    public static final String EXTRA_ROUTE = "com.markupartist.sthlmtraveling.route";
    private static final String STATE_GET_DETAILS_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getdetails.inprogress";
    private static final String STATE_ROUTE = "com.markupartist.sthlmtraveling.route";
    private static final int DIALOG_NETWORK_PROBLEM = 0;

    private ArrayAdapter<String> mDetailAdapter;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private ArrayList<String> mDetails;
    private GetDetailsTask mGetDetailsTask;
    private Route mRoute;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Bundle extras = getIntent().getExtras();
        mRoute = extras.getParcelable(EXTRA_ROUTE);
        Stop startPoint = extras.getParcelable(EXTRA_START_POINT);
        Stop endPoint = extras.getParcelable(EXTRA_END_POINT);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        TextView startPointView = (TextView) findViewById(R.id.route_from);
        startPointView.setText(startPoint.getName());
        TextView endPointView = (TextView) findViewById(R.id.route_to);
        endPointView.setText(endPoint.getName());

        if (startPoint.isMyLocation()) {
            startPointView.setText(getMyLocationString(startPoint));
        }
        if (endPoint.isMyLocation()) {
            endPointView.setText(getMyLocationString(endPoint));
        }
        
        TextView dateTimeView = (TextView) findViewById(R.id.route_date_time);
        dateTimeView.setText(mRoute.toString());

        FavoriteButtonHelper favoriteButtonHelper = new FavoriteButtonHelper(
                this, mFavoritesDbAdapter, startPoint, endPoint);
        favoriteButtonHelper.loadImage();

        initRouteDetails(mRoute);
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param stop the stop
     * @return a text representation of my location
     */
    private CharSequence getMyLocationString(Stop stop) {
        CharSequence string = getText(R.string.my_location);
        if (stop.getLocation() != null) {
            string = String.format("%s (%sm)", string, stop.getLocation().getAccuracy());
        }
        return string;
    }

    /**
     * Find route details. Will first check if we already have data stored. 
     * @param route
     */
    private void initRouteDetails(Route route) {
        @SuppressWarnings("unchecked")
        final ArrayList<String> details = (ArrayList<String>) getLastNonConfigurationInstance();
        if (details != null) {
            onRouteDetailsResult(details);
        } else if (mGetDetailsTask == null) {
            mGetDetailsTask = new GetDetailsTask();
            mGetDetailsTask.execute(route);
        }
    }

    /**
     * Called before this activity is destroyed, returns the previous details. This data is used 
     * if the screen is rotated. Then we don't need to ask for the data again.
     * @return route details
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDetails;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveGetDetailsTask(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
    }

    /**
     * Restores any local state, if any.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        restoreGetDetailsTask(savedInstanceState);
    }

    /**
     * Restores the search routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreGetDetailsTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_DETAILS_IN_PROGRESS)) {
            mRoute = savedInstanceState.getParcelable(STATE_ROUTE);
            Log.d(TAG, "restoring getDetailsTask");
            mGetDetailsTask = new GetDetailsTask();
            mGetDetailsTask.execute(mRoute);
        }
    }

    /**
     *
     * @param outState
     */
    private void saveGetDetailsTask(Bundle outState) {
        final GetDetailsTask task = mGetDetailsTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving SearchRoutesTask");
            task.cancel(true);
            mGetDetailsTask = null;
            outState.putBoolean(STATE_GET_DETAILS_IN_PROGRESS, true);
            outState.putParcelable(STATE_ROUTE, mRoute);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_route_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_search :
                Intent i = new Intent(this, StartActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
            case R.id.menu_departures_for_start:
                Intent departuresIntent = new Intent(this, DeparturesActivity.class);
                departuresIntent.putExtra(DeparturesActivity.EXTRA_SITE_NAME,
                        mRoute.from);
                startActivity(departuresIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        mFavoritesDbAdapter.close();
    }

    /**
     * Called when there is results to display.
     * @param details the route details
     */
    public void onRouteDetailsResult(ArrayList<String> details) {
        mDetailAdapter = new ArrayAdapter<String>(this, R.layout.route_details_row, details);
        setListAdapter(mDetailAdapter);
        mDetails = details;
    }

    /**
     * Called when there is no result returned. 
     */
    public void onNoRoutesDetailsResult() {
        TextView noResult = (TextView) findViewById(R.id.route_details_no_result);
        noResult.setVisibility(View.VISIBLE);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_NETWORK_PROBLEM:
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.network_problem_label))
                .setMessage(getText(R.string.network_problem_message))
                .setPositiveButton(getText(R.string.retry), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mGetDetailsTask = new GetDetailsTask();
                        mGetDetailsTask.execute(mRoute);
                    }
                })
                .setNegativeButton(getText(android.R.string.cancel), null)
                .create();
        }
        return null;
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
            mProgress.dismiss();
            mProgress = null;
        }
    }

    /**
     * Background task for fetching route details.
     */
    private class GetDetailsTask extends AsyncTask<Route, Void, ArrayList<String>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<String> doInBackground(Route... params) {
            try {
                String language = getApplicationContext()
                    .getResources()
                    .getConfiguration()
                    .locale.getLanguage();
                return Planner.getInstance().findRouteDetails(params[0], language);
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                onRouteDetailsResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_NETWORK_PROBLEM);
            } else {
                onNoRoutesDetailsResult();
            }
        }
    }
}
