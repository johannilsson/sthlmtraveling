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
import com.markupartist.sthlmtraveling.provider.site.Site

/**
 *
 */
class Place : Parcelable {
    @JvmField
    val id: String?
    @JvmField
    val name: String?
    @JvmField
    val type: String?
    @JvmField
    val lat: Double
    @JvmField
    val lon: Double
    @JvmField
    val stopIndex: Int
    @JvmField
    val track: String?
    @JvmField
    val entrances: MutableList<Entrance?>?

    constructor(
        id: String?, name: String?, type: String?, lat: Double, lon: Double,
        stopIndex: Int, track: String?, entrances: MutableList<Entrance?>?
    ) {
        this.id = id
        this.name = name
        this.type = type
        this.lat = lat
        this.lon = lon
        this.stopIndex = stopIndex
        this.track = track
        this.entrances = entrances
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readString()
        name = `in`.readString()
        type = `in`.readString()
        lat = `in`.readDouble()
        lon = `in`.readDouble()
        stopIndex = `in`.readInt()
        track = `in`.readString()
        entrances = `in`.createTypedArrayList<Entrance?>(Entrance.CREATOR)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeString(type)
        dest.writeDouble(lat)
        dest.writeDouble(lon)
        dest.writeInt(stopIndex)
        dest.writeString(track)
        dest.writeTypedList<Entrance?>(entrances)
    }

    val isMyLocation: Boolean
        get() = name != null && name == Site.TYPE_MY_LOCATION

    fun hasLocation(): Boolean {
        // Sorry Null Island.
        return lat != 0.0 && lon != 0.0
    }

    fun hasEntrances(): Boolean {
        return entrances != null && !entrances.isEmpty()
    }

    fun looksEquals(other: Place): Boolean {
        if (id != null && other.id != null) {
            return id == other.id
        } else if (stopIndex == other.stopIndex) {
            return true
        } else if (name != null && other.name != null) {
            return name == other.name
        }
        return false
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Place> = object : Parcelable.Creator<Place> {
            override fun createFromParcel(`in`: Parcel): Place {
                return Place(`in`)
            }

            override fun newArray(size: Int): Array<Place?> {
                return arrayOfNulls(size)
            }
        }
    }
}
