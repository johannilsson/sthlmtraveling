/*
 * Copyright (C) 2013 Johan Nilsson <http://markupartist.com>
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

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.models.IntermediateStop;
import com.markupartist.sthlmtraveling.data.models.IntermediateResponse;
import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.Place;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.Step;
import com.markupartist.sthlmtraveling.data.models.TravelMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.LegUtil;
import com.markupartist.sthlmtraveling.utils.PolyUtil;
import com.markupartist.sthlmtraveling.utils.StringUtils;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ViewOnMapActivity extends BaseFragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "ViewOnMapActivity";
    public static String EXTRA_LOCATION = "com.markupartist.sthlmtraveling.extra.Location";
    public static String EXTRA_JOURNEY_QUERY = "com.markupartist.sthlmtraveling.extra.JourneyQuery";
    public static String EXTRA_TRIP = "com.markupartist.sthlmtraveling.extra.Trip";
    public static String EXTRA_ROUTE = "com.markupartist.sthlmtraveling.extra.Route";

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private LatLng mFocusedLatLng;
    private Route mTransitRoute;
    private JourneyQuery mJourneyQuery;
    private Route mRoute;
    private ApiService mApiService;
    private TripMarkerManager mTripMarkerManager;

    public static Intent createIntent(Context context, JourneyQuery query, Route route) {
        Intent intent = new Intent(context, ViewOnMapActivity.class);
        intent.putExtra(ViewOnMapActivity.EXTRA_ROUTE, route);
        intent.putExtra(ViewOnMapActivity.EXTRA_JOURNEY_QUERY, query);
        return intent;
    }

    public static Intent createIntent(Context context, Route route, JourneyQuery query, Site location) {
        Intent intent = new Intent(context, ViewOnMapActivity.class);
        intent.putExtra(ViewOnMapActivity.EXTRA_TRIP, route);
        intent.putExtra(ViewOnMapActivity.EXTRA_JOURNEY_QUERY, query);
        intent.putExtra(ViewOnMapActivity.EXTRA_LOCATION, location);
        return intent;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.getInstance(this).registerScreen("View on map");
        setContentView(R.layout.map);

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setBackgroundDrawable(getResources().getDrawableColorInt(R.drawable.ab_bg_black));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.route_details_label);

        Bundle extras = getIntent().getExtras();

        mRoute = extras.getParcelable(EXTRA_ROUTE);
        mTransitRoute = extras.getParcelable(EXTRA_TRIP);
        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);
        final Site focusedLocation = extras.getParcelable(EXTRA_LOCATION);

        mApiService = MyApplication.get(this).getApiService();

        if (focusedLocation != null) {
            mFocusedLatLng = new LatLng(
                    focusedLocation.getLocation().getLatitude(),
                    focusedLocation.getLocation().getLongitude());
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        updateStartAndEndPointViews(mJourneyQuery);
    }

    public void loadIntermediateStops(final Route route) {
        List<String> references = new ArrayList<>();

        for (Leg leg : route.getLegs()) {
            if (leg.getIntermediateStops().isEmpty()) {
                references.add(leg.getDetailRef());
            }
        }

        mApiService.getIntermediateStops(references, new Callback<IntermediateResponse>() {
            @Override
            public void success(IntermediateResponse intermediateResponse, Response response) {
                for (Leg leg : route.getLegs()) {
                    if (leg.isTransit()) {
                        leg.setIntermediateStops(intermediateResponse.getStops(leg.getDetailRef()));
                    }
                }
                showTransitRoute(route);
            }

            @Override
            public void failure(RetrofitError error) {
                showTransitRoute(route);
            }
        });
    }

    public void showTransitRoute(Route route) {
        mMap.clear();
        mTripMarkerManager.clearItems();

        for (Leg leg : route.getLegs()) {

            float[] hsv = new float[3];
            Color.colorToHSV(LegUtil.getColor(this, leg), hsv);
            float hueColor = hsv[0];

            // One polyline per subtrip, different colors.
            PolylineOptions options = new PolylineOptions();

            LatLng origin = new LatLng(
                    leg.getFrom().getLat(),
                    leg.getFrom().getLon());
            options.add(origin);

            mTripMarkerManager.addMarker(
                    origin,
                    getLocationName(leg.getFrom()),
                    getRouteDescription(leg),
                    LegUtil.getColor(this, leg),
                    true);

            BitmapDescriptor icon = getColoredMarker(LegUtil.getColor(this, leg));
            for (IntermediateStop stop : leg.getIntermediateStops()) {
                LatLng intermediateStop = new LatLng(
                        stop.getLocation().getLat(),
                        stop.getLocation().getLon());
                options.add(intermediateStop);
                Date date = stop.getTimeRt();
                if (date == null) {
                    date = stop.getTime();
                }
                String time = date != null ? DateFormat.getTimeFormat(this).format(date) : "";
                mTripMarkerManager.addMarker(
                        intermediateStop,
                        getLocationName(stop.getLocation()),
                        time,
                        LegUtil.getColor(this, leg),
                        false);
            }
            LatLng destination = new LatLng(
                    leg.getTo().getLat(),
                    leg.getTo().getLon());
            options.add(destination);
            mTripMarkerManager.addMarker(
                    destination,
                    getLocationName(leg.getTo()),
                    DateFormat.getTimeFormat(this).format(leg.getEndTime()),
                    LegUtil.getColor(this, leg),
                    true);

            mMap.addPolyline(options
                    .width(ViewHelper.dipsToPix(getResources(), 8))
                    .color(LegUtil.getColor(this, leg)));
        }
        mTripMarkerManager.cluster();
    }

    public String getRouteDescription(Leg leg) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        String description;
        if (TravelMode.FOOT.equals(leg.getTravelMode())) {
            description = getString(R.string.trip_map_description_walk,
                    DateFormat.getTimeFormat(this).format(leg.getStartTime()),
                    getLocationName(leg.getTo()));
        } else {
            description = getString(R.string.trip_map_description_normal,
                    DateFormat.getTimeFormat(this).format(leg.getStartTime()),
                    leg.getRouteName(),
                    leg.getHeadsing().getName(),
                    getLocationName(leg.getTo()));
        }
        return description;
    }

    private String getLocationName(Place location) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        if (location == null) {
            return "Unknown";
        }
        if (location.isMyLocation()) {
            return getString(R.string.my_location);
        }
        return location.getName();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationPermissionGranted() {
        //noinspection ResourceType
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onLocationPermissionRationale() {
        Snackbar.make(findViewById(R.id.map), R.string.permission_location_needed_maps,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocationPermission();
                    }
                })
                .show();
    }

    private void setUpMap() {
        if (mTransitRoute != null && mJourneyQuery != null) {
            showTransitRoute(mTransitRoute);
            loadIntermediateStops(mTransitRoute);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(mFocusedLatLng, 16)
            ));
        } else if (mRoute != null) {
            showRoute();
        }

        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setMapToolbarEnabled(false);

        verifyLocationPermission();
    }

    private void showRoute() {
        // If we have geometry parse and all.
        List<LatLng> all = new ArrayList<>();
        for (Leg leg : mRoute.getLegs()) {
            if (leg.getGeometry() != null) {
                List<LatLng> latLgns = PolyUtil.decode(leg.getGeometry());
                drawPolyline(latLgns);
                all.addAll(latLgns);

                BitmapDescriptor icon = getColoredMarker(ContextCompat.getColor(this, R.color.primary));
                for (Step step : leg.getSteps()) {
                    if ("arrive".equals(step.getCode())
                            || "depart".equals(step.getCode())
                            || "waypoint".equals(step.getCode())) {
                        mMap.addMarker(new MarkerOptions()
                                .position(latLgns.get(step.getPosition()))
                                .anchor(0.5f, 0.5f)
                                .icon(icon));
                    }
                }

            }
        }

        zoomToFit(all);
    }

    BitmapDescriptor getColoredMarker(@ColorInt int colorInt) {
        Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_line_marker);
        Bitmap bitmapCopy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCopy);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
        return BitmapDescriptorFactory.fromBitmap(bitmapCopy);
    }

    private void drawPolyline(List<LatLng> latLngs) {
        Polyline poly = mMap.addPolyline(new PolylineOptions()
                .zIndex(1000)
                .addAll(latLngs)
                .width(ViewHelper.dipsToPix(getResources(), 8))
                .color(ContextCompat.getColor(this, R.color.primary))
                .geodesic(true));
        poly.setZIndex(Float.MAX_VALUE);
    }

    private void zoomToFit(List<LatLng> latLngs) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        for (LatLng latLng : latLngs) {
            builder.include(latLng);
        }

        LatLngBounds bounds = builder.build();

        // A "random" value for the top padding, fix to fetch from toolbar later on.
        int height = ViewHelper.getDisplayHeight(this) - ViewHelper.dipsToPix(getResources(), 52);
        int width = ViewHelper.getDisplayWidth(this);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height,
                getResources().getDimensionPixelSize(R.dimen.padding_large));
        mMap.moveCamera(cu);
        //mMap.animateCamera(cu);
    }

    /**
     * Update the action bar with start and end points.
     * @param journeyQuery the journey query
     */
    protected void updateStartAndEndPointViews(final JourneyQuery journeyQuery) {
        ActionBar ab = getSupportActionBar();
        if (journeyQuery.origin.isMyLocation()) {
            ab.setTitle(StringUtils.getStyledMyLocationString(this));
        } else {
            ab.setTitle(journeyQuery.origin.getName());
        }

        CharSequence via = null;
        if (journeyQuery.hasVia()) {
            via = journeyQuery.via.getName();
        }
        if (journeyQuery.destination.isMyLocation()) {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, StringUtils.getStyledMyLocationString(this)}));
            } else {
                ab.setSubtitle(StringUtils.getStyledMyLocationString(this));
            }
        } else {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, journeyQuery.destination.getName()}));
            } else {
                ab.setSubtitle(journeyQuery.destination.getName());
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mTripMarkerManager = new TripMarkerManager(this, mMap);
        mMap.setOnCameraChangeListener(mTripMarkerManager);
        mMap.setOnMarkerClickListener(mTripMarkerManager);
        mMap.setInfoWindowAdapter(mTripMarkerManager);

        setUpMap();
    }
}
