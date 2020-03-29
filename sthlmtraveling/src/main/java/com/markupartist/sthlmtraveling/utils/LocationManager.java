/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationCallback;
//import com.google.android.gms.location.LocationListener;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;

import java.util.List;

import static com.markupartist.sthlmtraveling.RouteDetailActivity.TAG;

/**
 * Created by johan on 4/5/14.
 */
public class LocationManager implements PlayService, LocationListener {
    // Timeout for finding
    private static final long FIND_LOCATION_TIME_OUT_MILLIS = 8000;
    // Accepted minimum location accuracy.
    private static final int ACCEPTED_LOCATION_ACCURACY_METERS = 300;
    // Accepted max age of an location
    private static final int ACCEPTED_LOCATION_AGE_MILLIS = 60000;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    private final Context mContext;
    private final HuaweiApiClient mGoogleApiClient;
    private final LocationRequestTimeOut mLocationRequestTimeOut;
    private LocationFoundListener mLocationFoundListener;
    private boolean mLocationRequested;
    private boolean mHighAccuracy = true;
    private LocationListener mLocationListener;

    public LocationManager(final Context context, HuaweiApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;
        mLocationRequestTimeOut = new LocationRequestTimeOut();
    }

    public LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(mHighAccuracy ?
                LocationRequest.PRIORITY_HIGH_ACCURACY :
                LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setExpirationDuration(ACCEPTED_LOCATION_AGE_MILLIS);
        locationRequest.setNumUpdates(5);

        return locationRequest;
    }

    public void setLocationListener(final LocationFoundListener listener) {
        mLocationFoundListener = listener;
    }

    Location getLastLocation() {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return LocationServices.getFusedLocationProviderClient(mContext).getLastLocation().getResult();
    }

    /**
     * Is the provided {@link Location} accepted?
     *
     * @param location the location
     * @return <code>true</code> if accepted <code>false</code> otherwise
     */
    public boolean isLocationAcceptable(final Location location) {
        if (location == null) {
            return false;
        }
        if (!mHighAccuracy) {
            return true;
        }
        long timeSinceUpdate = System.currentTimeMillis() - location.getTime();
        return timeSinceUpdate <= ACCEPTED_LOCATION_AGE_MILLIS
                && location.getAccuracy() <= ACCEPTED_LOCATION_ACCURACY_METERS;
    }

    @Override
    public void onStop() {
        removeUpdates();
    }

    public void resumed() {
    }

    public void paused() {
    }

    void reportLocationFound(final Location location) {
        if (mLocationFoundListener != null) {
            mLocationFoundListener.onMyLocationFound(location);
        }
    }

    public void setAccuracy(boolean high) {
        mHighAccuracy = high;
    }

    public void requestLocation() {
        if (mGoogleApiClient == null) {
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            mLocationRequested = true;
        } else {
            requestLocationOrDeliverCurrent();
        }
    }

    void requestLocationOrDeliverCurrent() {
        if (mGoogleApiClient == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        final Location location = getLastLocation();
        if (isLocationAcceptable(location)) {
            onLocationChanged(location);
        } else {
            mLocationRequestTimeOut.start();

            mLocationListener = new LocationListener();

            LocationServices.getFusedLocationProviderClient(mContext).requestLocationUpdates(
                    createLocationRequest(), mLocationListener, Looper.getMainLooper());
        }
    }

    public void removeUpdates() {
        if (mGoogleApiClient == null) {
            return;
        }

        mLocationRequestTimeOut.cancel();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.getFusedLocationProviderClient(mContext).removeLocationUpdates(mLocationListener);
            mLocationListener = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isLocationAcceptable(location)) {
            removeUpdates();
            reportLocationFound(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected() {
        if (mLocationRequested) {
            mLocationRequested = false;
            requestLocationOrDeliverCurrent();
        }
    }

    private class LocationListener extends LocationCallback {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            onLocationChanged(locationResult.getLastLocation());
        }
    }

    /**
     * Used to determine when we should stop search for location changes.
     */
    private class LocationRequestTimeOut extends CountDownTimer {
        public LocationRequestTimeOut() {
            super(FIND_LOCATION_TIME_OUT_MILLIS, 1000);
        }

        @Override
        public void onFinish() {
            reportLocationFound(getLastLocation());
            // Once we have reported a location, remove the need to search for more.
            removeUpdates();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            // Needed by the interface.
        }
    }

    /**
     *
     */
    public interface LocationFoundListener {
        /**
         * Called when a location is found.
         *
         * @param location the location, could be null
         */
        void onMyLocationFound(Location location);
    }
}
