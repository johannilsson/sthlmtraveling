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
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

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
    private final GoogleApiClient mGoogleApiClient;
    private final LocationRequestTimeOut mLocationRequestTimeOut;
    private LocationFoundListener mLocationFoundListener;
    private boolean mLocationRequested;

    public LocationManager(final Context context, GoogleApiClient googleApiClient) {
        mContext = context;
        mGoogleApiClient = googleApiClient;
        mLocationRequestTimeOut = new LocationRequestTimeOut();
    }

    public LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
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
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
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
        long timeSinceUpdate = System.currentTimeMillis() - location.getTime();
        return timeSinceUpdate <= ACCEPTED_LOCATION_AGE_MILLIS
                && location.getAccuracy() <= ACCEPTED_LOCATION_ACCURACY_METERS;
    }

    public void onStart() {
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
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, createLocationRequest(), this);
        }
    }

    public void removeUpdates() {
        if (mGoogleApiClient == null) {
            return;
        }

        mLocationRequestTimeOut.cancel();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
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
    public void onConnected() {
        if (mLocationRequested) {
            mLocationRequested = false;
            requestLocationOrDeliverCurrent();
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
