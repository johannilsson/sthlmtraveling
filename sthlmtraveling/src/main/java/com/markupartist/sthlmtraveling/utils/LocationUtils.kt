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

package com.markupartist.sthlmtraveling.utils

import android.location.Address
import android.location.Location
import android.util.Log

/**
 * Various location helpers.
 */
object LocationUtils {
    private const val TAG = "LocationUtils"

    /**
     * Simple formatting of an [Address].
     * @param address the address
     * @return formatted address.
     */
    @JvmStatic
    fun getAddressLine(address: Address): String {
        val sb = StringBuilder()
        if (address.getAddressLine(0) != null) {
            sb.append(address.getAddressLine(0))
        }
        if (address.getAddressLine(1) != null) {
            sb.append(" ").append(address.getAddressLine(1))
        }
        return sb.toString()
    }

    /**
     * Converts a string representation to [android.location.Location].
     * @param locationData the location (latitude,longitude)
     * @return android location.
     */
    @JvmStatic
    fun parseLocation(locationData: String): Location {
        val longAndLat = locationData.split(",")
        //Defualt to the world center, The Royal Palace
        var latitude = 59.3270053f
        var longitude = 18.0723166f
        try {
            latitude = longAndLat[0].toFloat()
            longitude = longAndLat[1].toFloat()
        } catch (e: NumberFormatException) {
            Log.e(TAG, e.toString())
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, e.toString())
        }

        val location = Location("sthlmtraveling")
        location.latitude = latitude.toDouble()
        location.longitude = longitude.toDouble()

        return location
    }
}
