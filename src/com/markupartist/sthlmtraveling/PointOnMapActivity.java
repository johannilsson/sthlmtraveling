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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.markupartist.sthlmtraveling.graphics.BalloonOverlayView;
import com.markupartist.sthlmtraveling.graphics.FixedMyLocationOverlay;
import com.markupartist.sthlmtraveling.graphics.BalloonOverlayView.OnTapBallonListener;
import com.markupartist.sthlmtraveling.provider.planner.Stop;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;

public class PointOnMapActivity extends MapActivity {
    private static final String TAG = "PointOnMapActivity";

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";
    public static String EXTRA_HELP_TEXT = "com.markupartist.sthlmtraveling.pointonmap.helptext";
    public static String EXTRA_MARKER_TEXT = "com.markupartist.sthlmtraveling.pointonmap.markertext";

    private MapView mMapView;
    private MapController mapController;
    private GeoPoint mGeoPoint;
    private OverlayManager mOverlayManager;
    private ManagedOverlayItem mManagedOverlayItem;
    private Stop mStop;
    private MyLocationOverlay mMyLocationOverlay;

    private BalloonOverlayView balloonView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.point_on_map);

        Log.d(TAG, "init point on map...");
        /*
        final AutoCompleteTextView locationSearch = (AutoCompleteTextView) findViewById(R.id.location_search);
        ImageButton locationSearchButton = (ImageButton) findViewById(R.id.location_search_btn);
        locationSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Move to async task...
                
                Log.d(TAG, "on click search for location..");
                
                Geocoder geocoder = new Geocoder(PointOnMapActivity.this);
                List<Address> addresses = null;
                try {
                    //addresses = geocoder.getFromLocationName(locationSearch.getText().toString(), 10);

                    double lowerLeftLatitude = 58.87;
                    double lowerLeftLongitude = 17.1754;
                    double upperRightLatitude = 59.8307;
                    double upperRightLongitude = 19.1907;

                    addresses = geocoder.getFromLocationName(locationSearch.getText().toString(), 10, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude);
                    
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (addresses != null && !addresses.isEmpty()) {
                    
                    Log.d(TAG, "Setting the geo point...");
                    
                    Address address = addresses.get(0);

                    mGeoPoint = new GeoPoint(
                            (int) (address.getLatitude() * 1E6),
                            (int) (address.getLongitude() * 1E6));
                    mapController.animateTo(mGeoPoint);
                    mapController.setZoom(16);
                    // Show the text balloon from start.
                    showBalloon(mMapView, mGeoPoint);
                }
            }
        });
        */
        
        
        Bundle extras = getIntent().getExtras();
        mStop = (Stop) extras.getParcelable(EXTRA_STOP);
        String helpText = extras.getString(EXTRA_HELP_TEXT);
        String markerText = extras.getString(EXTRA_MARKER_TEXT);

        showHelpToast(helpText);

        if (markerText == null) {
            markerText = getString(R.string.tap_to_select_this_point);
        }

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();
        myLocationOverlay();

        // Use stops location if present, otherwise set a geo point in 
        // central Stockholm.
        if (mStop.getLocation() != null) {
            mGeoPoint = new GeoPoint(
                    (int) (mStop.getLocation().getLatitude() * 1E6), 
                    (int) (mStop.getLocation().getLongitude() * 1E6));
            mapController.setZoom(16);
        } else {
            //mGeoPoint = mMyLocationOverlay.getMyLocation();
            mGeoPoint = new GeoPoint(
                    (int) (59.325309 * 1E6), 
                    (int) (18.069763 * 1E6));
            mapController.setZoom(12);
        }
        mapController.animateTo(mGeoPoint);

        // Show the text balloon from start.
        showBalloon(mMapView, mGeoPoint);

        mOverlayManager = new OverlayManager(getApplication(), mMapView);

        pointToSelectOverlay();
    }

    private void showHelpToast(String helpText) {
        if (helpText != null) {
            Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean b) {
        //pointToSelectOverlay();
        //myLocationOverlay();
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
        //if (mMyLocationOverlay.isMyLocationEnabled()) {
            mMyLocationOverlay.enableMyLocation();
        //}
        if (mMyLocationOverlay.isCompassEnabled()) {
            mMyLocationOverlay.enableCompass();
        }
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private void pointToSelectOverlay() {
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
                managedOverlay.remove(mManagedOverlayItem);
                mManagedOverlayItem = new ManagedOverlayItem(geoPoint, "title", "snippet");
                managedOverlay.add(mManagedOverlayItem);

                showBalloon(mMapView, geoPoint);
                //mapController.animateTo(geoPoint);    
                mMapView.invalidate();

                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent zoomEvent, ManagedOverlay managedOverlay) {
                return false;
            }

        });
        mOverlayManager.populate();
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
        
        if (balloonView == null) {
            balloonView = new BalloonOverlayView(mapView.getContext(), viewOffset);
            //View clickRegion = (View) balloonView.findViewById(R.id.balloon_inner_layout);
            isRecycled = false;
        } else {
            isRecycled = true;
        }

        balloonView.setVisibility(View.GONE);
        balloonView.setLabel(getString(R.string.tap_to_select_this_point));

        MapView.LayoutParams params = new MapView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
                MapView.LayoutParams.BOTTOM_CENTER);
        params.mode = MapView.LayoutParams.MODE_MAP;
        
        balloonView.setOnTapBalloonListener(new OnTapBallonListener() {
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

        balloonView.setVisibility(View.VISIBLE);

        if (isRecycled) {
            balloonView.setLayoutParams(params);
        } else {
            mapView.addView(balloonView, params);
        }
    }
}
