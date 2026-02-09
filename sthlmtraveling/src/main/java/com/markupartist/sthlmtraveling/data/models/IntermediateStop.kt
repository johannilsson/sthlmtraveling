/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
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
import androidx.core.util.Pair
import com.markupartist.sthlmtraveling.utils.DateTimeUtil
import java.util.Date

/**
 *
 */
class IntermediateStop : ParcelableBase {
    @JvmField
    val location: Place?
    private val startTime: Date?
    var startTimeRt: Date?
    val endTime: Date?
    var endTimeRt: Date?

    constructor(
        endTimeRt: Date?,
        location: Place?,
        startTime: Date?,
        startTimeRt: Date?,
        endTime: Date?
    ) {
        this.endTimeRt = endTimeRt
        this.location = location
        this.startTime = startTime
        this.startTimeRt = startTimeRt
        this.endTime = endTime
    }

    protected constructor(`in`: Parcel) {
        location = `in`.readParcelable<Place?>(Place::class.java.getClassLoader())
        startTime = readDate(`in`)
        startTimeRt = readDate(`in`)
        endTime = readDate(`in`)
        endTimeRt = readDate(`in`)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(location, flags)
        writeDate(dest, startTime)
        writeDate(dest, startTimeRt)
        writeDate(dest, endTime)
        writeDate(dest, endTimeRt)
    }

    val time: Date?
        get() {
            if (startTime != null) {
                return startTime
            }
            return endTime
        }

    val timeRt: Date?
        get() {
            if (startTime != null) {
                return startTimeRt
            }
            return endTimeRt
        }

    fun hasDelay(): Boolean {
        if (startTime != null) {
            return hasDepartureDelay()
        }
        return hasArrivalDelay()
    }

    fun hasDepartureDelay(): Boolean {
        if (startTimeRt != null && startTime != null) {
            return true
        }
        return false
    }

    fun hasArrivalDelay(): Boolean {
        if (endTimeRt != null && endTime != null) {
            return true
        }
        return false
    }

    fun startTimeDelay(): Int {
        return DateTimeUtil.getDelay(startTime, startTimeRt)
    }

    fun endTimeDelay(): Int {
        return DateTimeUtil.getDelay(endTime, endTimeRt)
    }

    fun delay(): Pair<Int?, RealTimeState?> {
        if (startTime != null && startTimeRt != null) {
            val delay = startTimeDelay()
            return Pair.create<Int?, RealTimeState?>(
                delay,
                DateTimeUtil.getRealTimeStateFromDelay(delay)
            )
        } else if (endTime != null && endTimeRt != null) {
            val delay = endTimeDelay()
            return Pair.create<Int?, RealTimeState?>(
                delay,
                DateTimeUtil.getRealTimeStateFromDelay(delay)
            )
        }
        return Pair.create<Int?, RealTimeState?>(0, RealTimeState.NOT_SET)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<IntermediateStop?> =
            object : Parcelable.Creator<IntermediateStop?> {
                override fun createFromParcel(`in`: Parcel): IntermediateStop {
                    return IntermediateStop(`in`)
                }

                override fun newArray(size: Int): Array<IntermediateStop?> {
                    return arrayOfNulls<IntermediateStop>(size)
                }
            }
    }
}
