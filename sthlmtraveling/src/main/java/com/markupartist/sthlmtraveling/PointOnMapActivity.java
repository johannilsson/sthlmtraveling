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
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.Analytics;

import java.io.IOException;
import java.util.List;

public class PointOnMapActivity extends BaseActivity
        implements OnMapClickListener, OnInfoWindowClickListener, OnMapReadyCallback {

    public static String EXTRA_STOP = "com.markupartist.sthlmtraveling.pointonmap.stop";
    public static String EXTRA_HELP_TEXT = "com.markupartist.sthlmtraveling.pointonmap.helptext";

    private Site mStop;
    private Marker mMarker;

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

        setContentView(R.layout.map);

        Analytics.getInstance(this).registerScreen("Point on map");

        Bundle extras = getIntent().getExtras();
        mStop = extras.getParcelable(EXTRA_STOP);
        String helpText = extras.getString(EXTRA_HELP_TEXT);

        ActionBar actionBar = initActionBar();
        actionBar.setTitle(R.string.point_on_map);
        showHelpToast(helpText);

        ImageView mCenterMarker = (ImageView) findViewById(R.id.map_center_marker);
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    public void onMapClick(LatLng position) {
        mMarker.setPosition(position);
        mMarker.showInfoWindow();
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
    public void onMapReady(GoogleMap map) {
        map.setMyLocationEnabled(true);
        map.setOnMapClickListener(this);
        map.setOnInfoWindowClickListener(this);

        UiSettings settings = map.getUiSettings();
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

        mMarker = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.tap_to_select_this_point))
                        .visible(true)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        mMarker.showInfoWindow();

        map.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(latLng, zoom)
        ));
    }
}
