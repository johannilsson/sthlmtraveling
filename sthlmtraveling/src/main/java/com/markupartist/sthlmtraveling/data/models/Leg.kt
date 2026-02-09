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

import android.os.Parcel
import android.os.Parcelable
import java.util.Date

/**
 *
 */
class Leg : ParcelableBase {
    @JvmField
    val from: Place?
    @JvmField
    val to: Place?
    @JvmField
    val geometry: String?
    @JvmField
    val distance: Int
    @JvmField
    val duration: Int
    @JvmField
    val steps: MutableList<Step?>?
    @JvmField
    val travelMode: String?
    @JvmField
    val headsign: Place?
    @JvmField
    val routeName: String?
    @JvmField
    val routeShortName: String?
    @JvmField
    val routeColor: String?
    @JvmField
    val startTime: Date?
    @JvmField
    val endTime: Date?
    var startTimeRt: Date?
        private set
    var endTimeRt: Date?
        private set
    private var departureDelay: Int
    private var arrivalDelay: Int
    var isRealTime: Boolean
        private set
    private val cancelled: Boolean
    private val unreachable: Boolean
    private val invalid: Boolean
    private val alternative: Boolean
    @JvmField
    val alerts: MutableList<Alert?>?
    @JvmField
    val notes: MutableList<Alert?>?
    private val geometryRef: String?
    @JvmField
    val detailRef: String?
    private var intermediateStops: MutableList<IntermediateStop>? = null
    private var updatedAtMillis: Long = 0

    constructor(
        alerts: MutableList<Alert?>?, from: Place?, to: Place?, geometry: String?, distance: Int,
        duration: Int, steps: MutableList<Step?>?, travelMode: String?, headsign: Place?,
        routeName: String?, routeShortName: String?, routeColor: String?, startTime: Date?,
        endTime: Date?, startTimeRt: Date?, endTimeRt: Date?, departureDelay: Int,
        arrivalDelay: Int, realTime: Boolean, cancelled: Boolean, unreachable: Boolean,
        invalid: Boolean, alternative: Boolean, notes: MutableList<Alert?>?,
        geometryRef: String?, detailRef: String?
    ) {
        this.alerts = alerts
        this.from = from
        this.to = to
        this.geometry = geometry
        this.distance = distance
        this.duration = duration
        this.steps = steps
        this.travelMode = travelMode
        this.headsign = headsign
        this.routeName = routeName
        this.routeShortName = routeShortName
        this.routeColor = routeColor
        this.startTime = startTime
        this.endTime = endTime
        this.startTimeRt = startTimeRt
        this.endTimeRt = endTimeRt
        this.departureDelay = departureDelay
        this.arrivalDelay = arrivalDelay
        this.isRealTime = realTime
        this.cancelled = cancelled
        this.unreachable = unreachable
        this.invalid = invalid
        this.alternative = alternative
        this.notes = notes
        this.geometryRef = geometryRef
        this.detailRef = detailRef
    }

    protected constructor(`in`: Parcel) {
        from = `in`.readParcelable<Place?>(Place::class.java.getClassLoader())
        to = `in`.readParcelable<Place?>(Place::class.java.getClassLoader())
        geometry = `in`.readString()
        distance = `in`.readInt()
        duration = `in`.readInt()
        steps = `in`.createTypedArrayList<Step?>(Step.CREATOR)
        travelMode = `in`.readString()
        headsign = `in`.readParcelable<Place?>(Place::class.java.getClassLoader())
        routeName = `in`.readString()
        routeShortName = `in`.readString()
        routeColor = `in`.readString()
        startTime = readDate(`in`)
        endTime = readDate(`in`)
        startTimeRt = readDate(`in`)
        endTimeRt = readDate(`in`)
        departureDelay = `in`.readInt()
        arrivalDelay = `in`.readInt()
        this.isRealTime = `in`.readInt() == 1
        cancelled = `in`.readInt() == 1
        unreachable = `in`.readInt() == 1
        invalid = `in`.readInt() == 1
        alternative = `in`.readInt() == 1
        alerts = `in`.createTypedArrayList<Alert?>(Alert.CREATOR)
        notes = `in`.createTypedArrayList<Alert?>(Alert.CREATOR)
        geometryRef = `in`.readString()
        detailRef = `in`.readString()
        intermediateStops = `in`.createTypedArrayList<IntermediateStop?>(IntermediateStop.CREATOR)
        updatedAtMillis = `in`.readLong()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(from, flags)
        dest.writeParcelable(to, flags)
        dest.writeString(geometry)
        dest.writeInt(distance)
        dest.writeInt(duration)
        dest.writeTypedList<Step?>(steps)
        dest.writeString(travelMode)
        dest.writeParcelable(headsign, flags)
        dest.writeString(routeName)
        dest.writeString(routeShortName)
        dest.writeString(routeColor)
        dest.writeLong(if (startTime != null) startTime.getTime() else -1)
        dest.writeLong(if (endTime != null) endTime.getTime() else -1)
        dest.writeLong(if (startTimeRt != null) startTimeRt!!.getTime() else -1)
        dest.writeLong(if (endTimeRt != null) endTimeRt!!.getTime() else -1)
        dest.writeInt(departureDelay)
        dest.writeInt(arrivalDelay)
        dest.writeInt(if (this.isRealTime) 1 else 0)
        dest.writeInt(if (cancelled) 1 else 0)
        dest.writeInt(if (unreachable) 1 else 0)
        dest.writeInt(if (invalid) 1 else 0)
        dest.writeInt(if (alternative) 1 else 0)
        dest.writeTypedList<Alert?>(alerts)
        dest.writeTypedList<Alert?>(notes)
        dest.writeString(geometryRef)
        dest.writeString(detailRef)
        dest.writeTypedList<IntermediateStop?>(intermediateStops)
        dest.writeLong(updatedAtMillis)
    }

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

    companion object {
        val CREATOR: Parcelable.Creator<Leg?> = object : Parcelable.Creator<Leg?> {
            override fun createFromParcel(`in`: Parcel): Leg {
                return Leg(`in`)
            }

            override fun newArray(size: Int): Array<Leg?> {
                return arrayOfNulls<Leg>(size)
            }
        }
    }
}
