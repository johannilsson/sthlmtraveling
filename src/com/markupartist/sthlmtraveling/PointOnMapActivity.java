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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.markupartist.sthlmtraveling.planner.Stop;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;

public class PointOnMapActivity extends MapActivity {

    private static final String TAG = "PointOnMapActivity";

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";

    private MapView mMapView;
    private MapController mapController;
    private GeoPoint mGeoPoint;
    private OverlayManager mOverlayManager;
    private ManagedOverlayItem mManagedOverlayItem;

    private Stop mStop;

    private MyLocationOverlay mMyLocationOverlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.point_on_map);

        Bundle extras = getIntent().getExtras();
        mStop = (Stop) extras.getParcelable(EXTRA_STOP);

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();

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

        mOverlayManager = new OverlayManager(getApplication(), mMapView);

        pointToSelectOverlay();
        myLocationOverlay();
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
                if (mMyLocationOverlay != null) {
                    mapController.animateTo(mMyLocationOverlay.getMyLocation());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
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
        mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);
        mMyLocationOverlay.enableCompass();
        mMyLocationOverlay.enableMyLocation();
        /*
        mMyLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mapController.animateTo(mMyLocationOverlay.getMyLocation());
            }
        });
        */
    }
    
    private void pointToSelectOverlay() {
        //ManagedOverlay managedOverlay = mOverlayManager.createOverlay();
        ManagedOverlay managedOverlay = mOverlayManager.createOverlay(
                getResources().getDrawable(R.drawable.point_on_map_marker));

        mManagedOverlayItem = new ManagedOverlayItem(mGeoPoint, "title", "snippet");
        managedOverlay.add(mManagedOverlayItem);

        managedOverlay.setOnOverlayGestureListener(
                new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent motionEvent, ManagedOverlay managedOverlay,
                    GeoPoint geoPoint, ManagedOverlayItem managedOverlayItem) {
                return false;
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
                if (managedOverlayItem != null) {
                    GeoPoint currentPoint = managedOverlayItem.getPoint();
                    Location location = new Location("sthlmtraveling");
                    location.setLatitude(currentPoint.getLatitudeE6() / 1E6);
                    location.setLongitude(currentPoint.getLongitudeE6() / 1E6);

                    mStop.setName(getStopName(location));
                    mStop.setLocation(location);

                    setResult(RESULT_OK, (new Intent())
                            .putExtra(EXTRA_STOP, mStop));
                    finish();
                } else {
                    managedOverlay.remove(mManagedOverlayItem);
                    mManagedOverlayItem = new ManagedOverlayItem(geoPoint, "", "");
                    managedOverlay.add(mManagedOverlayItem);

                    mapController.animateTo(geoPoint);    
                    mMapView.invalidate();
                }
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
}
