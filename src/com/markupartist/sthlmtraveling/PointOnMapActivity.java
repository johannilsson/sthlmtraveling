/*
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
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
import java.util.List;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;
import com.markupartist.sthlmtraveling.graphics.BalloonOverlayView;
import com.markupartist.sthlmtraveling.graphics.BalloonOverlayView.OnTapBallonListener;
import com.markupartist.sthlmtraveling.graphics.FixedMyLocationOverlay;
import com.markupartist.sthlmtraveling.provider.planner.Stop;
import com.readystatesoftware.maps.OnSingleTapListener;
import com.readystatesoftware.maps.TapControlledMapView;

public class PointOnMapActivity extends BaseMapActivity implements OnSingleTapListener {
    private static final String TAG = "PointOnMapActivity";

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";
    public static String EXTRA_HELP_TEXT = "com.markupartist.sthlmtraveling.pointonmap.helptext";
    public static String EXTRA_MARKER_TEXT = "com.markupartist.sthlmtraveling.pointonmap.markertext";

    private TapControlledMapView mMapView;
    private MapController mapController;
    private GeoPoint mGeoPoint;
    private Stop mStop;
    private MyLocationOverlay mMyLocationOverlay;

    private BalloonOverlayView mBalloonView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.point_on_map);

        registerEvent("Point on map");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.point_on_map);

        Bundle extras = getIntent().getExtras();
        mStop = (Stop) extras.getParcelable(EXTRA_STOP);
        String helpText = extras.getString(EXTRA_HELP_TEXT);
        String markerText = extras.getString(EXTRA_MARKER_TEXT);

        showHelpToast(helpText);

        if (markerText == null) {
            markerText = getString(R.string.tap_to_select_this_point);
        }

        mMapView = (TapControlledMapView) findViewById(R.id.mapview);
        mapController = mMapView.getController();

        mMapView.setBuiltInZoomControls(true);
        mMapView.setOnSingleTapListener(this);

        myLocationOverlay();

        // Use stops location if present, otherwise set a geo point in 
        // central Stockholm.
        if (mStop.getLocation() != null) {
            mGeoPoint = new GeoPoint(
                    (int) (mStop.getLocation().getLatitude() * 1E6), 
                    (int) (mStop.getLocation().getLongitude() * 1E6));
            mapController.setZoom(16);
        } else {
            mGeoPoint = new GeoPoint(
                    (int) (59.325309 * 1E6), 
                    (int) (18.069763 * 1E6));
            mapController.setZoom(12);
        }
        mapController.animateTo(mGeoPoint);

        // Show the text balloon from start.
        showBalloon(mMapView, mGeoPoint);
    }

    private void showHelpToast(String helpText) {
        if (helpText != null) {
            Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.actionbar_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_my_location:
                if (mMyLocationOverlay.isMyLocationEnabled()) {
                    GeoPoint myLocation = mMyLocationOverlay.getMyLocation();
                    if (myLocation != null) {
                        mapController.animateTo(myLocation);
                    }
                } else {
                    toastMissingMyLocationSource();
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toastMissingMyLocationSource() {
        Toast.makeText(this, getText(R.string.my_location_source_disabled),
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
 
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableMyLocation();
    }

    private void disableMyLocation() {
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.disableCompass();
            mMyLocationOverlay.disableMyLocation();
        }
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void myLocationOverlay() {
        mMyLocationOverlay = new FixedMyLocationOverlay(this, mMapView);
        //if (mMyLocationOverlay.isMyLocationEnabled()) {
            mMyLocationOverlay.enableMyLocation();
        //}
        if (mMyLocationOverlay.isCompassEnabled()) {
            mMyLocationOverlay.enableCompass();
        }
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private String getStopName(Location location) {
        Geocoder geocoder = new Geocoder(this);
        String name = "Unkown";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                name = address.getThoroughfare();
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to get name " + e.getMessage());
        }
        return name;
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    public void showBalloon(final MapView mapView, final GeoPoint point) {
        
        boolean isRecycled;
        int viewOffset = 10;
        
        if (mBalloonView == null) {
            mBalloonView = new BalloonOverlayView(mapView.getContext(), viewOffset);
            //View clickRegion = (View) balloonView.findViewById(R.id.balloon_inner_layout);
            isRecycled = false;
        } else {
            isRecycled = true;
        }

        mBalloonView.setVisibility(View.GONE);
        mBalloonView.setLabel(getString(R.string.tap_to_select_this_point));

        MapView.LayoutParams params = new MapView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
                MapView.LayoutParams.BOTTOM_CENTER);
        params.mode = MapView.LayoutParams.MODE_MAP;
        
        mBalloonView.setOnTapBalloonListener(new OnTapBallonListener() {
            @Override
            public void onTap() {
                Toast.makeText(getApplicationContext(),
                        getText(R.string.point_selected), Toast.LENGTH_LONG).show();
                mStop.setLocation(point.getLatitudeE6(), point.getLongitudeE6());
                mStop.setName(getStopName(mStop.getLocation()));

                setResult(RESULT_OK, (new Intent()).putExtra(EXTRA_STOP, mStop));
                finish();
            }
        });

        mBalloonView.setVisibility(View.VISIBLE);

        if (isRecycled) {
            mBalloonView.setLayoutParams(params);
        } else {
            mapView.addView(mBalloonView, params);
        }
    }

    @Override
    public boolean onSingleTap(MotionEvent e) {
        Projection projection = mMapView.getProjection();
        GeoPoint geoPoint = projection.fromPixels(
                (int)e.getX(), (int)e.getY());

        showBalloon(mMapView, geoPoint);
        return true;
    }
}
