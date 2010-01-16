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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SimpleAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.MyLocationManager.MyLocationFoundListener;
import com.markupartist.sthlmtraveling.SectionedAdapter.Section;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Route;
import com.markupartist.sthlmtraveling.provider.planner.Stop;
import com.markupartist.sthlmtraveling.utils.BarcodeScannerIntegrator;

/**
 * Routes activity
 * 
 * Accepts a routes data URI in the format:
 * 
 * <pre>
 * <code>journeyplanner://routes?start_point=STARTPOINT&end_point=ENDPOINT&time=TIME</code>
 * </pre>
 * 
 * All parameters needs to be url encoded. Time is optional, but if provided it must be in
 * RFC 2445 format.
 */
public class RoutesActivity extends ListActivity
        implements MyLocationFoundListener {
    /**
     * The start point for the search.
     */
    static final String EXTRA_START_POINT =
        "com.markupartist.sthlmtraveling.start_point";
    /**
     * The end point for the search.
     */
    static final String EXTRA_END_POINT =
        "com.markupartist.sthlmtraveling.end_point";
    /**
     * Departure time in RFC 2445 format.
     */
    static final String EXTRA_TIME = "com.markupartist.sthlmtraveling.time";

    /**
     * Indicates if the time is the departure or arrival time. 
     */
    static final String EXTRA_IS_TIME_DEPARTURE =
        "com.markupartist.sthlmtraveling.is_time_departure";

    private final String TAG = "RoutesActivity";

    private static final int DIALOG_ILLEGAL_PARAMETERS = 0;
    private static final int DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM = 1;
    private static final int DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM = 2;
    private static final int DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM = 3;
    private static final int DIALOG_GET_ROUTES_SESSION_TIMEOUT = 4;
    private static final int DIALOG_SEARCH_ROUTES_NO_RESULT = 5;

    private static final int ADAPTER_EARLIER = 0;
    private static final int ADAPTER_ROUTES = 1;
    private static final int ADAPTER_LATER = 2;

    private final int SECTION_CHANGE_TIME = 1;
    private final int SECTION_ROUTES = 2;

    protected static final int REQUEST_CODE_CHANGE_TIME = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 1;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 2;

    /**
     * Key to identify if the instance of SearchRoutesTask is in progress. 
     */
    private static final String STATE_SEARCH_ROUTES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.searchroutes.inprogress";
    private static final String STATE_GET_EARLIER_ROUTES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getearlierroutes.inprogress";
    private static final String STATE_GET_LATER_ROUTES_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getlaterroutes.inprogress";

    private RoutesAdapter mRouteAdapter;
    private MultipleListAdapter mMultipleListAdapter;
    private TextView mFromView;
    private TextView mToView;
    private ArrayList<HashMap<String, String>> mDateAdapterData;
    private Stop mStartPoint;
    private Stop mEndPoint;
    private Time mTime;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private FavoriteButtonHelper mFavoriteButtonHelper;
    private MyLocationManager mMyLocationManager;
    private SearchRoutesTask mSearchRoutesTask;
    private GetEarlierRoutesTask mGetEarlierRoutesTask;
    private GetLaterRoutesTask mGetLaterRoutesTask;
    private Toast mToast;
    private ProgressDialog mProgress;
    private boolean mIsTimeDeparture;

    //private Bundle mSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        LocationManager locationManager =
            (LocationManager)getSystemService(LOCATION_SERVICE);
        mMyLocationManager = new MyLocationManager(locationManager);

        mStartPoint = new Stop();
        mEndPoint = new Stop();

        // Parse data URI       
        final Uri uri = getIntent().getData();

        mStartPoint.setName(uri.getQueryParameter("start_point"));
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))) {
            double startPointLat = Double.parseDouble(uri.getQueryParameter("start_point_lat"));
            double startPointLng = Double.parseDouble(uri.getQueryParameter("start_point_lng"));
            if (startPointLat != 0 && startPointLng != 0) {
                Location startLocation = new Location("sthlmtraveling");
                startLocation.setLatitude(startPointLat);
                startLocation.setLongitude(startPointLng);
                mStartPoint.setLocation(startLocation);
            }
        }

        mEndPoint.setName(uri.getQueryParameter("end_point"));
        if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("end_point_lng"))) {
            double endPointLat = Double.parseDouble(uri.getQueryParameter("end_point_lat"));
            double endPointLng = Double.parseDouble(uri.getQueryParameter("end_point_lng"));
            if (endPointLat != 0 && endPointLng != 0) {
                Location endLocation = new Location("sthlmtraveling");
                endLocation.setLatitude(endPointLat);
                endLocation.setLongitude(endPointLng);
                mEndPoint.setLocation(endLocation);
            }
        }

        if (!TextUtils.isEmpty(uri.getQueryParameter("isTimeDeparture"))) {
            mIsTimeDeparture = Boolean.parseBoolean(
                    uri.getQueryParameter("isTimeDeparture"));
        } else {
            mIsTimeDeparture = true;
        }

        String time = uri.getQueryParameter("time");

        if (mStartPoint.getName() == null || mEndPoint.getName() == null) {
            showDialog(DIALOG_ILLEGAL_PARAMETERS);
            // If passed with bad parameters, break the execution.
            return;
        }

        mTime = new Time();
        if (!TextUtils.isEmpty(time)) {
            mTime.parse(time);
        } else {
            mTime.setToNow();
        }

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);
        updateStartAndEndPointViews(mStartPoint, mEndPoint);

        mFavoriteButtonHelper = new FavoriteButtonHelper(this, mFavoritesDbAdapter, 
                mStartPoint, mEndPoint);
        mFavoriteButtonHelper.loadImage();

        initRoutes(mStartPoint, mEndPoint, mTime);
    }

    /**
     * Update the {@link TextView} for start and end points in the ui.
     * @param startPoint the start point
     * @param endPoint the end point
     */
    private void updateStartAndEndPointViews(Stop startPoint, Stop endPoint) {
        if (startPoint.isMyLocation()) {
            mFromView.setText(getMyLocationString(startPoint));
        } else {
            mFromView.setText(startPoint.getName());
        }
        if (endPoint.isMyLocation()) {
            mToView.setText(getMyLocationString(endPoint));
        } else {
            mToView.setText(endPoint.getName());
        }
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param stop the stop
     * @return a text representation of my location
     */
    private CharSequence getMyLocationString(Stop stop) {
        CharSequence string = getText(R.string.my_location);
        Location location = stop.getLocation(); 
        if (location != null) {
            string = String.format("%s (%sm)", string, location.getAccuracy());
        }
        return string;
    }

    /**
     * Search for routes. Will first check if we already have data stored.
     * @param startPoint the start point
     * @param endPoint the end point
     * @param time the time
     */
    private void initRoutes(Stop startPoint, Stop endPoint, Time time) {
        @SuppressWarnings("unchecked")
        final ArrayList<Route> routes = (ArrayList<Route>) getLastNonConfigurationInstance();
        if (routes != null) {
            onSearchRoutesResult(routes);
        } else {
            if (startPoint.isMyLocation() || endPoint.isMyLocation()) {
                Location location = mMyLocationManager.getLastKnownLocation();
                if (mMyLocationManager.shouldAcceptLocation(location)) {
                    onMyLocationFound(location);
                } else {
                    mMyLocationManager.requestLocationUpdates(this);
                    mToast = Toast.makeText(this, "Determining your position", Toast.LENGTH_LONG);
                    mToast.show();
                }
            } else {
                mSearchRoutesTask = new SearchRoutesTask();
                //mSearchRoutesTask.setOnSearchRoutesResultListener(this);
                mSearchRoutesTask.execute(startPoint, endPoint, time, mIsTimeDeparture);
            }
        }
    }

    /**
     * Called before this activity is destroyed, returns the previous details. This data is used 
     * if the screen is rotated. Then we don't need to ask for the data again.
     * @return route details
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mRouteAdapter != null) {
            return mRouteAdapter.getRoutes();
        }
        return null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        //mSavedState = null;
    }

    /**
     * Restores the local state.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        restoreSearchCriteria(savedInstanceState);
        restoreSearchRoutesTask(savedInstanceState);
        restoreGetEarlierRoutesTask(savedInstanceState);
        restoreGetLaterRoutesTask(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveSearchCriteria(outState);
        saveSearchRoutesTask(outState);
        saveGetEarlierRoutesTask(outState);
        saveGetLaterRoutesTask(outState);
        //mSavedState = outState;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Could be null if bad parameters was passed to the search.
        if (mFavoriteButtonHelper != null) {
            mFavoriteButtonHelper.loadImage();
        }
        //if (mSavedState != null) restoreLocalState(mSavedState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFavoritesDbAdapter != null) {
            mFavoritesDbAdapter.close();
        }
        mMyLocationManager.removeUpdates();
        dismissProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMyLocationManager.removeUpdates();
    }

    /**
     * Restores the search routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreSearchCriteria(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRA_START_POINT)) {
            mStartPoint = savedInstanceState.getParcelable(EXTRA_START_POINT);
        }
        if (savedInstanceState.containsKey(EXTRA_END_POINT)) {
            mEndPoint = savedInstanceState.getParcelable(EXTRA_END_POINT);
        }
        if (savedInstanceState.containsKey(EXTRA_END_POINT)) {
            mIsTimeDeparture =
                savedInstanceState.getBoolean(EXTRA_IS_TIME_DEPARTURE);
        } else {
            mIsTimeDeparture = true;
        }
    }

    /**
     * If there is any running search for routes, save it and process it later 
     * on.
     * @param outState the out state
     */
    private void saveSearchCriteria(Bundle outState) {
        outState.putParcelable(EXTRA_START_POINT, mStartPoint);
        outState.putParcelable(EXTRA_END_POINT, mEndPoint);
        outState.putBoolean(EXTRA_IS_TIME_DEPARTURE, mIsTimeDeparture);
    }

    /**
     * Restores the search routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreSearchRoutesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_SEARCH_ROUTES_IN_PROGRESS)) {
            Log.d(TAG, "restoring SearchRoutesTask");
            mSearchRoutesTask = new SearchRoutesTask();
            mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);
        }
    }

    /**
     * If there is any running search for routes, save it and process it later 
     * on.
     * @param outState the out state
     */
    private void saveSearchRoutesTask(Bundle outState) {
        final SearchRoutesTask task = mSearchRoutesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving SearchRoutesTask");
            task.cancel(true);
            mSearchRoutesTask = null;
            outState.putBoolean(STATE_SEARCH_ROUTES_IN_PROGRESS, true);
        }
    }

    /**
     * Restores the task for getting earlier routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreGetEarlierRoutesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_EARLIER_ROUTES_IN_PROGRESS)) {
            Log.d(TAG, "restoring GetEarlierRoutesTask");
            mGetEarlierRoutesTask = new GetEarlierRoutesTask();
            mGetEarlierRoutesTask.execute();
        }
    }

    /**
     * Save the state for the task for getting earlier routes.
     * @param outState the out state
     */
    private void saveGetEarlierRoutesTask(Bundle outState) {
        final GetEarlierRoutesTask task = mGetEarlierRoutesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetEarlierRoutesTas");
            task.cancel(true);
            mGetEarlierRoutesTask = null;
            outState.putBoolean(STATE_GET_EARLIER_ROUTES_IN_PROGRESS, true);
        }
    }

    /**
     * Restores the task for getting earlier routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreGetLaterRoutesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_LATER_ROUTES_IN_PROGRESS)) {
            Log.d(TAG, "restoring GetLaterRoutesTask");
            mGetLaterRoutesTask = new GetLaterRoutesTask();
            mGetLaterRoutesTask.execute();
        }
    }

    /**
     * Save the state for the task for getting earlier routes.
     * @param outState the out state
     */
    private void saveGetLaterRoutesTask(Bundle outState) {
        final GetLaterRoutesTask task = mGetLaterRoutesTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetLaterRoutesTas");
            task.cancel(true);
            mGetLaterRoutesTask = null;
            outState.putBoolean(STATE_GET_LATER_ROUTES_IN_PROGRESS, true);
        }
    }

    private String buildDateString() {
        String timeString = mTime.format("%R");
        String dateString = mTime.format("%e/%m");

        if (mIsTimeDeparture) {
            return getString(R.string.departing_on, timeString, dateString);
        } else {
            return getString(R.string.arriving_by, timeString, dateString);
        }
    }

    private void createSections() {
        // Date and time adapter.
        mDateAdapterData = new ArrayList<HashMap<String,String>>(1); 
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("title", buildDateString());
        mDateAdapterData.add(item);
        SimpleAdapter dateTimeAdapter = new SimpleAdapter(
                this,
                mDateAdapterData,
                R.layout.date_and_time,
                new String[] { "title" },
                new int[] { R.id.date_time } );

        // Earlier routes
        SimpleAdapter earlierAdapter = createEarlierLaterAdapter(android.R.drawable.arrow_up_float);

        // Later routes
        SimpleAdapter laterAdapter = createEarlierLaterAdapter(android.R.drawable.arrow_down_float);

        mMultipleListAdapter = new MultipleListAdapter();
        mMultipleListAdapter.addAdapter(ADAPTER_EARLIER, earlierAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_ROUTES, mRouteAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_LATER, laterAdapter);

        mSectionedAdapter.addSection(SECTION_CHANGE_TIME, "Date & Time", dateTimeAdapter);
        mSectionedAdapter.addSection(SECTION_ROUTES, "Routes", mMultipleListAdapter);

        setListAdapter(mSectionedAdapter);
    }

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

    /**
     * Helper to create earlier or later adapter.
     * @param resource the image resource to show in the list
     * @return a prepared adapter
     */
    private SimpleAdapter createEarlierLaterAdapter(int resource) {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("image", resource);
        list.add(map);

        SimpleAdapter adapter = new SimpleAdapter(this, list, 
                R.layout.earlier_later_routes_row,
                new String[] { "image"},
                new int[] { 
                    R.id.earlier_later,
                }
        );

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                switch (view.getId()) {
                case R.id.earlier_later:
                    ImageView imageView = (ImageView) view;
                    imageView.setImageResource((Integer) data);
                    return true;
                }
                return false;
            }
        });
        return adapter;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Section section = mSectionedAdapter.getSection(position);
        int sectionId = section.id;
        int innerPosition = mSectionedAdapter.getSectionIndex(position);
        Adapter adapter = section.adapter;

        switch (sectionId) {
        case SECTION_ROUTES:
            MultipleListAdapter multipleListAdapter = (MultipleListAdapter) adapter;
            int adapterId = multipleListAdapter.getAdapterId(innerPosition);
            switch(adapterId) {
            case ADAPTER_EARLIER:
                mGetEarlierRoutesTask = new GetEarlierRoutesTask();
                mGetEarlierRoutesTask.execute();
                break;
            case ADAPTER_LATER:
                mGetLaterRoutesTask = new GetLaterRoutesTask();
                mGetLaterRoutesTask.execute();
                break;
            case ADAPTER_ROUTES:
                Route route = (Route) mSectionedAdapter.getItem(position);
                findRouteDetails(route);
                break;
            }
            break;
        case SECTION_CHANGE_TIME:
            Intent i = new Intent(this, ChangeRouteTimeActivity.class);
            i.putExtra(EXTRA_TIME, mTime.format2445());
            i.putExtra(EXTRA_START_POINT, mStartPoint);
            i.putExtra(EXTRA_END_POINT, mEndPoint);
            i.putExtra(EXTRA_IS_TIME_DEPARTURE, mIsTimeDeparture);
            startActivityForResult(i, REQUEST_CODE_CHANGE_TIME);
            break;
        }
    }

    public void onSearchRoutesResult(ArrayList<Route> routes) { 
        if (mRouteAdapter == null) {
            mRouteAdapter = new RoutesAdapter(this, routes);
            createSections();
        } else {
            mRouteAdapter.refill(routes);
            mSectionedAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMyLocationFound(Location location) {
        Log.d(TAG, "onMyLocationFound: " + location);

        mMyLocationManager.removeUpdates();

        if (mToast != null ) mToast.cancel();

        if (mStartPoint.isMyLocation()) {
            mStartPoint.setLocation(location);
            if (!mMyLocationManager.shouldAcceptLocation(location)) {
                Intent i = new Intent(this, PointOnMapActivity.class);
                i.putExtra(PointOnMapActivity.EXTRA_STOP, mStartPoint);
                i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                        getString(R.string.tap_your_location_on_map));
                startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_START);
            }
        }
        if (mEndPoint.isMyLocation()) {
            mEndPoint.setLocation(location);
            if (!mMyLocationManager.shouldAcceptLocation(location)) {
                Intent i = new Intent(this, PointOnMapActivity.class);
                i.putExtra(PointOnMapActivity.EXTRA_STOP, mStartPoint);
                i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                        getString(R.string.tap_your_location_on_map));
                startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_START);
            }
        }

        updateStartAndEndPointViews(mStartPoint, mEndPoint);

        mSearchRoutesTask = new SearchRoutesTask();
        mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);
    }

    /**
     * Find route details. Will start {@link RouteDetailActivity}. 
     * @param route the route to find details for 
     */
    private void findRouteDetails(final Route route) {
        Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
        i.putExtra(RouteDetailActivity.EXTRA_START_POINT, mStartPoint);
        i.putExtra(RouteDetailActivity.EXTRA_END_POINT, mEndPoint);
        i.putExtra(RouteDetailActivity.EXTRA_ROUTE, route);
        startActivity(i);
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                Intent data) {
        switch (requestCode) {
        case REQUEST_CODE_CHANGE_TIME:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Change time activity cancelled.");
            } else {
                Stop startPoint = data.getParcelableExtra(EXTRA_START_POINT);
                Stop endPoint = data.getParcelableExtra(EXTRA_END_POINT);
                String newTime = data.getStringExtra(EXTRA_TIME);
                mIsTimeDeparture = data.getBooleanExtra(EXTRA_IS_TIME_DEPARTURE, true);

                mTime.parse(newTime);
                HashMap<String, String> item = mDateAdapterData.get(0);
                item.put("title", buildDateString());

                //updateStartAndEndPointViews(startPoint, endPoint);

                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(startPoint, endPoint, mTime, mIsTimeDeparture);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_START:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mStartPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                Log.d(TAG, "Got Stop " + mStartPoint);
                mStartPoint.setName(Stop.TYPE_MY_LOCATION);
                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);
                updateStartAndEndPointViews(mStartPoint, mEndPoint);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_END:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mEndPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                Log.d(TAG, "Got Stop " + mEndPoint);
                mEndPoint.setName(Stop.TYPE_MY_LOCATION);
                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);
                updateStartAndEndPointViews(mStartPoint, mEndPoint);
            }
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_routes, menu);
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
            case R.id.reverse_start_end :
                Stop tmpStartPoint = new Stop(mEndPoint);
                Stop tmpEndPoint = new Stop(mStartPoint);

                mStartPoint = tmpStartPoint;
                mEndPoint = tmpEndPoint;

                /*
                 * Note: To launch a new intent won't work because sl.se would need to have a new
                 * ident generated to be able to search for route details in the next step.
                 */

                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);

                updateStartAndEndPointViews(mStartPoint, mEndPoint);

                // Update the favorite button
                mFavoriteButtonHelper
                        .setStartPoint(mStartPoint)
                        .setEndPoint(mEndPoint)
                        .loadImage();
                return true;
            case R.id.show_qr_code :
                Uri routesUri = createRoutesUri(mStartPoint, mEndPoint, null, true);
                BarcodeScannerIntegrator.shareText(this, routesUri.toString(),
                        R.string.install_barcode_scanner_title,
                        R.string.requires_barcode_scanner_message,
                        R.string.yes, R.string.no);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_ILLEGAL_PARAMETERS:
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.attention_label))
                .setMessage(getText(R.string.bad_routes_parameters_message))
                .setCancelable(true)
                .setNeutralButton(getText(android.R.string.ok), null)
                .create();
        case DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mSearchRoutesTask = new SearchRoutesTask();
                    mSearchRoutesTask.execute(mStartPoint, mEndPoint, mTime, mIsTimeDeparture);
                }
            });
        case DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetEarlierRoutesTask = new GetEarlierRoutesTask();
                    mGetEarlierRoutesTask.execute();
                }
            });
        case DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetLaterRoutesTask = new GetLaterRoutesTask();
                    mGetLaterRoutesTask.execute();
                }
            });
        case DIALOG_GET_ROUTES_SESSION_TIMEOUT:
            return new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getText(R.string.attention_label))
            .setMessage(getText(R.string.session_timeout_message))
            .setNeutralButton(getText(android.R.string.ok), null)
            .create();
        case DIALOG_SEARCH_ROUTES_NO_RESULT:
            return new AlertDialog.Builder(this)
            .setTitle(getText(R.string.no_routes_found_label))
            .setMessage(getText(R.string.no_routes_found_message))
            .setPositiveButton("Back", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton(getText(android.R.string.cancel), null)
            .create();
        }
        return null;
    }

    /**
     * Constructs a search routes data URI.
     * @param startPoint the start point
     * @param endPoint the end point
     * @param time the time, pass null for now
     * @param isTimeDeparture true if the time is departure time, false if arrival
     * @return the data uri
     */
    public static Uri createRoutesUri(Stop startPoint, Stop endPoint, Time time,
            boolean isTimeDeparture) {
        Uri routesUri;

        String timeString = "";
        String startLat = "";
        String startLng = "";
        String endLat = "";
        String endLng = "";

        if (time != null) {
            timeString = time.format2445();
        }
        if (startPoint.getLocation() != null) {
            startLat = String.valueOf(startPoint.getLocation().getLatitude());
            startLng = String.valueOf(startPoint.getLocation().getLongitude());
        }
        if (endPoint.getLocation() != null) {
            endLat = String.valueOf(endPoint.getLocation().getLatitude());
            endLng = String.valueOf(endPoint.getLocation().getLongitude());
        }

        routesUri = Uri.parse(
                    String.format("journeyplanner://routes?" 
                            + "start_point=%s"
                            + "&start_point_lat=%s"
                            + "&start_point_lng=%s"
                            + "&end_point=%s"
                            + "&end_point_lat=%s"
                            + "&end_point_lng=%s"
                            + "&time=%s"
                            + "&isTimeDeparture=%s",
                            Uri.encode(startPoint.getName()), startLat, startLng,
                            Uri.encode(endPoint.getName()), endLat, endLng, 
                            timeString, isTimeDeparture));

        return routesUri;
    }

    private class RoutesAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<Route> mRoutes;

        public RoutesAdapter(Context context, ArrayList<Route> routes) {
            mContext = context;
            mRoutes = routes;
        }

        public void refill(ArrayList<Route> routes) {
            mRoutes = routes;
        }

        public ArrayList<Route> getRoutes() {
            return mRoutes;
        }

        @Override
        public int getCount() {
            return mRoutes.size();
        }

        @Override
        public Object getItem(int position) {
            return mRoutes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Route route = mRoutes.get(position);
            return new RouteAdapterView(mContext, route);
            //return createView(mContext, route);
        }

        /*
        private View createView(Context context, Route route) {
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.routes_row, null);

            TextView startPoint = (TextView) layout.findViewById(R.id.route_startpoint_label);
            startPoint.setText(route.from);
            TextView startPointDeparture = (TextView) layout.findViewById(R.id.route_startpoint_departure);
            startPointDeparture.setText(route.departure);

            TextView endPoint = (TextView) layout.findViewById(R.id.route_endpoint_label);
            endPoint.setText(route.to);
            TextView endPointArrival = (TextView) layout.findViewById(R.id.route_endpoint_arrival);
            endPointArrival.setText(route.arrival);

            TextView durationAndChanges = (TextView) layout.findViewById(R.id.route_duration_and_changes);
            durationAndChanges.setText(route.duration);

            LinearLayout routeChangesDrawables = (LinearLayout) findViewById(R.id.route_changes);
            int currentTransportCount = 1;
            int transportCount = route.transports.size();
            for (Route.Transport transport : route.transports) {
                ImageView change = new ImageView(context);
                change.setImageResource(transport.imageResource());
                change.setPadding(0, 0, 5, 0);
                routeChangesDrawables.addView(change);

                if (transportCount > currentTransportCount) {
                    ImageView separator = new ImageView(context);
                    separator.setImageResource(R.drawable.transport_separator);
                    separator.setPadding(0, 5, 5, 0);
                    routeChangesDrawables.addView(separator);
                }

                currentTransportCount++;
            }
            
            return layout;
        }
        */
    }

    private class RouteAdapterView extends LinearLayout {

        public RouteAdapterView(Context context, Route route) {
            super(context);
            this.setOrientation(VERTICAL);

            this.setPadding(10, 10, 10, 10);

            TextView routeDetail = new TextView(context);
            routeDetail.setText(route.toString());
            routeDetail.setTextColor(Color.WHITE);
            //routeDetail.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

            TextView startAndEndPoint = new TextView(context);
            startAndEndPoint.setText(route.from + " - " + route.to);
            startAndEndPoint.setTextColor(Color.GRAY);
            startAndEndPoint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);

            LinearLayout routeChanges = new LinearLayout(context);
            routeChanges.setPadding(0, 5, 0, 0);

            int currentTransportCount = 1;
            int transportCount = route.transports.size();
            for (Route.Transport transport : route.transports) {
                ImageView change = new ImageView(context);
                change.setImageResource(transport.imageResource());
                change.setPadding(0, 0, 5, 0);
                routeChanges.addView(change);

                if (transport.lineNumber() > 0) {
                    TextView lineNumberView = new TextView(context);
                    lineNumberView.setTextColor(Color.GRAY);
                    lineNumberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                    lineNumberView.setText(String.valueOf(transport.lineNumber()));
                    lineNumberView.setPadding(0, 2, 5, 0);
                    routeChanges.addView(lineNumberView);
                }
                
                if (transportCount > currentTransportCount) {
                    ImageView separator = new ImageView(context);
                    separator.setImageResource(R.drawable.transport_separator);
                    separator.setPadding(0, 5, 5, 0);
                    routeChanges.addView(separator);
                }

                currentTransportCount++;
            }

            this.addView(routeDetail);
            this.addView(startAndEndPoint);
            this.addView(routeChanges);
        }
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
     * Background task for searching for routes.
     */
    private class SearchRoutesTask extends AsyncTask<Object, Void, ArrayList<Route>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Route> doInBackground(Object... params) {
            try {
                return Planner.getInstance().findRoutes(
                        (Stop) params[0], (Stop) params[1], (Time) params[2],
                        (Boolean) params[3]);
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Route> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM);
            } else {
                showDialog(DIALOG_SEARCH_ROUTES_NO_RESULT);
            }
        }
    }

    /**
     * Background task for getting earlier routes.
     */
    private class GetEarlierRoutesTask extends AsyncTask<Void, Void, ArrayList<Route>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Route> doInBackground(Void... params) {
            try {
                return Planner.getInstance().findEarlierRoutes();
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Route> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM);
            } else {
                showDialog(DIALOG_GET_ROUTES_SESSION_TIMEOUT);
            }
        }
    }

    /**
     * Background task for getting later routes.
     */
    private class GetLaterRoutesTask extends AsyncTask<Void, Void, ArrayList<Route>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Route> doInBackground(Void... params) {
            try {
                return Planner.getInstance().findLaterRoutes();
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Route> result) {
            dismissProgress();

            if (result != null && !result.isEmpty()) {
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM);
            } else {
                showDialog(DIALOG_GET_ROUTES_SESSION_TIMEOUT);
            }
        }
    }
}
