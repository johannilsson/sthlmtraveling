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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.MyLocationManager.MyLocationFoundListener;
import com.markupartist.sthlmtraveling.SectionedAdapter.Section;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Response;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.provider.planner.Route;
import com.markupartist.sthlmtraveling.provider.planner.Stop;
import com.markupartist.sthlmtraveling.provider.planner.Trip;

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
     * The Journey
     */
    static final String EXTRA_JOURNEY =
        "sthlmtraveling.intent.action.JOURNEY";

    /**
     * The Journey
     */
    static final String EXTRA_JOURNEY_QUERY =
        "sthlmtraveling.intent.action.JOURNEY_QUERY";

    /**
     * The trip.
     */
    @Deprecated
    static final String EXTRA_TRIP = "com.markupartist.sthlmtraveling.trip";

    /**
     * The start point for the search.
     */
    @Deprecated
    static final String EXTRA_START_POINT =
        "com.markupartist.sthlmtraveling.start_point";
    /**
     * The end point for the search.
     */
    @Deprecated
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
    private static final int DIALOG_START_POINT_ALTERNATIVES = 6;
    private static final int DIALOG_END_POINT_ALTERNATIVES = 7;

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
    //private Stop mStartPoint;
    //private Stop mEndPoint;
    //private Time mTime;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private FavoriteButtonHelper mFavoriteButtonHelper;
    private MyLocationManager mMyLocationManager;
    private SearchRoutesTask mSearchRoutesTask;
    private GetEarlierRoutesTask mGetEarlierRoutesTask;
    private GetLaterRoutesTask mGetLaterRoutesTask;
    private Toast mToast;
    private ProgressDialog mProgress;
    //private boolean mIsTimeDeparture;
    //private Trip mTrip;
    private Response mPlannerResponse;
    private JourneyQuery mJourneyQuery;

    private Bundle mSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mMyLocationManager = new MyLocationManager(locationManager);

        // Parse data URI       
        mJourneyQuery = createQuery(getIntent().getData());

        Log.d(TAG, "dest: " + mJourneyQuery.destination.name);
        

        if (mJourneyQuery.origin.name == null
                || mJourneyQuery.destination.name == null) {
            showDialog(DIALOG_ILLEGAL_PARAMETERS);
            // If passed with bad parameters, break the execution.
            return;
        }

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);
        updateStartAndEndPointViews(mJourneyQuery.origin, mJourneyQuery.destination);

        mFavoriteButtonHelper = new FavoriteButtonHelper(this, mFavoritesDbAdapter, 
                mJourneyQuery.origin, mJourneyQuery.destination);
        mFavoriteButtonHelper.loadImage();

        initRoutes(mJourneyQuery);
    }

    private JourneyQuery createQuery(Uri uri) {
        JourneyQuery jq = new JourneyQuery();

        jq.origin = new Planner.Location();        
        jq.origin.name = uri.getQueryParameter("start_point");
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))) {
            jq.origin.latitude =
                (int) (Double.parseDouble(uri.getQueryParameter("start_point_lat")) * 1E6);
            jq.origin.longitude =
                (int) (Double.parseDouble(uri.getQueryParameter("start_point_lng")) * 1E6);
        }

        jq.destination = new Planner.Location();
        jq.destination.name = uri.getQueryParameter("end_point");
        if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("end_point_lng"))) {
            jq.destination.latitude =
                (int) (Double.parseDouble(uri.getQueryParameter("end_point_lat")) * 1E6);
            jq.destination.longitude =
                (int) (Double.parseDouble(uri.getQueryParameter("end_point_lng")) * 1E6);
        }

        jq.isTimeDeparture = true;
        if (!TextUtils.isEmpty(uri.getQueryParameter("isTimeDeparture"))) {
            jq.isTimeDeparture = Boolean.parseBoolean(
                    uri.getQueryParameter("isTimeDeparture"));
        }

        jq.time = new Time();
        String timeString = uri.getQueryParameter("time");
        if (!TextUtils.isEmpty(timeString)) {
            jq.time.parse(timeString);
        } else {
            jq.time.setToNow();
        }

        return jq;
    }
    
    /**
     * Update the {@link TextView} for start and end points in the ui.
     * @param startPoint the start point
     * @param endPoint the end point
     */
    private void updateStartAndEndPointViews(Planner.Location startPoint,
            Planner.Location endPoint) {
        if (startPoint.isMyLocation()) {
            mFromView.setText(getMyLocationString(startPoint));
        } else {
            mFromView.setText(startPoint.name);
        }
        if (endPoint.isMyLocation()) {
            mToView.setText(getMyLocationString(endPoint));
        } else {
            mToView.setText(endPoint.name);
        }
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param stop the stop
     * @return a text representation of my location
     */
    private CharSequence getMyLocationString(Planner.Location stop) {
        CharSequence string = getText(R.string.my_location);
        /*
        Location location = stop.getLocation(); 
        if (location != null) {
            string = String.format("%s (%sm)", string, location.getAccuracy());
        }
        */
        return string;
    }

    /**
     * Search for routes. Will first check if we already have data stored.
     * @param startPoint the start point
     * @param endPoint the end point
     * @param time the time
     */
    private void initRoutes(JourneyQuery journeyQuery) {
        final Planner.Response savedResponse = (Planner.Response) getLastNonConfigurationInstance();
        if (savedResponse != null) {
            onSearchRoutesResult(savedResponse);
        } else {
            if (journeyQuery.origin.isMyLocation()
                    || journeyQuery.destination.isMyLocation()) {
                Location location = mMyLocationManager.getLastKnownLocation();
                if (mMyLocationManager.shouldAcceptLocation(location)) {
                    onMyLocationFound(location);
                } else {
                    mMyLocationManager.requestLocationUpdates(this);
                    mToast = Toast.makeText(this, getText(R.string.determining_your_position), Toast.LENGTH_LONG);
                    mToast.show();
                }
            } else {
                mSearchRoutesTask = new SearchRoutesTask();
                //mSearchRoutesTask.setOnSearchRoutesResultListener(this);
                mSearchRoutesTask.execute(mJourneyQuery);
            }
        }
    }

    /**
     * Called before this activity is destroyed, returns the previous details.
     * This data is used if the screen is rotated. Then we don't need to ask for the data again.
     * @return the trip
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mPlannerResponse;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        mSavedState = null;
    }

    /**
     * Restores the local state.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        restoreJourneyQuery(savedInstanceState);
        restoreSearchRoutesTask(savedInstanceState);
        restoreGetEarlierRoutesTask(savedInstanceState);
        restoreGetLaterRoutesTask(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveJourneyQuery(outState);
        saveSearchRoutesTask(outState);
        saveGetEarlierRoutesTask(outState);
        saveGetLaterRoutesTask(outState);
        mSavedState = outState;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Could be null if bad parameters was passed to the search.
        if (mFavoriteButtonHelper != null) {
            mFavoriteButtonHelper.loadImage();
        }
        if (mSavedState != null) restoreLocalState(mSavedState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancelSearchRoutesTask();
        onCancelGetEarlierRoutesTask();
        onCancelGetLaterRoutesTask();

        if (mFavoritesDbAdapter != null) {
            mFavoritesDbAdapter.close();
        }
        mMyLocationManager.removeUpdates();
        dismissProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();

        onCancelSearchRoutesTask();
        onCancelGetEarlierRoutesTask();
        onCancelGetLaterRoutesTask();

        mMyLocationManager.removeUpdates();

        dismissProgress();
    }

    /**
     * Restores the search routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreJourneyQuery(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(EXTRA_JOURNEY_QUERY)) {
            mJourneyQuery = savedInstanceState.getParcelable(EXTRA_JOURNEY_QUERY);
        }
    }

    /**
     * If there is any running search for routes, save it and process it later 
     * on.
     * @param outState the out state
     */
    private void saveJourneyQuery(Bundle outState) {
        outState.putParcelable(EXTRA_JOURNEY_QUERY, mJourneyQuery);
    }

    /**
     * Cancels a search routes task if it is running.
     */
    private void onCancelSearchRoutesTask() {
        if (mSearchRoutesTask != null && mSearchRoutesTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels the search routes task.");
            mSearchRoutesTask.cancel(true);
            mSearchRoutesTask = null;
        }
    }
    
    /**
     * Restores the search routes task.
     * @param savedInstanceState the saved state
     */
    private void restoreSearchRoutesTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_SEARCH_ROUTES_IN_PROGRESS)) {
            Log.d(TAG, "restoring SearchRoutesTask");
            mSearchRoutesTask = new SearchRoutesTask();
            mSearchRoutesTask.execute(mJourneyQuery);
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
     * Cancel the get earlier routes task if it is running.
     */
    private void onCancelGetEarlierRoutesTask() {
        if (mGetEarlierRoutesTask != null &&
                mGetEarlierRoutesTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels the get earlier routes task.");
            mGetEarlierRoutesTask.cancel(true);
            mGetEarlierRoutesTask = null;
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
            mGetEarlierRoutesTask.execute(mJourneyQuery);
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
     * Cancel the get later routes task if it is running.
     */
    private void onCancelGetLaterRoutesTask() {
        if (mGetLaterRoutesTask != null &&
                mGetLaterRoutesTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels the get later routes task.");
            mGetLaterRoutesTask.cancel(true);
            mGetLaterRoutesTask = null;
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
            mGetLaterRoutesTask.execute(mJourneyQuery);
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
        String timeString = mJourneyQuery.time.format("%R");
        String dateString = mJourneyQuery.time.format("%e/%m");

        if (mJourneyQuery.isTimeDeparture) {
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
        SimpleAdapter earlierAdapter = createEarlierLaterAdapter(R.drawable.arrow_up_float);

        // Later routes
        SimpleAdapter laterAdapter = createEarlierLaterAdapter(R.drawable.arrow_down_float);

        mMultipleListAdapter = new MultipleListAdapter();
        mMultipleListAdapter.addAdapter(ADAPTER_EARLIER, earlierAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_ROUTES, mRouteAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_LATER, laterAdapter);

        mSectionedAdapter.addSection(SECTION_CHANGE_TIME, getString(R.string.date_and_time_label), dateTimeAdapter);
        mSectionedAdapter.addSection(SECTION_ROUTES, getString(R.string.route_alternatives_label), mMultipleListAdapter);

        setListAdapter(mSectionedAdapter);
    }

    SectionedAdapter mSectionedAdapter = new SectionedAdapter() {
        protected View getHeaderView(Section section, int index, View convertView, ViewGroup parent) {
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
                mGetEarlierRoutesTask.execute(mJourneyQuery);
                break;
            case ADAPTER_LATER:
                mGetLaterRoutesTask = new GetLaterRoutesTask();
                mGetLaterRoutesTask.execute(mJourneyQuery);
                break;
            case ADAPTER_ROUTES:
                Trip2 trip = (Trip2) mSectionedAdapter.getItem(position);
                findRouteDetails(trip);
                break;
            }
            break;
        case SECTION_CHANGE_TIME:
            Intent i = new Intent(this, ChangeRouteTimeActivity.class);
            i.putExtra(EXTRA_JOURNEY_QUERY, mJourneyQuery);
            startActivityForResult(i, REQUEST_CODE_CHANGE_TIME);
            break;
        }
    }

    public void onSearchRoutesResult(Planner.Response response) {
        mPlannerResponse = response;
        //mTrip = trip;
        //updateStartAndEndPointViews(trip.getStartPoint(), trip.getEndPoint());

        mJourneyQuery.ident = response.ident;
        mJourneyQuery.seqnr = response.seqnr;

        if (mRouteAdapter == null) {
            mRouteAdapter = new RoutesAdapter(this, response.trips);
            createSections();
        } else {
            mRouteAdapter.refill(response.trips);
            mSectionedAdapter.notifyDataSetChanged();
        }
    }

    public void onSiteAlternatives(Trip trip) {
        // TODO: Handle alternatives...
        /*
        if (trip.getStartPoint().getSiteId() == 0
                && trip.getStartPointAlternatives().size() > 1) {
            Log.d(TAG, "show start alternatives...");
            showDialog(DIALOG_START_POINT_ALTERNATIVES);
        } else if (trip.getEndPoint().getSiteId() == 0
                && trip.getEndPointAlternatives().size() > 1) {
            Log.d(TAG, "show end alternatives...");
            showDialog(DIALOG_END_POINT_ALTERNATIVES);
        } else {
            mSearchRoutesTask = new SearchRoutesTask();
            mSearchRoutesTask.execute(mTrip);
        }
        */
    }

    @Override
    public void onMyLocationFound(Location location) {
        Log.d(TAG, "onMyLocationFound: " + location);

        mMyLocationManager.removeUpdates();

        if (mToast != null ) mToast.cancel();

        Planner.Location startPoint = mJourneyQuery.origin;
        Planner.Location endPoint = mJourneyQuery.destination;

        Stop tmpStop = new Stop();
        tmpStop.setLocation(location);

        if (startPoint.isMyLocation()) {
            if (!mMyLocationManager.shouldAcceptLocation(location)) {
                Intent i = new Intent(this, PointOnMapActivity.class);
                i.putExtra(PointOnMapActivity.EXTRA_STOP, tmpStop);
                i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                        getString(R.string.tap_your_location_on_map));
                startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_START);
            } else {
                startPoint.latitude = (int) (location.getLatitude() * 1E6);
                startPoint.longitude = (int) (location.getLongitude() * 1E6);
            }
        }
        if (endPoint.isMyLocation()) {
            if (!mMyLocationManager.shouldAcceptLocation(location)) {
                Intent i = new Intent(this, PointOnMapActivity.class);
                i.putExtra(PointOnMapActivity.EXTRA_STOP, tmpStop);
                i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                        getString(R.string.tap_your_location_on_map));
                startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_END);
            } else {
                endPoint.latitude = (int) (location.getLatitude() * 1E6);
                endPoint.longitude = (int) (location.getLongitude() * 1E6);
            }
        }

        updateStartAndEndPointViews(startPoint, endPoint);

        // TODO: Maybe need to set start and end points to the trip again here?
        mSearchRoutesTask = new SearchRoutesTask();
        mSearchRoutesTask.execute(mJourneyQuery);
    }

    /**
     * Find route details. Will start {@link RouteDetailActivity}. 
     * @param route the route to find details for 
     */
    private void findRouteDetails(final Trip2 trip) {
        // TODO: Change to pass the trip later on instead.
        Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
        i.putExtra(RouteDetailActivity.EXTRA_JOURNEY_QUERY, mJourneyQuery);
        i.putExtra(RouteDetailActivity.EXTRA_JOURNEY_TRIP, trip);
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
                mJourneyQuery = data.getParcelableExtra(EXTRA_JOURNEY_QUERY);

                HashMap<String, String> item = mDateAdapterData.get(0);
                item.put("title", buildDateString());

                //updateStartAndEndPointViews(startPoint, endPoint);

                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mJourneyQuery);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_START:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
                finish();
                return;
            } else {
                Stop startPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                Log.d(TAG, "Got Stop " + startPoint);

                mJourneyQuery.origin.name = Planner.Location.TYPE_MY_LOCATION;
                mJourneyQuery.origin.latitude = (int) (startPoint.getLocation().getLatitude() * 1E6);
                mJourneyQuery.origin.longitude = (int) (startPoint.getLocation().getLongitude() * 1E6);

                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mJourneyQuery);

                // TODO: Is this call really needed?
                updateStartAndEndPointViews(mJourneyQuery.origin,
                        mJourneyQuery.destination);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_END:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
                finish();
                return;
            } else {
                Stop endPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                Log.d(TAG, "Got Stop " + endPoint);

                mJourneyQuery.destination.name = Planner.Location.TYPE_MY_LOCATION;
                mJourneyQuery.destination.latitude = (int) (endPoint.getLocation().getLatitude() * 1E6);
                mJourneyQuery.destination.longitude = (int) (endPoint.getLocation().getLongitude() * 1E6);

                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mJourneyQuery);

                // TODO: Is this call really needed?
                updateStartAndEndPointViews(mJourneyQuery.origin,
                        mJourneyQuery.destination);            }
            
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
                Planner.Location tmpStartPoint = new Planner.Location(mJourneyQuery.destination);
                Planner.Location tmpEndPoint = new Planner.Location(mJourneyQuery.origin);

                mJourneyQuery.origin = tmpStartPoint;
                mJourneyQuery.destination = tmpEndPoint;

                /*
                 * Note: To launch a new intent won't work because sl.se would
                 * need to have a new ident generated to be able to search for
                 * route details in the next step.
                 */
                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mJourneyQuery);

                updateStartAndEndPointViews(
                        mJourneyQuery.origin, mJourneyQuery.destination);

                // Update the favorite button
                mFavoriteButtonHelper
                        .setStartPoint(mJourneyQuery.origin)
                        .setEndPoint(mJourneyQuery.destination)
                        .loadImage();
                return true;
            case R.id.menu_share:
                if (mRouteAdapter != null) {
                    //share(mRouteAdapter.mTrips);
                }
                return true;
            /*
            case R.id.show_qr_code :
                Uri routesUri = createRoutesUri(
                        mTrip.getStartPoint(), mTrip.getEndPoint(), null, true);
                BarcodeScannerIntegrator.shareText(this, routesUri.toString(),
                        R.string.install_barcode_scanner_title,
                        R.string.requires_barcode_scanner_message,
                        R.string.yes, R.string.no);

                return true;
            */
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
                    mSearchRoutesTask.execute(mJourneyQuery);
                }
            });
        case DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetEarlierRoutesTask = new GetEarlierRoutesTask();
                    mGetEarlierRoutesTask.execute(mJourneyQuery);
                }
            });
        case DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetLaterRoutesTask = new GetLaterRoutesTask();
                    mGetLaterRoutesTask.execute(mJourneyQuery);
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
            .setPositiveButton(getText(R.string.back), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setNegativeButton(getText(R.string.cancel), null)
            .create();
        case DIALOG_START_POINT_ALTERNATIVES:
            /*
            ArrayAdapter<Site> startAlternativesAdapter =
                new ArrayAdapter<Site>(this, android.R.layout.simple_dropdown_item_1line,
                        mTrip.getStartPointAlternatives());
            return new AlertDialog.Builder(this)
                .setTitle(R.string.did_you_mean)
                .setAdapter(startAlternativesAdapter, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Site site = mTrip.getStartPointAlternatives().get(which);
                        Stop startPoint = mTrip.getStartPoint();
                        startPoint.setSiteId(site.getId());
                        startPoint.setName(site.getName());

                        onSiteAlternatives(mTrip);
                    }
                })
                .create();
            */
            break;
        case DIALOG_END_POINT_ALTERNATIVES:
            /*
            ArrayAdapter<Site> endAlternativesAdapter =
                new ArrayAdapter<Site>(this, android.R.layout.simple_dropdown_item_1line,
                        mTrip.getEndPointAlternatives());
            return new AlertDialog.Builder(this)
                .setTitle(R.string.did_you_mean)
                .setAdapter(endAlternativesAdapter, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Site site = mTrip.getEndPointAlternatives().get(which);
                        Stop endPoint = mTrip.getEndPoint();
                        endPoint.setSiteId(site.getId());
                        endPoint.setName(site.getName());

                        onSiteAlternatives(mTrip);
                    }
                })
                .create();
            */
            break;
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

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private class RoutesAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<Trip2> mTrips;

        public RoutesAdapter(Context context, ArrayList<Trip2> trips) {
            mContext = context;
            mTrips = trips;
        }

        public void refill(ArrayList<Trip2> trips) {
            mTrips = trips;
        }

        @Override
        public int getCount() {
            return mTrips.size();
        }

        @Override
        public Object getItem(int position) {
            return mTrips.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!isEmpty()) {
                Trip2 trip = mTrips.get(position);
                return new RouteAdapterView(mContext, trip);
            }
            return new View(mContext);
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
    	private boolean hasTime = false;
    	// TODO: Replace this hack with a proper layout defined in XML.
        public RouteAdapterView(Context context, Trip2 trip) {
            super(context);
            this.setOrientation(VERTICAL);

            float scale = getResources().getDisplayMetrics().density;

            this.setPadding((int)(5 * scale), (int)(10 * scale), (int)(5 * scale), (int)(10 * scale));

            LinearLayout timeLayout = new LinearLayout(context);

            TextView routeDetail = new TextView(context);
            routeDetail.setText(trip.toText());
            routeDetail.setTextColor(Color.BLACK);
            routeDetail.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            routeDetail.setPadding((int)(5 * scale), (int)(2 * scale), 0, (int)(2 * scale));
            //routeDetail.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

            timeLayout.addView(routeDetail);
            
            if (trip.mt6MessageExist || trip.remarksMessageExist || trip.rtuMessageExist) {
                ImageView warning = new ImageView(context);
                warning.setImageResource(R.drawable.trip_message_warning);
                warning.setPadding((int)(8 * scale), (int)(7 * scale), 0, 0);

                timeLayout.addView(warning);
            }

            TextView startAndEndPoint = new TextView(context);
            startAndEndPoint.setText(trip.origin.name + " - " + trip.destination.name);
            startAndEndPoint.setTextColor(0xFF444444); // Dark gray
            startAndEndPoint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            startAndEndPoint.setPadding((int)(5 * scale), (int)(2 * scale), 0, (int)(2 * scale));
            
            LinearLayout routeChanges = new LinearLayout(context);
            routeChanges.setPadding((int)(5 * scale), (int)(10 * scale), 0, 0);

            int currentTransportCount = 1;

            int transportCount = trip.subTrips.size();
            for (SubTrip subTrip : trip.subTrips) {
                ImageView change = new ImageView(context);
                change.setImageResource(subTrip.transport.getImageResource());
                change.setPadding(0, 0, (int)(5 * scale), 0);
                routeChanges.addView(change);

                /*
                RoundRectShape rr = new RoundRectShape(new float[]{6, 6, 6, 6, 6, 6, 6, 6}, null, null);
                ShapeDrawable ds = new ShapeDrawable();
                ds.setShape(rr);
                ds.setColorFilter(transport.getColor(), Mode.SCREEN);
                */

                // Okey, this is _not_ okey!!
                ArrayList<Integer> lineNumbers = new ArrayList<Integer>();
                lineNumbers = DeviationStore.extractLineNumbers(subTrip.transport.name, lineNumbers);
                if (!lineNumbers.isEmpty()) {
                    TextView lineNumberView = new TextView(context);
                    lineNumberView.setTextColor(Color.BLACK);
                    lineNumberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                    //lineNumberView.setBackgroundDrawable(ds);
                    //lineNumberView.setText(transport.getShortName());
                    lineNumberView.setText(Integer.toString(lineNumbers.get(0)));
                    //lineNumberView.setPadding(7, 2, 7, 2);
                    lineNumberView.setPadding((int)(2 * scale), (int)(2 * scale), (int)(2 * scale), (int)(2 * scale));
                    routeChanges.addView(lineNumberView);
                }

                if (transportCount > currentTransportCount) {
                    ImageView separator = new ImageView(context);
                    separator.setImageResource(R.drawable.transport_separator);
                    //separator.setPadding(9, 7, 9, 0);
                    separator.setPadding((int)(5 * scale), (int)(7 * scale), (int)(5 * scale), 0);
                    routeChanges.addView(separator);
                }

                currentTransportCount++;
            }

            this.addView(timeLayout);
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
     * Share the list of {@link Route}S with others.
     * @param routes the routes
     */
    public void share(ArrayList<Route> routes) {
        if (routes == null) {
            return; // TODO: Fire a toast with some message.
        }

        final Intent intent = new Intent(Intent.ACTION_SEND);

        String routesString = "";
        int routesCount = routes.size();
        int addedRoutes = 0;
        for (Route route : routes) {
            routesString += route.toTextRepresentation();
            addedRoutes++;
            if (routesCount > addedRoutes)
                routesString += "\n----------\n";
        }

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.routes_label));
        intent.putExtra(Intent.EXTRA_TEXT, routesString);

        startActivity(Intent.createChooser(intent, getText(R.string.share_label)));
    }

    /**
     * Background task for searching for routes.
     */
    private class SearchRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findJourney(params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
                // TODO: We should return the Trip here as well.
                return null;
            }
        }

        @Override
        protected void onPostExecute(Planner.Response result) {
            dismissProgress();

            if (result != null && !result.trips.isEmpty()) {
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM);
            }/* else if (result.hasAlternatives()) {
                onSiteAlternatives(result);
            }*/ else {
                showDialog(DIALOG_SEARCH_ROUTES_NO_RESULT);
            }
        }
    }

    /**
     * Background task for getting earlier routes.
     */
    private class GetEarlierRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findPreviousJourney(params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Planner.Response result) {
            dismissProgress();
            if (result != null && !result.trips.isEmpty()) {
                //mTrip.setRoutes(result);
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
    private class GetLaterRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findNextJourney(params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Planner.Response result) {
            dismissProgress();
            if (result != null && !result.trips.isEmpty()) {
                //mTrip.setRoutes(result);
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                showDialog(DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM);
            } else {
                showDialog(DIALOG_GET_ROUTES_SESSION_TIMEOUT);
            }
        }
    }
}
