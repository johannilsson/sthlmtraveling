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

package com.markupartist.sthlmtraveling.utils;

import android.location.Address;
import android.location.Location;
import android.util.Log;

/**
 * Various location helpers.
 */
public class LocationUtils {

    private static final String TAG = "LocationUtils";
    /**
     * Simple formatting of an {@link Address}.
     * @param address the address
     * @return formatted address.
     */
    public static String getAddressLine(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getAddressLine(0) != null) {
            sb.append(address.getAddressLine(0));
        }
        if (address.getAddressLine(1) != null) {
            sb.append(" ").append(address.getAddressLine(1));
        }
        return sb.toString();
    }

    /**
     * Converts a string representation to {@link android.location.Location}.
     * @param locationData the location (latitude,longitude)
     * @return android location.
     */
    public static Location parseLocation(String locationData) {
        String[] longAndLat = locationData.split(",");
        //Defualt to the world center, The Royal Palace
        float latitude = 59.3270053f;
        float longitude = 18.0723166f;
        try {
            latitude = Float.parseFloat(longAndLat[0]);
            longitude = Float.parseFloat(longAndLat[1]);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, nfe.toString());
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Log.e(TAG, aioobe.toString());
        }

        Location location = new Location("sthlmtraveling");
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        return location;
    }

}
