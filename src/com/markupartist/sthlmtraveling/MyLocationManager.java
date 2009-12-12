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

public class MyLocationManager {
    private static final String TAG = "MyLocationManager";
    private static final long FIND_LOCATION_TIME_OUT_MILLIS = 5000;
    private static final int ACCEPTED_LOCATION_ACCURACY_METERS = 100;
    private static final int ACCEPTED_LOCATION_AGE_MILLIS = 150000;
    private LocationRequestTimeOut mLocationRequestTimeOut;
    private LocationManager mLocationManager;
    private MyLocationFoundListener mMyLocationFoundListener;
    private MyLocationListener mGpsLocationListener;
    private MyLocationListener mNetworkLocationListener;

    public MyLocationManager(LocationManager locationManager) {
        mLocationManager = locationManager;
        mLocationRequestTimeOut = new LocationRequestTimeOut();
    }

    public void requestLocationUpdates(MyLocationFoundListener myLocationFoundListener) {
        mMyLocationFoundListener = myLocationFoundListener;

        mGpsLocationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                0, 0, mGpsLocationListener);
        mNetworkLocationListener = new MyLocationListener();
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                0, 0, mNetworkLocationListener);

        mLocationRequestTimeOut.start();
    }

    public void removeUpdates() {
        if (mGpsLocationListener != null)
            mLocationManager.removeUpdates(mGpsLocationListener);
        if (mNetworkLocationListener != null)
            mLocationManager.removeUpdates(mNetworkLocationListener);

        mLocationRequestTimeOut.cancel();
        mMyLocationFoundListener = null;
    }

    public Location getLastKnownLocation() {
        Location gpsLocation =
            mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation =
            mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        return getBestLocation(gpsLocation, networkLocation);
    }

    public Location getBestLocation(Location location1, Location location2) {
        if (location1 == null && location2 == null) {
            return null;
        } else if (location2 == null) {
            return location1;
        } else if (location1 == null) {
            return location2;
        } else {
            float location1Score = location1.getTime() + location1.getAccuracy();
            float location2Score = location2.getTime() + location2.getAccuracy();
            Log.d(TAG, "location1Score=" + location1Score + ", location2Score=" + location2Score);
            if (location1Score < location2Score) {
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
        long timeSinceUpdate = System.currentTimeMillis() - location.getTime();
        Log.d(TAG, "time since updated " + timeSinceUpdate);
        if (timeSinceUpdate <= ACCEPTED_LOCATION_AGE_MILLIS 
                && location.getAccuracy() <= ACCEPTED_LOCATION_ACCURACY_METERS) {
            return true;
        }
        return false;
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "location changed " + location);
            if (shouldAcceptLocation(location)) {
                reportLocationFound(location);
                removeUpdates();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
    }
    
    
    /**
     * Helper that determine when we should stop check for {@link Location} updates.
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
            // Needed by interface.
        }        
    }

    /**
     *  
     */
    public interface MyLocationFoundListener {
        /**
         *  
         * @param location the location
         */
        public void onMyLocationFound(Location location);
    }
}
