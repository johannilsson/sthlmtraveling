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
package com.markupartist.sthlmtraveling.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 *
 */
@Parcelize
data class Leg(
    @JvmField val from: Place?,
    @JvmField val to: Place?,
    @JvmField val geometry: String?,
    @JvmField val distance: Int,
    @JvmField val duration: Int,
    @JvmField val steps: MutableList<Step?>?,
    @JvmField val travelMode: String?,
    @JvmField val headsign: Place?,
    @JvmField val routeName: String?,
    @JvmField val routeShortName: String?,
    @JvmField val routeColor: String?,
    @JvmField val startTime: Date?,
    @JvmField val endTime: Date?,
    var startTimeRt: Date?,
    var endTimeRt: Date?,
    private var departureDelay: Int,
    private var arrivalDelay: Int,
    var isRealTime: Boolean,
    private val cancelled: Boolean,
    private val unreachable: Boolean,
    private val invalid: Boolean,
    private val alternative: Boolean,
    @JvmField val alerts: MutableList<Alert?>?,
    @JvmField val notes: MutableList<Alert?>?,
    private val geometryRef: String?,
    @JvmField val detailRef: String?,
    private var intermediateStops: MutableList<IntermediateStop>? = null,
    private var updatedAtMillis: Long = 0
) : Parcelable {

    fun departsAt(): Date? {
        if (startTimeRt != null) {
            return startTimeRt
        }
        return startTime
    }

    fun arrivesAt(): Date? {
        if (endTimeRt != null) {
            return endTimeRt
        }
        return endTime
    }

    fun hasAlerts(): Boolean {
        return alerts != null && alerts.size > 0
    }

    fun hasNotes(): Boolean {
        return notes != null && notes.size > 0
    }

    val isTransit: Boolean
        get() = "foot" != travelMode

    fun hasDepartureDelay(): Boolean {
        return departureDelay != 0
    }

    fun hasArrivalDelay(): Boolean {
        return arrivalDelay != 0
    }

    fun isOnTime(isDeparture: Boolean): Boolean {
        val delay = if (isDeparture) departureDelay else arrivalDelay
        return this.isRealTime && delay == 0
    }

    fun isLate(isDeparture: Boolean): Boolean {
        val delay = if (isDeparture) departureDelay else arrivalDelay
        return this.isRealTime && delay > 0
    }

    fun isAhead(isDeparture: Boolean): Boolean {
        val delay = if (isDeparture) departureDelay else arrivalDelay
        return this.isRealTime && delay < 0
    }

    fun hasStopIndex(stopIndex: Int): Boolean {
        return this.from!!.stopIndex != stopIndex
                && this.to!!.stopIndex != stopIndex
    }

    fun getIntermediateStops(): MutableList<IntermediateStop> {
        if (intermediateStops == null) {
            intermediateStops = ArrayList<IntermediateStop>()
        }
        return intermediateStops!!
    }

    fun setIntermediateStops(intermediateStops: MutableList<IntermediateStop>?) {
        this.intermediateStops = intermediateStops
    }

    fun realtimeState(isDeparture: Boolean): RealTimeState {
        if (isOnTime(isDeparture)) {
            return RealTimeState.ON_TIME
        } else if (isAhead(isDeparture)) {
            return RealTimeState.AHEAD_OF_SCHEDULE
        } else if (isLate(isDeparture)) {
            return RealTimeState.BEHIND_SCHEDULE
        }
        return RealTimeState.NOT_SET
    }

    fun updateTimes(stopTimes: MutableList<IntermediateStop>): Boolean {
        var updated = false

        // If intermediate stops is missing, attempt to add them.
        if (getIntermediateStops().isEmpty()) {
            var addStopTime = false
            for (stopTime in stopTimes) {
                // Add all stop times that is between start and end.
                if (stopTime.location?.looksEquals(this.to!!) == true) {
                    addStopTime = false
                }
                if (addStopTime) {
                    intermediateStops!!.add(stopTime)
                }
                if (stopTime.location?.looksEquals(this.from!!) == true) {
                    addStopTime = true
                }
            }
        }

        for (stopTime in stopTimes) {
            // Check from if we match update start time
            if (stopTime.location?.looksEquals(this.from!!) == true) {
                startTimeRt = stopTime.startTimeRt
                if (stopTime.hasDepartureDelay()) {
                    this.isRealTime = true
                    departureDelay = stopTime.startTimeDelay()
                } else {
                    this.isRealTime = false
                    departureDelay = 0
                }
                updated = true
            } else if (stopTime.location?.looksEquals(this.to!!) == true) {
                endTimeRt = stopTime.endTimeRt
                if (stopTime.hasArrivalDelay()) {
                    this.isRealTime = true
                    arrivalDelay = stopTime.endTimeDelay()
                } else {
                    this.isRealTime = false
                    arrivalDelay = 0
                }
                updated = true
            }

            val currentIntermediateStops = intermediateStops
            if (currentIntermediateStops != null && !currentIntermediateStops.isEmpty()) {
                for (intermediateStop in currentIntermediateStops) {
                    val intLocation = intermediateStop.location
                    if (intLocation != null && stopTime.location?.looksEquals(intLocation) == true) {
                        intermediateStop.startTimeRt = stopTime.startTimeRt
                        intermediateStop.endTimeRt = stopTime.endTimeRt
                        updated = true
                    }
                }
            }
        }
        if (updated) {
            updatedAtMillis = System.currentTimeMillis()
        }
        return updated
    }

    /**
     * Checks if this leg is considered to be active.
     *
     * @param timeMillis
     * @return
     */
    fun shouldRefresh(timeMillis: Long): Boolean {
        // Do we have real-time?
        if (this.isRealTime) {
            return true
        }
        // 1 hour before departure, 30 minutes after arrival.
        if (departsAt()!!.getTime() - 3600000 > timeMillis
            && arrivesAt()!!.getTime() + 1800000 < timeMillis
        ) {
            return true
        }
        // If we haven't been refreshed in an hour.
        if (updatedAtMillis != 0L && updatedAtMillis < timeMillis - 3600000) {
            return true
        }
        return false
    }
}
