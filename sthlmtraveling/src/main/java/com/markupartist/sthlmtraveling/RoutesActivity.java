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
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.data.models.Plan;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.BadResponse;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Response;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.provider.routing.Router;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.SmsTicketDialog;
import com.markupartist.sthlmtraveling.ui.view.TripView;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.LocationManager;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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

    private final String TAG = "RoutesActivity";

    private static final int DIALOG_ILLEGAL_PARAMETERS = 0;
    private static final int DIALOG_GET_EARLIER_ROUTES_NETWORK_PROBLEM = 2;
    private static final int DIALOG_GET_LATER_ROUTES_NETWORK_PROBLEM = 3;
    private static final int DIALOG_GET_ROUTES_SESSION_TIMEOUT = 4;
    private static final int DIALOG_SEARCH_ROUTES_NO_RESULT = 5;
    private static final int DIALOG_SEARCH_ROUTES_ERROR = 8;
    private static final int DIALOG_BUY_SMS_TICKET = 9;

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
    private static final String STATE_PLANNER_RESPONSE =
            "com.markupartist.sthlmtraveling.state.plannerresponse";
    private static final String STATE_PLAN =
            "com.markupartist.sthlmtraveling.state.plan";

    private RoutesAdapter mRouteAdapter;

    private LocationManager mMyLocationManager;
    private SearchRoutesTask mSearchRoutesTask;
    private GetEarlierRoutesTask mGetEarlierRoutesTask;
    private GetLaterRoutesTask mGetLaterRoutesTask;

    private Response mPlannerResponse;
    private JourneyQuery mJourneyQuery;
    private String mRouteErrorCode;
    private Plan mPlan;

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
    private Toast mToast;
    private View mLoadingRoutesViews;
    private View mRouteAlternativesView;

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
            mPlan = savedInstanceState.getParcelable(STATE_PLAN);
        }

        if (mJourneyQuery == null || (mJourneyQuery.origin.getName() == null
                || mJourneyQuery.destination.getName() == null)) {
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

        initActionBar();
        updateStartAndEndPointViews(mJourneyQuery);
        updateJourneyHistory();
        initListView();
        //initRoutes(mJourneyQuery);
        initAutoHideHeader(getListView());
    }

    public void updateRouteAlternatives(Plan plan) {
        mPlan = plan;

        if (mRouteAlternativesView == null) {
            mRouteAlternativesView = LayoutInflater.from(RoutesActivity.this)
                    .inflate(R.layout.route_alternatives, getListView(), false);
            getListView().addHeaderView(mRouteAlternativesView, null, false);
        }

        TextView footDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_foot_description);
        TextView bikeDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_bike_description);
        TextView carDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_car_description);

        mRouteAlternativesView.findViewById(R.id.route_foot).setOnClickListener(this);
        mRouteAlternativesView.findViewById(R.id.route_bike).setOnClickListener(this);
        mRouteAlternativesView.findViewById(R.id.route_car).setOnClickListener(this);

        mLoadingRoutesViews = mRouteAlternativesView.findViewById(R.id.loading_routes);
        if (mPlannerResponse == null) {
            showProgress();
        }

        ImageView footIcon = (ImageView) mRouteAlternativesView.findViewById(R.id.route_foot_icon);
        ImageView bikeIcon = (ImageView) mRouteAlternativesView.findViewById(R.id.route_bike_icon);
        ImageView carIcon = (ImageView) mRouteAlternativesView.findViewById(R.id.route_car_icon);
        int iconColor = ContextCompat.getColor(this, R.color.icon_default);
        footIcon.setImageDrawable(ViewHelper.getDrawableColorInt(this, R.drawable.ic_transport_walk_20dp, iconColor));
        bikeIcon.setImageDrawable(ViewHelper.getDrawableColorInt(this, R.drawable.ic_transport_bike_20dp, iconColor));
        carIcon.setImageDrawable(ViewHelper.getDrawableColorInt(this, R.drawable.ic_transport_car_20dp, iconColor));

        for (Route route : plan.getRoutes()) {
            switch (route.getMode()) {
                case "foot":
                    footDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(), route.getLegs().get(0).getDuration() * 1000));
                    break;
                case "bike":
                    bikeDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(), route.getLegs().get(0).getDuration() * 1000));
                    break;
                case "car":
                    carDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(), route.getLegs().get(0).getDuration() * 1000));
                    break;
            }
        }

        showRoutes();
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

        jq.origin = new Site();
        jq.origin.setName(uri.getQueryParameter("start_point"));
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_id"))) {
            jq.origin.setId(uri.getQueryParameter("start_point_id"));
        }
        if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))) {
            jq.origin.setLocation(
                    (int) (Double.parseDouble(uri.getQueryParameter("start_point_lat")) * 1E6),
                    (int) (Double.parseDouble(uri.getQueryParameter("start_point_lng")) * 1E6));
        }

        jq.destination = new Site();
        jq.destination.setName(uri.getQueryParameter("end_point"));
        if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_id"))) {
            jq.destination.setId(uri.getQueryParameter("end_point_id"));
        }
        if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("end_point_lng"))) {
            jq.destination.setLocation(
                    (int) (Double.parseDouble(uri.getQueryParameter("end_point_lat")) * 1E6),
                    (int) (Double.parseDouble(uri.getQueryParameter("end_point_lng")) * 1E6));
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
        if (mPlannerResponse != null) {
            fetchRouteAlternatives(mJourneyQuery);
            onSearchRoutesResult(mPlannerResponse);
        } else {
            if (journeyQuery.origin.isMyLocation()
                    || journeyQuery.destination.isMyLocation()) {
                mMyLocationManager.requestLocation();
            } else {
                mSearchRoutesTask = new SearchRoutesTask();
                mSearchRoutesTask.execute(mJourneyQuery);

                fetchRouteAlternatives(mJourneyQuery);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
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
        outState.putParcelable(STATE_PLAN, mPlan);

        if (!TextUtils.isEmpty(mRouteErrorCode)) {
            outState.putString(STATE_ROUTE_ERROR_CODE, mRouteErrorCode);
        }

        mSavedState = outState;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSavedState != null) restoreLocalState(mSavedState);

        initRoutes(mJourneyQuery);
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
            String pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMd HHmm");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            return simpleDateFormat.format(mJourneyQuery.time);
        } else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
            return dateFormat.format(mJourneyQuery.time);
        }
    }

    private void initListView() {
        mRouteAdapter = new RoutesAdapter(this, new ArrayList<Trip2>());
        setListAdapter(mRouteAdapter);
        getListView().setHeaderDividersEnabled(false);
        getListView().setVerticalFadingEdgeEnabled(false);
        getListView().setHorizontalFadingEdgeEnabled(false);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        int headerViewsCount = getListView().getHeaderViewsCount();
        position -= headerViewsCount;

        int viewType = mRouteAdapter.getItemViewType(position);
        switch (viewType) {
            case RoutesAdapter.TYPE_GET_EARLIER:
                mGetEarlierRoutesTask = new GetEarlierRoutesTask();
                mGetEarlierRoutesTask.execute(mJourneyQuery);
                break;
            case RoutesAdapter.TYPE_GET_LATER:
                mGetLaterRoutesTask = new GetLaterRoutesTask();
                mGetLaterRoutesTask.execute(mJourneyQuery);
                break;
            case RoutesAdapter.TYPE_ROUTES:
                Trip2 trip = mRouteAdapter.getTripItem(position);
                findRouteDetails(trip);
                break;
        }
    }

    public void showRoutes() {
        ViewHelper.crossfade(mEmptyView, getListView());
    }

    public void onSearchRoutesResult(Planner.Response response) {
        mPlannerResponse = response;
        mJourneyQuery.ident = response.ident;
        mJourneyQuery.seqnr = response.seqnr;
        mJourneyQuery.hasPromotions = response.hasPromotions;
        mJourneyQuery.promotionNetwork = response.promotionNetwork;

        mRouteAdapter.refill(response.trips);
        showRoutes();
        supportInvalidateOptionsMenu();
        dismissProgress();
    }

    @Override
    public void onMyLocationFound(Location location) {
        Log.d(TAG, "onMyLocationFound: " + location);

        mMyLocationManager.removeUpdates();

        if (mToast != null) mToast.cancel();

        Site startPoint = mJourneyQuery.origin;
        Site endPoint = mJourneyQuery.destination;

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
                startPoint.setLocation(location);
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
                endPoint.setLocation(location);
            }
        }

        updateStartAndEndPointViews(mJourneyQuery);

        // TODO: Maybe need to set start and end points to the trip again here?
        mSearchRoutesTask = new SearchRoutesTask();
        mSearchRoutesTask.execute(mJourneyQuery);

        fetchRouteAlternatives(mJourneyQuery);
    }

    void fetchRouteAlternatives(JourneyQuery journeyQuery) {
        if (mPlan != null) {
            updateRouteAlternatives(mPlan);
            return;
        }

        MyApplication app = MyApplication.get(this);
        Router router = new Router(app.getApiService());
        router.plan(journeyQuery, new Router.Callback() {
            @Override
            public void onPlan(Plan plan) {
                if (plan != null) {
                    updateRouteAlternatives(plan);
                }
            }
        });
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

                    mJourneyQuery.origin.setName(Site.TYPE_MY_LOCATION);
                    mJourneyQuery.origin.setLocation(startPoint.getLocation());

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

                    mJourneyQuery.destination.setName(Site.TYPE_MY_LOCATION);
                    mJourneyQuery.destination.setLocation(endPoint.getLocation());

                    mSearchRoutesTask = new SearchRoutesTask();
                    mSearchRoutesTask.execute(mJourneyQuery);

                    // TODO: Is this call really needed?
                    updateStartAndEndPointViews(mJourneyQuery);
                }

                break;
        }
    }

    protected void reverseJourneyQuery() {
        Site tmpStartPoint = new Site(mJourneyQuery.destination);
        Site tmpEndPoint = new Site(mJourneyQuery.origin);

        mJourneyQuery.origin = tmpStartPoint;
        mJourneyQuery.destination = tmpEndPoint;

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
            case R.id.route_foot:
                for (Route route : mPlan.getRoutes()) {
                    if ("foot".equals(route.getMode())) {
                        startActivity(ViewOnMapActivity.createIntent(this, mJourneyQuery, route));
                        break;
                    }
                }
                break;
            case R.id.route_bike:
                for (Route route : mPlan.getRoutes()) {
                    if ("bike".equals(route.getMode())) {
                        startActivity(ViewOnMapActivity.createIntent(this, mJourneyQuery, route));
                        break;
                    }
                }
                break;
            case R.id.route_car:
                for (Route route : mPlan.getRoutes()) {
                    if ("car".equals(route.getMode())) {
                        startActivity(ViewOnMapActivity.createIntent(this, mJourneyQuery, route));
                        break;
                    }
                }
                break;
        }
    }

    /**
     * Show progress dialog.
     */
    private void showProgress() {
        if (mLoadingRoutesViews != null && mRouteAdapter.getDataItemCount() == 0) {
            mLoadingRoutesViews.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        if (mLoadingRoutesViews != null) {
            mLoadingRoutesViews.setVisibility(View.GONE);
        }
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
                    showErrorForPublicTransport(getString(R.string.network_problem_message));
                } else {
                    mEmptyView.setVisibility(View.GONE);
                    mRouteErrorCode = mErrorCode;
                    try {
                        showDialog(DIALOG_SEARCH_ROUTES_ERROR);
                    } catch (BadTokenException e) {
                        Log.w(TAG, "Caught BadTokenException when trying to show routes error dialog.");
                    }
                }
            } else {
                mEmptyView.setVisibility(View.GONE);
                try {
                    showDialog(DIALOG_SEARCH_ROUTES_NO_RESULT);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to no results dialog.");
                }
            }
        }
    }

    public void showErrorForPublicTransport(String message) {
        Snackbar.make(getListView(), message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Route this through something that can check what failed.
                        mSearchRoutesTask = new SearchRoutesTask();
                        mSearchRoutesTask.execute(mJourneyQuery);
                    }
                })
                .show();
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
                        showErrorForPublicTransport(getString(Planner.plannerErrorCodeToStringRes(mRouteErrorCode)));
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
                        showErrorForPublicTransport(getString(Planner.plannerErrorCodeToStringRes(mRouteErrorCode)));
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

    private static class RoutesAdapter extends BaseAdapter {

        public static final int TYPE_GET_EARLIER = 0;
        public static final int TYPE_GET_LATER = 1;
        public static final int TYPE_ROUTES = 2;

        private final LayoutInflater mInflater;
        private final Context mContext;
        private ArrayList<Trip2> mTrips;
        private SparseBooleanArray animatedItems = new SparseBooleanArray();

        public RoutesAdapter(Context context, ArrayList<Trip2> trips) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mTrips = trips;
        }

        public void refill(ArrayList<Trip2> trips) {
            if (trips == mTrips) {
                return;
            }
            mTrips = trips;
            animatedItems.clear();
            notifyDataSetChanged();
        }

        public int getDataItemCount() {
            return mTrips.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_GET_EARLIER;
            }
            if (position - 1 < getDataItemCount() && getDataItemCount() > 0) {
                return TYPE_ROUTES;
            }
            return TYPE_GET_LATER;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getCount() {
            if (getDataItemCount() > 0) {
                // number of items plus others.
                return getDataItemCount() + 2;
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            int viewType = getItemViewType(position);
            switch (viewType) {
                case TYPE_GET_EARLIER:
                    convertView = createViewForEarlierLater(position, convertView, parent);
                    break;
                case TYPE_GET_LATER:
                    convertView = createViewForEarlierLater(position, convertView, parent);
                    break;
                case TYPE_ROUTES:
                    convertView = createViewForRoute(position, convertView, parent);

                    if (!animatedItems.get(position, false)) {
                        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.bottom_to_top);
                        convertView.startAnimation(animation);
                        animatedItems.put(position, true);
                    }
                    break;
            }


            return convertView;
        }

        View createViewForEarlierLater(int position, View convertView, ViewGroup parent) {
            NextPreviousViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.earlier_later_routes_row, parent, false);
                holder = new NextPreviousViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (NextPreviousViewHolder) convertView.getTag();
            }
            // The first one, need to be adapted.
            if (position == 0) {
                holder.icon.setImageResource(R.drawable.ic_expand_less_grey600_24dp);
            } else {
                holder.icon.setImageResource(R.drawable.ic_expand_more_grey600_24dp);
            }
            return convertView;
        }

        View createViewForRoute(int position, View convertView, ViewGroup parent) {
            RouteViewHolder holder;
            if (convertView == null) {
                convertView = new TripView(mContext);
                holder = new RouteViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (RouteViewHolder) convertView.getTag();
            }

            // convert postion to a routes position.
            position = position - 1;

            Trip2 trip = mTrips.get(position);
            holder.bindTo(trip);
            return convertView;
        }

        public Trip2 getTripItem(int position) {
            return mTrips.get(position - 1);
        }

        public static class NextPreviousViewHolder {
            ImageView icon;
            public NextPreviousViewHolder(View view) {
                icon = (ImageView) view.findViewById(R.id.earlier_later);
            }
        }

        public static class RouteViewHolder {
            TripView tripView;
            public RouteViewHolder(View view) {
                tripView = (TripView) view;
            }

            public void bindTo(Trip2 trip) {
                tripView.setTrip(trip);
            }
        }
    }
}
