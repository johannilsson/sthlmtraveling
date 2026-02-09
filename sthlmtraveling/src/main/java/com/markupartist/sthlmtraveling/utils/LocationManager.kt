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

package com.markupartist.sthlmtraveling.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Created by johan on 4/5/14.
 */
class LocationManager(
    private val context: Context,
    private val googleApiClient: GoogleApiClient?
) : PlayService, LocationListener {

    private val locationRequestTimeOut = LocationRequestTimeOut()
    private var locationFoundListener: LocationFoundListener? = null
    private var locationRequested = false
    private var highAccuracy = true

    fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = if (highAccuracy)
            LocationRequest.PRIORITY_HIGH_ACCURACY
        else
            LocationRequest.PRIORITY_LOW_POWER
        locationRequest.interval = UPDATE_INTERVAL
        locationRequest.fastestInterval = FASTEST_INTERVAL
        locationRequest.setExpirationDuration(ACCEPTED_LOCATION_AGE_MILLIS.toLong())
        locationRequest.numUpdates = 5

        return locationRequest
    }

    fun setLocationListener(listener: LocationFoundListener?) {
        locationFoundListener = listener
    }

    fun getLastLocation(): Location? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return googleApiClient?.let {
            LocationServices.FusedLocationApi.getLastLocation(it)
        }
    }

    /**
     * Is the provided [Location] accepted?
     *
     * @param location the location
     * @return `true` if accepted `false` otherwise
     */
    fun isLocationAcceptable(location: Location?): Boolean {
        if (location == null) {
            return false
        }
        if (!highAccuracy) {
            return true
        }
        val timeSinceUpdate = System.currentTimeMillis() - location.time
        return timeSinceUpdate <= ACCEPTED_LOCATION_AGE_MILLIS && location.accuracy <= ACCEPTED_LOCATION_ACCURACY_METERS
    }

    fun onStart() {
        // No-op
    }

    override fun onStop() {
        removeUpdates()
    }

    fun resumed() {
        // No-op
    }

    fun paused() {
        // No-op
    }

    fun reportLocationFound(location: Location?) {
        locationFoundListener?.onMyLocationFound(location)
    }

    fun setAccuracy(high: Boolean) {
        highAccuracy = high
    }

    fun requestLocation() {
        if (googleApiClient == null) {
            return
        }
        if (!googleApiClient.isConnected) {
            locationRequested = true
        } else {
            requestLocationOrDeliverCurrent()
        }
    }

    fun requestLocationOrDeliverCurrent() {
        val client = googleApiClient ?: return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val location = getLastLocation()
        if (location != null && isLocationAcceptable(location)) {
            onLocationChanged(location)
        } else {
            locationRequestTimeOut.start()
            LocationServices.FusedLocationApi.requestLocationUpdates(
                client, createLocationRequest(), this
            )
        }
    }

    fun removeUpdates() {
        if (googleApiClient == null) {
            return
        }

        locationRequestTimeOut.cancel()
        if (googleApiClient.isConnected) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        if (isLocationAcceptable(location)) {
            removeUpdates()
            reportLocationFound(location)
        }
    }

    override fun onConnected() {
        if (locationRequested) {
            locationRequested = false
            requestLocationOrDeliverCurrent()
        }
    }

    /**
     * Used to determine when we should stop search for location changes.
     */
    private inner class LocationRequestTimeOut : CountDownTimer(FIND_LOCATION_TIME_OUT_MILLIS, 1000) {
        override fun onFinish() {
            reportLocationFound(getLastLocation())
            // Once we have reported a location, remove the need to search for more.
            removeUpdates()
        }

        override fun onTick(millisUntilFinished: Long) {
            // Needed by the interface.
        }
    }

    /**
     *
     */
    interface LocationFoundListener {
        /**
         * Called when a location is found.
         *
         * @param location the location, could be null
         */
        fun onMyLocationFound(location: Location?)
    }

    companion object {
        // Timeout for finding
        private const val FIND_LOCATION_TIME_OUT_MILLIS = 8000L
        // Accepted minimum location accuracy.
        private const val ACCEPTED_LOCATION_ACCURACY_METERS = 300
        // Accepted max age of an location
        private const val ACCEPTED_LOCATION_AGE_MILLIS = 60000
        // Milliseconds per second
        private const val MILLISECONDS_PER_SECOND = 1000
        // Update frequency in seconds
        const val UPDATE_INTERVAL_IN_SECONDS = 5
        // Update frequency in milliseconds
        private val UPDATE_INTERVAL = (MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS).toLong()
        // The fastest update frequency, in seconds
        private const val FASTEST_INTERVAL_IN_SECONDS = 1
        // A fast frequency ceiling in milliseconds
        private val FASTEST_INTERVAL = (MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS).toLong()
    }
}
