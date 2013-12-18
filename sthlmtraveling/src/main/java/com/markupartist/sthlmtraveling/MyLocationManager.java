/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;

/**
 * Manager for retrieving the user location.
 */
public class MyLocationManager {
    private static final String TAG = "MyLocationManager";
    private static final long FIND_LOCATION_TIME_OUT_MILLIS = 8000;
    private static final int ACCEPTED_LOCATION_ACCURACY_METERS = 300;
    private static final int ACCEPTED_LOCATION_AGE_MILLIS = 600000;
    private LocationRequestTimeOut mLocationRequestTimeOut;
    private LocationManager mLocationManager;
    private MyLocationFoundListener mMyLocationFoundListener;
    private MyLocationListener mGpsLocationListener;
    private MyLocationListener mNetworkLocationListener;
    private LocationListener mLocationListener;

    /**
     * Constructs a new MyLocationManager.
     * @param locationManager the location manager
     */
    public MyLocationManager(LocationManager locationManager) {
        mLocationManager = locationManager;
        mLocationRequestTimeOut = new LocationRequestTimeOut();
    }

    public void setLocationListener(LocationListener locationListener) {
        mLocationListener = locationListener;
    }

    /**
     * Request location updates, will periodically query the GPS and network
     * provider for location updates.
     * @param myLocationFoundListener the callback
     */
    public void requestLocationUpdates(MyLocationFoundListener myLocationFoundListener) {
        mMyLocationFoundListener = myLocationFoundListener;

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mGpsLocationListener = new MyLocationListener();
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0, mGpsLocationListener);
        }

        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mNetworkLocationListener = new MyLocationListener();
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    0, 0, mNetworkLocationListener);
        }

        if (mNetworkLocationListener != null || mGpsLocationListener != null) {
            mLocationRequestTimeOut.start();
        } else {
            reportLocationFound(getLastKnownLocation());
        }
    }

    /**
     * Removes any current registration for location updates. 
     */
    public void removeUpdates() {
        if (mGpsLocationListener != null)
            mLocationManager.removeUpdates(mGpsLocationListener);
        if (mNetworkLocationListener != null)
            mLocationManager.removeUpdates(mNetworkLocationListener);

        mLocationRequestTimeOut.cancel();
        mMyLocationFoundListener = null;
    }

    /**
     * Get the last known location, will look for the best location from both
     * GPS and the network provider.
     * @return
     */
    public Location getLastKnownLocation() {
        Location gpsLocation =
            mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation =
            mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        return getBestLocation(gpsLocation, networkLocation);
    }

    /**
     * Get the best location out of the passed {@link Location}S.
     * @param location1 the location
     * @param location2 the location
     * @return the best location
     */
    public Location getBestLocation(Location location1, Location location2) {
        if (location1 == null && location2 == null) {
            return null;
        } else if (location2 == null) {
            return location1;
        } else if (location1 == null) {
            return location2;
        } else {
            int location1Score =
                (int) (getLocationAge(location1) + location1.getAccuracy());
            int location2Score =
                (int) (getLocationAge(location2) + location2.getAccuracy());

            if (location1Score <= location2Score) {
                Log.d(TAG, location1.getProvider() + " won");
                return location1;
            }
            return location2;
        }
    }

    private void reportLocationFound(Location location) {
        Log.d(TAG, "reportLocationFound");
        if (mMyLocationFoundListener != null) {
            mMyLocationFoundListener.onMyLocationFound(location);
        }
    }

    /**
     * Is the provided {@link Location} accepted?
     * @param location the location
     * @return <code>true</code> if accepted <code>false</code> otherwise
     */
    public boolean shouldAcceptLocation(Location location) {
        if (location == null)
            return false;

        long timeSinceUpdate = System.currentTimeMillis() - location.getTime();
        if (timeSinceUpdate <= ACCEPTED_LOCATION_AGE_MILLIS 
                && location.getAccuracy() <= ACCEPTED_LOCATION_ACCURACY_METERS) {
            return true;
        }
        return false;
    }

    public int getLocationAge(Location location) {
        return (int) ((System.currentTimeMillis() - location.getTime()) / 1000);
    }

    /**
     * Used for receiving notifications from the LocationManager when the 
     * location has changed.
     */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (mLocationListener != null) {
                mLocationListener.onLocationChanged(location);
            }
            
            if (shouldAcceptLocation(location)) {
                reportLocationFound(location);
                removeUpdates();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Needed by the interface.
            
        }

        @Override
        public void onProviderEnabled(String provider) {
            // Needed by the interface.
            
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Needed by the interface.
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
            reportLocationFound(getLastKnownLocation());
            // Once we have reported a location, remove the need to search 
            // for more.
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
    public interface MyLocationFoundListener {
        /**
         * Called when a location is found.
         * @param location the location, could be null
         */
        public void onMyLocationFound(Location location);
    }
}
