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

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.IntermediateStop;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.StringUtils;

import java.io.IOException;

public class ViewOnMapActivity extends AppCompatActivity {

    private static final String TAG = "ViewOnMapActivity";
    public static String EXTRA_LOCATION = "com.markupartist.sthlmtraveling.extra.Location";
    public static String EXTRA_JOURNEY_QUERY = "com.markupartist.sthlmtraveling.extra.JourneyQuery";
    public static String EXTRA_TRIP = "com.markupartist.sthlmtraveling.extra.Trip";

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private LatLng mFocusedLatLng;
    private Trip2 mTrip;
    private JourneyQuery mJourneyQuery;

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

        // TODO: Use transparent action bar, fix location of my location btn.

        Analytics.getInstance(this).registerScreen("View on map");
        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.map);

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.route_details_label);

        Bundle extras = getIntent().getExtras();

        mTrip = extras.getParcelable(EXTRA_TRIP);
        mJourneyQuery = extras.getParcelable(EXTRA_JOURNEY_QUERY);
        final Planner.Location focusedLocation = extras.getParcelable(EXTRA_LOCATION);

        mFocusedLatLng = new LatLng(focusedLocation.latitude / 1E6, focusedLocation.longitude / 1E6);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (savedInstanceState == null) {
            // First incarnation of this activity.
            mapFragment.setRetainInstance(true);
        } else {
            // Reincarnated activity. The obtained map is the same map instance in the previous
            // activity life cycle. There is no need to reinitialize it.
            mMap = mapFragment.getMap();
        }

        updateStartAndEndPointViews(mJourneyQuery);

        setUpMapIfNeeded();
    }

    public void fetchRoute(final Trip2 trip, final JourneyQuery journeyQuery) {
        setSupportProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Planner.getInstance().addIntermediateStops(
                            ViewOnMapActivity.this, trip, journeyQuery);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setSupportProgressBarIndeterminateVisibility(false);
                        addRoute(trip);
                    }
                });
            }
        }).start();
    }

    public void addRoute(Trip2 trip) {
        for (SubTrip subTrip : trip.subTrips) {

            float[] hsv = new float[3];
            Color.colorToHSV(subTrip.transport.getColor(this), hsv);
            float hueColor = hsv[0];

            // One polyline per subtrip, different colors.
            PolylineOptions options = new PolylineOptions();

            LatLng origin = new LatLng(
                    subTrip.origin.latitude / 1E6,
                    subTrip.origin.longitude / 1E6);
            options.add(origin);

            mMap.addMarker(new MarkerOptions()
                .position(origin)
                .title(getLocationName(subTrip.origin))
                .snippet(getRouteDescription(subTrip))
                .icon(BitmapDescriptorFactory.defaultMarker(hueColor)));

            for (IntermediateStop stop : subTrip.intermediateStop) {
                LatLng intermediateStop = new LatLng(
                        stop.location.latitude / 1E6,
                        stop.location.longitude / 1E6);
                options.add(intermediateStop);
                mMap.addMarker(new MarkerOptions()
                    .position(intermediateStop)
                    .title(getLocationName(stop.location))
                    .snippet(stop.arrivalTime)
                    .icon(BitmapDescriptorFactory.defaultMarker(hueColor)));
            }
            LatLng destination = new LatLng(
                    subTrip.destination.latitude / 1E6,
                    subTrip.destination.longitude / 1E6);
            options.add(destination);
            mMap.addMarker(new MarkerOptions()
                .position(destination)
                .title(getLocationName(subTrip.destination))
                .snippet(subTrip.arrivalTime)
                .icon(BitmapDescriptorFactory.defaultMarker(hueColor)));

            mMap.addPolyline(options
                    .width(5)
                    .color(subTrip.transport.getColor(this)));
        }
    }

    public String getRouteDescription(SubTrip subTrip) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        String description;
        if ("Walk".equals(subTrip.transport.type)) {
            description = getString(R.string.trip_map_description_walk,
                    subTrip.departureTime,
                    getLocationName(subTrip.destination));
        } else {
            description = getString(R.string.trip_map_description_normal,
                    subTrip.departureTime,
                    subTrip.transport.name,
                    subTrip.transport.towards,
                    getLocationName(subTrip.destination));
        }
        return description;
    }

    private String getLocationName(Planner.Location location) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        if (location == null) {
            return "Unknown";
        }
        if (location.isMyLocation()) {
            return getString(R.string.my_location);
        }
        return location.name;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
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

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        fetchRoute(mTrip, mJourneyQuery);

        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setMapToolbarEnabled(false);

        mMap.setMyLocationEnabled(true);

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.fromLatLngZoom(mFocusedLatLng, 16)
            ));
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
            ab.setTitle(journeyQuery.origin.getCleanName());
        }

        CharSequence via = null;
        if (journeyQuery.hasVia()) {
            via = journeyQuery.via.getCleanName();
        }
        if (journeyQuery.destination.isMyLocation()) {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, StringUtils.getStyledMyLocationString(this)}));
            } else {
                ab.setSubtitle(StringUtils.getStyledMyLocationString(this));
            }
        } else {
            if (via != null) {
                ab.setSubtitle(TextUtils.join(" • ", new CharSequence[]{via, journeyQuery.destination.getCleanName()}));
            } else {
                ab.setSubtitle(journeyQuery.destination.name);
            }
        }
    }
}
