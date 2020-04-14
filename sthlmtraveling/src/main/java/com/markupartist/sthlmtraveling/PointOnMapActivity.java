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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.HuaweiMap.OnInfoWindowClickListener;
import com.huawei.hms.maps.HuaweiMap.OnMapClickListener;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.SupportMapFragment;
import com.huawei.hms.maps.UiSettings;
import com.huawei.hms.maps.model.BitmapDescriptorFactory;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;
import com.huawei.hms.maps.model.MapStyleOptions;
import com.huawei.hms.maps.model.Marker;
import com.huawei.hms.maps.model.MarkerOptions;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.LocationManager;

import java.io.IOException;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;

public class PointOnMapActivity extends BaseFragmentActivity
        implements OnMapClickListener, OnInfoWindowClickListener, OnMapReadyCallback, LocationManager.LocationFoundListener {

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";
    public static String EXTRA_HELP_TEXT = "com.markupartist.sthlmtraveling.pointonmap.helptext";
    public static String EXTRA_MARKER_TEXT = "com.markupartist.sthlmtraveling.pointonmap.markertext";

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private HuaweiMap mMap;

    private Site mStop;
    private Marker mMarker;
    private LocationManager mMyLocationManager;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
     }
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Use transparent action bar, fix location of my location btn.

        //requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.map);

        Analytics.getInstance(this).registerScreen("Point on map");

        ActionBar actionBar = getSupportActionBar();
        //actionBar.setBackgroundDrawable(getResources().getDrawableColorInt(R.drawable.ab_bg_black));
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.point_on_map);

        Bundle extras = getIntent().getExtras();
        // TODO: Can we make this as a none member.
        mStop = (Site) extras.getParcelable(EXTRA_STOP);
        String helpText = extras.getString(EXTRA_HELP_TEXT);
        String markerText = extras.getString(EXTRA_MARKER_TEXT);

        showHelpToast(helpText);

        if (markerText == null) {
            markerText = getString(R.string.tap_to_select_this_point);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initGoogleApiClient(true);
        mMyLocationManager = new LocationManager(this, getGoogleApiClient());
        mMyLocationManager.setLocationListener(this);
        mMyLocationManager.setAccuracy(false);
        registerPlayService(mMyLocationManager);
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

    private void showHelpToast(String helpText) {
        if (helpText != null) {
            Toast.makeText(this, helpText, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationPermissionGranted() {
        mMap.setMyLocationEnabled(true);
        mMyLocationManager.requestLocation();
    }

    @Override
    public void onLocationPermissionRationale() {
    }

    private void setUpMap() {
        verifyLocationPermission();

        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setMapToolbarEnabled(false);

        // Use stops location if present, otherwise set a geo point in 
        // central Stockholm.
        LatLng latLng;
        int zoom;
        if (mStop.getLocation() != null) {
            latLng = new LatLng(
                    mStop.getLocation().getLatitude(), 
                    mStop.getLocation().getLongitude());
            zoom = 16;
        } else {
            latLng = new LatLng(59.325309, 18.069763);
            zoom = 12;
        }

        mMarker = mMap.addMarker(new MarkerOptions()
            .position(latLng)
            .title(getString(R.string.tap_to_select_this_point))
            .visible(true)
            .draggable(true)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMarker.showInfoWindow();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.fromLatLngZoom(latLng, zoom)
            ));
    }

    @Override
    public void onMapClick(LatLng position) {
        mMarker.setPosition(position);
        mMarker.showInfoWindow();
    }

    private String getStopName(Location location) {
        Geocoder geocoder = new Geocoder(this);
        String name = "Unknown";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                name = address.getThoroughfare();
            }
        } catch (IOException e) {
            Log.d("Map", "Failed to get name " + e.getMessage());
        }
        return name;
    }

    @Override
    public void onInfoWindowClick(Marker clickedMarker) {
        if (clickedMarker.equals(mMarker)) {
            Toast.makeText(getApplicationContext(),
                    getText(R.string.point_selected), Toast.LENGTH_LONG).show();
            Location location = new Location("sthlmtraveling");
            location.setLatitude(mMarker.getPosition().latitude);
            location.setLongitude(mMarker.getPosition().longitude);
            mStop.setLocation(location);
            mStop.setName(getStopName(mStop.getLocation()));
            setResult(RESULT_OK, (new Intent()).putExtra(EXTRA_STOP, mStop));
            finish();
        }
    }

    @Override
    public void onMapReady(HuaweiMap googleMap) {
        mMap = googleMap;
        setUpMap();
    }

    @Override
    public void onMyLocationFound(Location location) {
        if (location == null) {
            return;
        }

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMarker.setPosition(latLng);

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(latLng, 12)
        ));

    }
}
