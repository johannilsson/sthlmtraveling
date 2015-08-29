/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.BadResponse;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Response;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.SmsTicketDialog;
import com.markupartist.sthlmtraveling.ui.view.TripView;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.LocationManager;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Routes activity
 * <p/>
 * Accepts a routes data URI in the format:
 * <p/>
 * <pre>
 * <code>journeyplanner://routes?start_point=STARTPOINT&end_point=ENDPOINT&time=TIME</code>
 * </pre>
 * <p/>
 * All parameters needs to be url encoded. Time is optional, but if provided it must be in
 * RFC 2445 format.
 */
public class RoutesActivity extends BaseListActivity implements
        LocationManager.LocationFoundListener,
        View.OnClickListener {

    /**
     * The Journey
     */
    static final String EXTRA_JOURNEY_QUERY = "sthlmtraveling.intent.action.JOURNEY_QUERY";

    /**
     * The trip.
     */
    @Deprecated
    static final String EXTRA_TRIP = "com.markupartist.sthlmtraveling.trip";

    /**
     * The start point for the search.
     */
    @Deprecated
    static final String EXTRA_START_POINT = "com.markupartist.sthlmtraveling.start_point";
    /**
     * The end point for the search.
     */
    @Deprecated
    static final String EXTRA_END_POINT = "com.markupartist.sthlmtraveling.end_point";
    /**
     * Departure time in RFC 2445 format.
     */
    static final String EXTRA_TIME = "com.markupartist.sthlmtraveling.time";

    /**
     * Indicates if the time is the departure or arrival time.
     */
    static final String EXTRA_IS_TIME_DEPARTURE = "com.markupartist.sthlmtraveling.is_time_departure";


    private final String TAG = "RoutesActivity";

    private static final int DIALOG_ILLEGAL_PARAMETERS = 0;
    private static final int DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM = 1;
    private static final int DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM = 2;
    private static final int DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM = 3;
    private static final int DIALOG_GET_ROUTES_SESSION_TIMEOUT = 4;
    private static final int DIALOG_SEARCH_ROUTES_NO_RESULT = 5;
    private static final int DIALOG_START_POINT_ALTERNATIVES = 6;
    private static final int DIALOG_END_POINT_ALTERNATIVES = 7;
    private static final int DIALOG_SEARCH_ROUTES_ERROR = 8;
    private static final int DIALOG_BUY_SMS_TICKET = 9;

    private static final int ADAPTER_EARLIER = 0;
    private static final int ADAPTER_ROUTES = 1;
    private static final int ADAPTER_LATER = 2;

    protected static final int REQUEST_CODE_CHANGE_TIME = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 1;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 2;

    private static final long HEADER_HIDE_ANIM_DURATION = 300;

    /**
     * Key to identify if the instance of SearchRoutesTask is in progress.
     */
    private static final String STATE_SEARCH_ROUTES_IN_PROGRESS =
            "com.markupartist.sthlmtraveling.searchroutes.inprogress";
    private static final String STATE_GET_EARLIER_ROUTES_IN_PROGRESS =
            "com.markupartist.sthlmtraveling.getearlierroutes.inprogress";
    private static final String STATE_GET_LATER_ROUTES_IN_PROGRESS =
            "com.markupartist.sthlmtraveling.getlaterroutes.inprogress";
    private static final String STATE_ROUTE_ERROR_CODE =
            "com.markupartist.sthlmtraveling.state.routeerrorcode";
    private static final String STATE_PLANNER_RESPONSE = "STATE_PLANNER_RESPONSE";

    private RoutesAdapter mRouteAdapter;
    private MultipleListAdapter mMultipleListAdapter;
    private ArrayList<HashMap<String, String>> mDateAdapterData;

    private LocationManager mMyLocationManager;
    private SearchRoutesTask mSearchRoutesTask;
    private GetEarlierRoutesTask mGetEarlierRoutesTask;
    private GetLaterRoutesTask mGetLaterRoutesTask;
    private Toast mToast;

    private Response mPlannerResponse;
    private JourneyQuery mJourneyQuery;
    private String mRouteErrorCode;

    private Bundle mSavedState;
    private Button mTimeAndDate;
    private View mEmptyView;

    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;
    private View mHeaderbarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.routes_list);

        registerScreen("Routes");

        // Get the journey query.
        mJourneyQuery = getJourneyQueryFromIntent(getIntent());
        if (savedInstanceState != null) {
            mPlannerResponse = savedInstanceState.getParcelable(STATE_PLANNER_RESPONSE);
            mJourneyQuery = savedInstanceState.getParcelable(EXTRA_JOURNEY_QUERY);
        }

        if (mJourneyQuery == null || (mJourneyQuery.origin.name == null
                || mJourneyQuery.destination.name == null)) {
            showDialog(DIALOG_ILLEGAL_PARAMETERS);
            // If passed with bad parameters, break the execution.
            return;
        }

        if (mJourneyQuery.origin.isMyLocation() || mJourneyQuery.destination.isMyLocation()) {
            initGoogleApiClient();
        }
        mMyLocationManager = new LocationManager(this, getGoogleApiClient());
        mMyLocationManager.setLocationListener(this);
        registerPlayService(mMyLocationManager);

        mHeaderbarView = findViewById(R.id.headerbar);

        mEmptyView = findViewById(R.id.empty_view);

        mTimeAndDate = (Button) findViewById(R.id.date_time);
        mTimeAndDate.setText(buildDateTimeString());
        mTimeAndDate.setOnClickListener(this);
        ViewHelper.tintIcon(mTimeAndDate.getCompoundDrawables()[0],
                getResources().getColor(R.color.primary_light));

        View headerView = getLayoutInflater().inflate(R.layout.empty, null);
        getListView().addHeaderView(headerView, null, false);
        getListView().setHeaderDividersEnabled(false);

        getListView().setVerticalFadingEdgeEnabled(false);
        getListView().setHorizontalFadingEdgeEnabled(false);

        initActionBar();
        updateStartAndEndPointViews(mJourneyQuery);
        updateJourneyHistory();
        initRoutes(mJourneyQuery);
        initAutoHideHeader(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_routes, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem starItem = menu.findItem(R.id.actionbar_item_star);
        if (isStarredJourney(mJourneyQuery)) {
            starItem.setIcon(R.drawable.ic_action_star_on);
            ViewHelper.tintIcon(getResources(), starItem.getIcon());
        } else {
            starItem.setIcon(R.drawable.ic_action_star_off);
            ViewHelper.tintIcon(getResources(), starItem.getIcon());
        }

        if (mPlannerResponse != null && mPlannerResponse.canBuySmsTicket()) {
            MenuItem smsItem = menu.findItem(R.id.actionbar_item_sms);
            ViewHelper.tintIcon(getResources(), smsItem.getIcon());
            smsItem.setVisible(false);  // disable SMS tickets on this view
        }

        ViewHelper.tintIcon(getResources(), menu.findItem(R.id.actionbar_item_reverse).getIcon());

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionbar_item_reverse:
                reverseJourneyQuery();
                return true;
            case R.id.actionbar_item_star:
                handleStarAction();
                supportInvalidateOptionsMenu();
                return true;
            case R.id.actionbar_item_sms:
                Analytics.getInstance(this).event("Ticket", "Click on ab");
                showDialog(DIALOG_BUY_SMS_TICKET);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private JourneyQuery getJourneyQueryFromIntent(Intent intent) {
        JourneyQuery journeyQuery;
        if (intent.hasExtra(EXTRA_JOURNEY_QUERY)) {
            journeyQuery = intent.getExtras().getParcelable(EXTRA_JOURNEY_QUERY);
        } else {

            Log.e(TAG, intent.getData().toString());

            journeyQuery = getJourneyQueryFromUri(intent.getData());
        }
        return journeyQuery;
    }

    private JourneyQuery getJourneyQueryFromUri(Uri uri) {
        JourneyQuery jq = new JourneyQuery();

        jq.origin = new Planner.Location();
        jq.origin.name = uri.getQueryParameter("start_point");
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_id"))) {
            jq.origin.id = Integer.parseInt(uri.getQueryParameter("start_point_id"));
        }
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))) {
            jq.origin.latitude =
                    (int) (Double.parseDouble(uri.getQueryParameter("start_point_lat")) * 1E6);
            jq.origin.longitude =
                    (int) (Double.parseDouble(uri.getQueryParameter("start_point_lng")) * 1E6);
        }

        jq.destination = new Planner.Location();
        jq.destination.name = uri.getQueryParameter("end_point");
        if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_id"))) {
            jq.destination.id = Integer.parseInt(uri.getQueryParameter("end_point_id"));
        }
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

        jq.time = new Date();
        String timeString = uri.getQueryParameter("time");
        if (!TextUtils.isEmpty(timeString)) {
            Log.e(TAG, "DATE FORMAT: " + timeString);
            // TODO: What is the format here?
            //jq.time.parse(timeString);
        } else {
            jq.time.setTime(System.currentTimeMillis());
        }

        return jq;
    }

    /**
     * Search for routes. Will first check if we already have data stored.
     *
     * @param journeyQuery The journey query
     */
    private void initRoutes(JourneyQuery journeyQuery) {
//        final Planner.Response savedResponse = (Planner.Response) getLastNonConfigurationInstance();
        if (mPlannerResponse != null) {
            onSearchRoutesResult(mPlannerResponse);
        } else {
            if (journeyQuery.origin.isMyLocation()
                    || journeyQuery.destination.isMyLocation()) {
                mMyLocationManager.requestLocation();
            } else {
                mSearchRoutesTask = new SearchRoutesTask();
                //mSearchRoutesTask.setOnSearchRoutesResultListener(this);
                mSearchRoutesTask.execute(mJourneyQuery);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        mSavedState = null;
    }

    /**
     * Restores the local state.
     *
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
//        restoreJourneyQuery(savedInstanceState);
        restoreSearchRoutesTask(savedInstanceState);
        restoreGetEarlierRoutesTask(savedInstanceState);
        restoreGetLaterRoutesTask(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_ROUTE_ERROR_CODE)) {
            mRouteErrorCode = savedInstanceState.getString(STATE_ROUTE_ERROR_CODE);
        }

        mPlannerResponse = savedInstanceState.getParcelable(STATE_PLANNER_RESPONSE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveJourneyQuery(outState);
        saveSearchRoutesTask(outState);
        saveGetEarlierRoutesTask(outState);
        saveGetLaterRoutesTask(outState);

        outState.putParcelable(STATE_PLANNER_RESPONSE, mPlannerResponse);

        if (!TextUtils.isEmpty(mRouteErrorCode)) {
            outState.putString(STATE_ROUTE_ERROR_CODE, mRouteErrorCode);
        }

        mSavedState = outState;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSavedState != null) restoreLocalState(mSavedState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancelSearchRoutesTask();
        onCancelGetEarlierRoutesTask();
        onCancelGetLaterRoutesTask();

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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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

    private String buildDateTimeString() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            String pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMdd HHmm");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            return simpleDateFormat.format(mJourneyQuery.time);
        } else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
            return dateFormat.format(mJourneyQuery.time);
        }
    }

    private void createSections() {
        SimpleAdapter earlierAdapter = createEarlierLaterAdapter(R.drawable.arrow_up_float);
        SimpleAdapter laterAdapter = createEarlierLaterAdapter(R.drawable.arrow_down_float);

        mMultipleListAdapter = new MultipleListAdapter();
        mMultipleListAdapter.addAdapter(ADAPTER_EARLIER, earlierAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_ROUTES, mRouteAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_LATER, laterAdapter);

        setListAdapter(mMultipleListAdapter);

        ViewHelper.crossfade(mEmptyView, getListView());
    }

    /**
     * Helper to create earlier or later adapter.
     *
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
                new String[]{"image"},
                new int[]{
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

        int headerViewsCount = getListView().getHeaderViewsCount();
        position -= headerViewsCount;

        int adapterId = mMultipleListAdapter.getAdapterId(position);
        switch (adapterId) {
            case ADAPTER_EARLIER:
                mGetEarlierRoutesTask = new GetEarlierRoutesTask();
                mGetEarlierRoutesTask.execute(mJourneyQuery);
                break;
            case ADAPTER_LATER:
                mGetLaterRoutesTask = new GetLaterRoutesTask();
                mGetLaterRoutesTask.execute(mJourneyQuery);
                break;
            case ADAPTER_ROUTES:
                Trip2 trip = (Trip2) mMultipleListAdapter.getItem(position);
                findRouteDetails(trip);
                break;
        }
    }

    public void onSearchRoutesResult(Planner.Response response) {
        mPlannerResponse = response;
        mJourneyQuery.ident = response.ident;
        mJourneyQuery.seqnr = response.seqnr;
        mJourneyQuery.hasPromotions = response.hasPromotions;
        mJourneyQuery.promotionNetwork = response.promotionNetwork;

        if (mRouteAdapter == null) {
            mRouteAdapter = new RoutesAdapter(this, response.trips);
            createSections();
        } else {
            // TODO: Scroll and animate to the new result.
            mRouteAdapter.refill(response.trips);
            mMultipleListAdapter.notifyDataSetChanged();
        }

        supportInvalidateOptionsMenu();
    }

    /*
    public void onSiteAlternatives(Trip trip) {
        // TODO: Handle alternatives...

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

    }
    */

    @Override
    public void onMyLocationFound(Location location) {
        Log.d(TAG, "onMyLocationFound: " + location);

        mMyLocationManager.removeUpdates();

        if (mToast != null) mToast.cancel();

        Planner.Location startPoint = mJourneyQuery.origin;
        Planner.Location endPoint = mJourneyQuery.destination;

        Site tmpStop = new Site();
        tmpStop.setLocation(location);

        if (startPoint.isMyLocation()) {
            if (!mMyLocationManager.isLocationAcceptable(location)) {
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
            if (!mMyLocationManager.isLocationAcceptable(location)) {
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

        updateStartAndEndPointViews(mJourneyQuery);

        // TODO: Maybe need to set start and end points to the trip again here?
        mSearchRoutesTask = new SearchRoutesTask();
        mSearchRoutesTask.execute(mJourneyQuery);
    }

    /**
     * Find route details. Will start {@link RouteDetailActivity}.
     *
     * @param trip the route to find details for
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
     * @param resultCode  From sending activity as per setResult().
     * @param data        From sending activity as per setResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHANGE_TIME:
                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Change time activity cancelled.");
                } else {
                    mJourneyQuery = data.getParcelableExtra(EXTRA_JOURNEY_QUERY);

                    updateStartAndEndPointViews(mJourneyQuery);
                    updateJourneyHistory();

                    mTimeAndDate.setText(buildDateTimeString());

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
                    Site startPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                    Log.d(TAG, "Got Stop " + startPoint);

                    mJourneyQuery.origin.name = Planner.Location.TYPE_MY_LOCATION;
                    mJourneyQuery.origin.latitude = (int) (startPoint.getLocation().getLatitude() * 1E6);
                    mJourneyQuery.origin.longitude = (int) (startPoint.getLocation().getLongitude() * 1E6);

                    mSearchRoutesTask = new SearchRoutesTask();
                    mSearchRoutesTask.execute(mJourneyQuery);

                    // TODO: Is this call really needed?
                    updateStartAndEndPointViews(mJourneyQuery);
                }
                break;
            case REQUEST_CODE_POINT_ON_MAP_END:
                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "action canceled");
                    finish();
                    return;
                } else {
                    Site endPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                    Log.d(TAG, "Got Stop " + endPoint);

                    mJourneyQuery.destination.name = Planner.Location.TYPE_MY_LOCATION;
                    mJourneyQuery.destination.latitude = (int) (endPoint.getLocation().getLatitude() * 1E6);
                    mJourneyQuery.destination.longitude = (int) (endPoint.getLocation().getLongitude() * 1E6);

                    mSearchRoutesTask = new SearchRoutesTask();
                    mSearchRoutesTask.execute(mJourneyQuery);

                    // TODO: Is this call really needed?
                    updateStartAndEndPointViews(mJourneyQuery);
                }

                break;
        }
    }

    protected void reverseJourneyQuery() {
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

        updateStartAndEndPointViews(mJourneyQuery);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
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
            case DIALOG_SEARCH_ROUTES_ERROR:

                return new AlertDialog.Builder(this)
                        .setTitle(R.string.planner_error_title)
                        .setMessage(Planner.plannerErrorCodeToStringRes(mRouteErrorCode))
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
            case DIALOG_BUY_SMS_TICKET:
                return SmsTicketDialog.createDialog(this, mPlannerResponse.getTariffZones());
        }
        return null;
    }

    /**
     * Constructs a search routes data URI.
     *
     * @param startPoint      the start point
     * @param endPoint        the end point
     * @param time            the time, pass null for now
     * @param isTimeDeparture true if the time is departure time, false if arrival
     * @return the data uri
     */
    public static Uri createRoutesUri(Site startPoint, Site endPoint, Time time,
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
                                + "&start_point_id=%s"
                                + "&start_point_lat=%s"
                                + "&start_point_lng=%s"
                                + "&end_point=%s"
                                + "&end_point_id=%s"
                                + "&end_point_lat=%s"
                                + "&end_point_lng=%s"
                                + "&time=%s"
                                + "&isTimeDeparture=%s",
                        Uri.encode(startPoint.getName()), startPoint.getId(), startLat, startLng,
                        Uri.encode(endPoint.getName()), endPoint.getId(), endLat, endLng,
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.date_time:
                Intent i = new Intent(RoutesActivity.this, ChangeRouteTimeActivity.class);
                i.putExtra(EXTRA_JOURNEY_QUERY, mJourneyQuery);
                startActivityForResult(i, REQUEST_CODE_CHANGE_TIME);
                break;
        }
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
                TripView v = new TripView(mContext);
                if (position == getCount() - 1) {
                    v.showDivider(false);
                }
                v.setTrip(trip);
                return v;
            }
            return new View(mContext);
        }
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

    protected void initAutoHideHeader(ListView listView) {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 1;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }

        mActionBarShown = show;
        onActionBarAutoShowOrHide(show);
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        if (shown) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mHeaderbarView.animate()
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                mHeaderbarView.setVisibility(View.VISIBLE);
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mHeaderbarView.animate()
                        .translationY(-mHeaderbarView.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                mHeaderbarView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Background task for searching for routes.
     */
    private class SearchRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;
        private String mErrorCode;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findJourney(RoutesActivity.this, params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
                // TODO: We should return the Trip here as well.
                return null;
            } catch (BadResponse e) {
                mWasSuccess = false;
                mErrorCode = e.errorCode;
                return null;
            }
        }

        @Override
        protected void onPostExecute(Planner.Response result) {
            dismissProgress();

            if (result != null && !result.trips.isEmpty()) {
                mEmptyView.setVisibility(View.GONE);
                onSearchRoutesResult(result);
            } else if (!mWasSuccess) {
                if (TextUtils.isEmpty(mErrorCode)) {
                    mEmptyView.setVisibility(View.GONE);
                    try {
                        showDialog(DIALOG_SEARCH_ROUTES_NETWORK_PROBLEM);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                    }
                } else {
                    mEmptyView.setVisibility(View.GONE);
                    mRouteErrorCode = mErrorCode;
                    try {
                        showDialog(DIALOG_SEARCH_ROUTES_ERROR);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show routes error dialog.");
                    }
                }
            }/* else if (result.hasAlternatives()) {
                onSiteAlternatives(result);
            }*/ else {
                mEmptyView.setVisibility(View.GONE);
                try {
                    showDialog(DIALOG_SEARCH_ROUTES_NO_RESULT);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to no results dialog.");
                }
            }
        }
    }

    /**
     * Background task for getting earlier routes.
     */
    private class GetEarlierRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;
        private String mErrorCode;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findPreviousJourney(RoutesActivity.this, params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
            } catch (BadResponse e) {
                mWasSuccess = false;
                mErrorCode = e.errorCode;
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
                if (TextUtils.isEmpty(mErrorCode)) {
                    try {
                        showDialog(DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                    }
                } else {
                    mRouteErrorCode = mErrorCode;
                    try {
                        showDialog(DIALOG_SEARCH_ROUTES_ERROR);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show routes error dialog.");
                    }
                }
            } else {
                try {
                    showDialog(DIALOG_GET_ROUTES_SESSION_TIMEOUT);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show session timeout dialog.");
                }
            }
        }
    }

    /**
     * Background task for getting later routes.
     */
    private class GetLaterRoutesTask extends AsyncTask<JourneyQuery, Void, Planner.Response> {
        private boolean mWasSuccess = true;
        private String mErrorCode;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected Planner.Response doInBackground(JourneyQuery... params) {
            try {
                return Planner.getInstance().findNextJourney(RoutesActivity.this, params[0]);
            } catch (IOException e) {
                mWasSuccess = false;
            } catch (BadResponse e) {
                mWasSuccess = false;
                mErrorCode = e.errorCode;
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
                if (TextUtils.isEmpty(mErrorCode)) {
                    try {
                        showDialog(DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                    }
                } else {
                    mRouteErrorCode = mErrorCode;
                    try {
                        showDialog(DIALOG_SEARCH_ROUTES_ERROR);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show routes error dialog.");
                    }
                }
            } else {
                try {
                    showDialog(DIALOG_GET_ROUTES_SESSION_TIMEOUT);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show session timeout dialog.");
                }
            }
        }
    }

    private boolean isStarredJourney(JourneyQuery journeyQuery) {
        String json;
        try {
            json = mJourneyQuery.toJson(false).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to convert journey to a json document.");
            return false;
        }

        String[] projection = new String[]{Journeys.JOURNEY_DATA,};
        Uri uri = Journeys.CONTENT_URI;
        String selection = Journeys.STARRED + " = ? AND " + Journeys.JOURNEY_DATA + " = ?";
        String[] selectionArgs = new String[]{"1", json};
        Cursor cursor = managedQuery(uri, projection, selection, selectionArgs, null);
        startManagingCursor(cursor);

        boolean isStarred = cursor.getCount() > 0;

        stopManagingCursor(cursor);

        return isStarred;
    }

    private void handleStarAction() {
        String json;
        try {
            json = mJourneyQuery.toJson(false).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to convert journey to a json document.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(Journeys.JOURNEY_DATA, json);
        Uri uri = Journeys.CONTENT_URI;
        String where = Journeys.JOURNEY_DATA + "= ?";
        String[] selectionArgs = new String[]{json};
        // TODO: Replace button with a checkbox and check with that instead?
        if (isStarredJourney(mJourneyQuery)) {
            values.put(Journeys.STARRED, "0");
            getContentResolver().update(
                    uri, values, where, selectionArgs);
        } else {
            values.put(Journeys.STARRED, "1");
            int affectedRows = getContentResolver().update(
                    uri, values, where, selectionArgs);
            if (affectedRows <= 0) {
                getContentResolver().insert(
                        Journeys.CONTENT_URI, values);
            }
        }
    }

    /**
     * Updates the journey history.
     */
    private void updateJourneyHistory() {
        // TODO: Move to async task.
        String json;
        try {
            json = mJourneyQuery.toJson(false).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to convert journey to a json document.");
            return;
        }
        String[] projection = new String[]{
                Journeys._ID,           // 0
                Journeys.JOURNEY_DATA,  // 1
                Journeys.STARRED,       // 2
        };
        String selection = Journeys.JOURNEY_DATA + " = ?";

        Cursor cursor = managedQuery(Journeys.CONTENT_URI, projection,
                selection, new String[]{json}, null);
        startManagingCursor(cursor);

        ContentValues values = new ContentValues();
        Uri journeyUri;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            journeyUri = ContentUris.withAppendedId(Journeys.CONTENT_URI,
                    cursor.getInt(0));
            getContentResolver().update(
                    journeyUri, values, null, null);
        } else {
            // Not sure if this is the best way to do it, but the lack limit and
            // offset in on a content provider leaves us to fetch all and iterate.
            values.put(Journeys.JOURNEY_DATA, json);
            journeyUri = getContentResolver().insert(
                    Journeys.CONTENT_URI, values);
            Cursor notStarredCursor = managedQuery(Journeys.CONTENT_URI,
                    projection,
                    Journeys.STARRED + " = ? OR " + Journeys.STARRED + " IS NULL",
                    new String[]{"0"},
                    Journeys.DEFAULT_SORT_ORDER);
            startManagingCursor(notStarredCursor);
            // +1 because the position is zero-based.
            if (notStarredCursor.moveToPosition(Journeys.DEFAULT_HISTORY_SIZE + 1)) {
                do {
                    Uri deleteUri = ContentUris.withAppendedId(
                            Journeys.CONTENT_URI, notStarredCursor.getInt(0));
                    getContentResolver().delete(deleteUri, null, null);
                } while (notStarredCursor.moveToNext());
            }
            stopManagingCursor(notStarredCursor);
        }
        stopManagingCursor(cursor);
        // TODO: Store created id and work on that while toggling if starred or not.

    }

}
