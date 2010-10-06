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

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.markupartist.sthlmtraveling.graphics.BalloonOverlayView;
import com.markupartist.sthlmtraveling.graphics.FixedMyLocationOverlay;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;

public class ViewOnMapActivity extends BaseMapActivity {
    private static final String TAG = "ViewOnMapActivity";

    public static String EXTRA_LOCATION = "com.markupartist.sthlmtraveling.pointonmap.location";
    public static String EXTRA_MARKER_TEXT = "com.markupartist.sthlmtraveling.pointonmap.markertext";

    private MapView mMapView;
    private MapController mapController;
    private GeoPoint mGeoPoint;
    private OverlayManager mOverlayManager;
    private ManagedOverlayItem mManagedOverlayItem;
    private MyLocationOverlay mMyLocationOverlay;

    private BalloonOverlayView balloonView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.point_on_map);

        registerEvent("View on map");

        Bundle extras = getIntent().getExtras();
        Location location = (Location) extras.getParcelable(EXTRA_LOCATION);
        String markerText = extras.getString(EXTRA_MARKER_TEXT);

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();
        myLocationOverlay();


        mGeoPoint = new GeoPoint(
                (int) (location.getLatitude() * 1E6), 
                (int) (location.getLongitude() * 1E6));

        mapController.setZoom(16);
        mapController.animateTo(mGeoPoint); 

        // Show the text balloon from start.
        showBalloon(mMapView, mGeoPoint, markerText);

        mOverlayManager = new OverlayManager(getApplication(), mMapView);

        locationOverlay();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_point_on_map, menu);
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
            mMyLocationOverlay.enableCompass();
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
        if (mMyLocationOverlay.isMyLocationEnabled()) {
            mMyLocationOverlay.enableMyLocation();
        }
        if (mMyLocationOverlay.isCompassEnabled()) {
            mMyLocationOverlay.enableCompass();
        }
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private void locationOverlay() {
        /*ManagedOverlay managedOverlay = mOverlayManager.createOverlay(
                mLabelMarker.getMarker());*/
        final ManagedOverlay managedOverlay = mOverlayManager.createOverlay(
                getResources().getDrawable(R.drawable.marker));

        mManagedOverlayItem = new ManagedOverlayItem(mGeoPoint, "title", "snippet");
        managedOverlay.add(mManagedOverlayItem);

        managedOverlay.setOnOverlayGestureListener(
                new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent, ManagedOverlay managedOverlay,
                    GeoPoint geoPoint, ManagedOverlayItem managedOverlayItem) {
                mapController.zoomIn();
                return true;
            }

            @Override
            public void onLongPress(MotionEvent arg0, ManagedOverlay arg1) {
                // Needed by interface, not used
            }

            @Override
            public void onLongPressFinished(MotionEvent motionEvent,
                                            ManagedOverlay managedOverlay,
                                            GeoPoint geoPoint,
                                            ManagedOverlayItem managedOverlayItem) {
                // Needed by interface, not used
            }

            @Override
            public boolean onScrolled(MotionEvent arg0, MotionEvent arg1,
                    float arg2, float arg3, ManagedOverlay arg4) {
                return false;
            }

            @Override
            public boolean onSingleTap(MotionEvent motionEvent, 
                                       ManagedOverlay managedOverlay,
                                       GeoPoint geoPoint,
                                       ManagedOverlayItem managedOverlayItem) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent, ManagedOverlay managedOverlay) {
                return false;
            }

        });
        mOverlayManager.populate();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    public void showBalloon(final MapView mapView, final GeoPoint point,
            String markerText) {
        boolean isRecycled;
        int viewOffset = 10;
        
        if (balloonView == null) {
            balloonView = new BalloonOverlayView(mapView.getContext(), viewOffset);
            isRecycled = false;
        } else {
            isRecycled = true;
        }

        balloonView.setVisibility(View.GONE);
        balloonView.setLabel(markerText);

        MapView.LayoutParams params = new MapView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
                MapView.LayoutParams.BOTTOM_CENTER);
        params.mode = MapView.LayoutParams.MODE_MAP;

        balloonView.setVisibility(View.VISIBLE);

        if (isRecycled) {
            balloonView.setLayoutParams(params);
        } else {
            mapView.addView(balloonView, params);
        }
    }
}
