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
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.data.models.Plan;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.RouteError;
import com.markupartist.sthlmtraveling.provider.JourneysProvider.Journey.Journeys;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.routing.Router;
import com.markupartist.sthlmtraveling.provider.routing.ScrollDir;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.TripView;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.IntentUtil;
import com.markupartist.sthlmtraveling.utils.LocationManager;
import com.markupartist.sthlmtraveling.utils.Monitor;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import org.json.JSONException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    protected static final int REQUEST_CODE_CHANGE_TIME = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 1;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 2;

    private static final long HEADER_HIDE_ANIM_DURATION = 300;

    private static final String STATE_ROUTE_ERROR_CODE =
            "com.markupartist.sthlmtraveling.state.routeerrorcode";
    private static final String STATE_PLANNER_RESPONSE =
            "com.markupartist.sthlmtraveling.state.plannerresponse";
    private static final String STATE_PLAN =
            "com.markupartist.sthlmtraveling.state.plan";

    private RoutesAdapter mRouteAdapter;

    private LocationManager mMyLocationManager;

    private JourneyQuery mJourneyQuery;
    private String mRouteErrorCode;
    private Plan mPlan;
    private Plan mTransitPlan;

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
    private FrameLayout mRouteAlternativesStub;
    private Router mRouter;
    private Monitor mMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.routes_list);

        registerScreen("Routes");

        // Get the journey query.
        mJourneyQuery = getJourneyQueryFromIntent(getIntent());
        if (savedInstanceState != null) {
            mTransitPlan = savedInstanceState.getParcelable(STATE_PLANNER_RESPONSE);
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
        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(this);
        if (elevationOverlayProvider.isThemeElevationOverlayEnabled()) {
            mHeaderbarView.setBackgroundColor(elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(4));
        }

        mEmptyView = findViewById(R.id.empty_view);

        mTimeAndDate = findViewById(R.id.date_time);
        mTimeAndDate.setText(buildDateTimeString());
        mTimeAndDate.setOnClickListener(this);

        MyApplication app = MyApplication.get(this);
        mRouter = new Router(app.getApiService());

        initActionBar();
        updateStartAndEndPointViews(mJourneyQuery);
        updateJourneyHistory();
        initListView();
        initAutoHideHeader(getListView());
        maybeRequestLocationUpdate();

        mMonitor = new Monitor() {
            @Override
            public void handleUpdate() {
                if (mTransitPlan != null
                        && mTransitPlan.shouldRefresh(System.currentTimeMillis())) {
                    mRouter.refreshTransit(mJourneyQuery, mPlanRefreshCallback);
                }
            }
        };
    }

    @Override
    public void onLocationPermissionGranted() {
        showProgress();
        mMyLocationManager.requestLocation();
    }

    @Override
    public void onLocationPermissionRationale() {
        dismissProgress();
        Snackbar.make(getListView(), R.string.permission_location_needed_search, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocationPermission();
                    }
                })
                .show();
    }

    @Override
    public void onLocationPermissionDontShowAgain() {
        dismissProgress();
        Snackbar.make(getListView(), R.string.permission_location_needed_search, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        IntentUtil.openSettings(RoutesActivity.this);
                    }
                })
                .show();
    }

    public void updateTransitRoutes(Plan plan) {
        dismissProgress();

        if (plan.hasErrors("transit")) {
            RouteError routeError = plan.getError("transit");
            mRouteErrorCode = routeError.getCode();
            showErrorForPublicTransport(getString(Planner.plannerErrorCodeToStringRes(mRouteErrorCode)));
        } else {
            onSearchRoutesResult(plan);
        }
    }

    public void updateRouteAlternatives(Plan plan) {
        mPlan = plan;

        if (mRouteAlternativesView == null) {
            mRouteAlternativesView = LayoutInflater.from(RoutesActivity.this).inflate(
                    R.layout.route_alternatives, mRouteAlternativesStub, false);
            mRouteAlternativesStub.addView(mRouteAlternativesView);
        }

        TextView footDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_foot_description);
        TextView bikeDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_bike_description);
        TextView carDurationText = (TextView) mRouteAlternativesView.findViewById(R.id.route_car_description);

        mRouteAlternativesView.findViewById(R.id.route_foot).setOnClickListener(this);
        mRouteAlternativesView.findViewById(R.id.route_bike).setOnClickListener(this);
        mRouteAlternativesView.findViewById(R.id.route_car).setOnClickListener(this);

        mLoadingRoutesViews = mRouteAlternativesView.findViewById(R.id.loading_routes);
        if (mTransitPlan == null && mRouteErrorCode == null) {
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
                    footDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(),
                            route.getDuration() * 1000));
                    mRouteAlternativesView.findViewById(R.id.route_foot).setVisibility(View.VISIBLE);
                    break;
                case "bike":
                    bikeDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(),
                            route.getDuration() * 1000));
                    mRouteAlternativesView.findViewById(R.id.route_bike).setVisibility(View.VISIBLE);
                    break;
                case "car":
                    carDurationText.setText(DateTimeUtil.formatDetailedDuration(getResources(),
                            route.getDuration() * 1000));
                    mRouteAlternativesView.findViewById(R.id.route_car).setVisibility(View.VISIBLE);
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

        if (mTransitPlan != null && mTransitPlan.canBuySmsTicket()) {
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
        }
        return super.onOptionsItemSelected(item);
    }

    private JourneyQuery getJourneyQueryFromIntent(Intent intent) {
        JourneyQuery journeyQuery = null;
        if (intent.hasExtra(EXTRA_JOURNEY_QUERY)) {
            journeyQuery = intent.getExtras().getParcelable(EXTRA_JOURNEY_QUERY);
        } else if (intent.getData() != null) {
            Log.e(TAG, intent.getData().toString());
            journeyQuery = new JourneyQuery.Builder()
                    .uri(intent.getData())
                    .create();
        }
        return journeyQuery;
    }

    /**
     * Ask for a fresh location if needed.
     */
    void maybeRequestLocationUpdate() {
        if ((mJourneyQuery.origin.isMyLocation()
                && !mJourneyQuery.origin.hasLocation())
                || (mJourneyQuery.destination.isMyLocation()
                && !mJourneyQuery.destination.hasLocation())) {
            verifyLocationPermission();
        }
    }

    /**
     * Search for routes. Will first check if we already have data stored.
     *
     * @param journeyQuery The journey query
     */
    private void initRoutes(JourneyQuery journeyQuery) {
        if (mTransitPlan != null) {
            fetchRouteAlternatives(mJourneyQuery);
            onSearchRoutesResult(mTransitPlan);
        } else if (mRouteErrorCode != null) {
            showErrorForPublicTransport(getString(Planner.plannerErrorCodeToStringRes(mRouteErrorCode)));
            fetchRouteAlternatives(mJourneyQuery);
        } else {
            // Fetch if origin or destination is not my location
            // Fetch if origin or destination is my location and has a location
            // If none of these are true we rely on the location manager to give us an callback
            // with a proper location that will trigger a new query.
            if ((!journeyQuery.origin.isMyLocation() && !journeyQuery.destination.isMyLocation())
                    || ((mJourneyQuery.origin.isMyLocation() && mJourneyQuery.origin.hasLocation())
                    || (mJourneyQuery.destination.isMyLocation() && mJourneyQuery.destination.hasLocation()))) {
                fetchTransitRoute(mJourneyQuery);
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
        restoreJourneyQuery(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_ROUTE_ERROR_CODE)) {
            mRouteErrorCode = savedInstanceState.getString(STATE_ROUTE_ERROR_CODE);
        }

        mTransitPlan = savedInstanceState.getParcelable(STATE_PLANNER_RESPONSE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveJourneyQuery(outState);

        outState.putParcelable(STATE_PLANNER_RESPONSE, mTransitPlan);
        outState.putParcelable(STATE_PLAN, mPlan);

        if (!TextUtils.isEmpty(mRouteErrorCode)) {
            outState.putString(STATE_ROUTE_ERROR_CODE, mRouteErrorCode);
        }

        mSavedState = outState;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        initRoutes(mJourneyQuery);
        mMonitor.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mMyLocationManager.removeUpdates();
        dismissProgress();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMyLocationManager.removeUpdates();
        mMonitor.onStop();

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
        mRouteAdapter = new RoutesAdapter(this, new ArrayList<Route>());
        // Faked stub, but get the job done.
        mRouteAlternativesStub = (FrameLayout) LayoutInflater.from(RoutesActivity.this)
                .inflate(R.layout.routes_list_header, getListView(), false);
        getListView().addHeaderView(mRouteAlternativesStub, null, false);
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
                mRouter.planTransit(mJourneyQuery, mPlanCallback, ScrollDir.PREVIOUS);
                break;
            case RoutesAdapter.TYPE_GET_LATER:
                mRouter.planTransit(mJourneyQuery, mPlanCallback, ScrollDir.NEXT);
                break;
            case RoutesAdapter.TYPE_ROUTES:
                Route route = mRouteAdapter.getTripItem(position);
                findRouteDetails(route);
                break;
        }
    }

    private Router.Callback mPlanRefreshCallback = new Router.Callback() {
        @Override
        public void onPlan(Plan plan) {
            updateTransitRoutes(plan);
        }

        @Override
        public void onPlanError(JourneyQuery journeyQuery, String errorCode) {
            Log.w(TAG, "Failed to reload routes.");
        }
    };

    private Router.Callback mPlanCallback = new Router.Callback() {
        @Override
        public void onPlan(Plan plan) {
            updateTransitRoutes(plan);
        }

        @Override
        public void onPlanError(JourneyQuery journeyQuery, String errorCode) {
            dismissProgress();
            mEmptyView.setVisibility(View.GONE);
            showErrorForPublicTransport(getString(R.string.network_problem_message));
        }
    };

    public void showRoutes() {
        ViewHelper.crossfade(mEmptyView, getListView());
    }

    public void onSearchRoutesResult(Plan plan) {
        mRouteErrorCode = null;
        mTransitPlan = plan;
        mJourneyQuery.ident = plan.getPaginateRef();
        // TODO: Add to API
        mJourneyQuery.hasPromotions = true;
        mJourneyQuery.promotionNetwork = 1;

        mRouteAdapter.refill(plan.getRoutes());
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
        if (mMyLocationManager.isLocationAcceptable(location)) {
            fetchTransitRoute(mJourneyQuery);
            fetchRouteAlternatives(mJourneyQuery);
        }
    }

    void fetchRouteAlternatives(JourneyQuery journeyQuery) {
        if (mPlan != null && mPlan.hasRoutes()) {
            updateRouteAlternatives(mPlan);
            return;
        }

        mRouter.plan(journeyQuery, new Router.Callback() {
            @Override
            public void onPlan(Plan plan) {
                if (plan != null && plan.hasRoutes()) {
                    updateRouteAlternatives(plan);
                }
            }

            @Override
            public void onPlanError(JourneyQuery journeyQuery, String errorCode) {
                // Do nothing here for now.
            }
        });
    }

    void fetchTransitRoute(JourneyQuery journeyQuery) {
        showProgress();
        mRouter.planTransit(journeyQuery, mPlanCallback);
    }

    /**
     * Find route details. Will start {@link RouteDetailActivity}.
     *
     * @param route the route to find details for
     */
    private void findRouteDetails(final Route route) {
        // TODO: Change to pass the trip later on instead.
        Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
        i.putExtra(RouteDetailActivity.EXTRA_JOURNEY_QUERY, mJourneyQuery);
        i.putExtra(RouteDetailActivity.EXTRA_ROUTE, route);
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_CHANGE_TIME:
                if (resultCode == RESULT_CANCELED) {
                    Log.d(TAG, "Change time activity cancelled.");
                } else {
                    mJourneyQuery = data.getParcelableExtra(EXTRA_JOURNEY_QUERY);

                    Log.e(TAG, "JQ: " + mJourneyQuery.toString());

                    updateStartAndEndPointViews(mJourneyQuery);
                    updateJourneyHistory();

                    mTimeAndDate.setText(buildDateTimeString());

                    fetchTransitRoute(mJourneyQuery);
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

                    fetchTransitRoute(mJourneyQuery);
                    fetchRouteAlternatives(mJourneyQuery);
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

                    fetchTransitRoute(mJourneyQuery);
                    fetchRouteAlternatives(mJourneyQuery);
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

        fetchTransitRoute(mJourneyQuery);
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
        }
        return null;
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

                Log.e(TAG, "JQ: " + mJourneyQuery.toString());

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
        } else if (mTransitPlan == null) {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        if (mLoadingRoutesViews != null) {
            mLoadingRoutesViews.setVisibility(View.GONE);
        }
        mEmptyView.setVisibility(View.GONE);
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
            mHeaderbarView.animate()
                    .translationY(0)
                    .alpha(1)
                    .setDuration(HEADER_HIDE_ANIM_DURATION)
                    .setInterpolator(new DecelerateInterpolator());
        } else {
            mHeaderbarView.animate()
                    .translationY(-mHeaderbarView.getBottom())
                    .alpha(0)
                    .setDuration(HEADER_HIDE_ANIM_DURATION)
                    .setInterpolator(new DecelerateInterpolator());
        }
    }

    public void showErrorForPublicTransport(String message) {
        dismissProgress();
        Snackbar.make(getListView(), message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Route this through something that can check what failed.
                        fetchTransitRoute(mJourneyQuery);
                    }
                })
                .show();
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
        private List<Route> mRoutes = Collections.emptyList();
        private SparseBooleanArray animatedItems = new SparseBooleanArray();

        public RoutesAdapter(Context context, ArrayList<Route> routes) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mRoutes = routes;
        }

        public void refill(List<Route> trips) {
            if (trips == mRoutes) {
                return;
            }
            mRoutes = trips;
            //animatedItems.clear();
            notifyDataSetChanged();
        }

        public int getDataItemCount() {
            if (mRoutes == null) {
                return 0;
            }
            return mRoutes.size();
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

            Route route = mRoutes.get(position);
            holder.bindTo(route);
            return convertView;
        }

        public Route getTripItem(int position) {
            return mRoutes.get(position - 1);
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

            public void bindTo(Route trip) {
                tripView.setTrip(trip);
            }
        }
    }
}
