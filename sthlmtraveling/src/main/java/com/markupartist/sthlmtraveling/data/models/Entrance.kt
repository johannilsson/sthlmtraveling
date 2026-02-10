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

/**
 *
 */
class Entrance : Parcelable {
    private val id: String?
    @JvmField
    val name: String?
    @JvmField
    val lat: Double
    @JvmField
    val lon: Double

    constructor(id: String?, name: String?, lat: Double, lon: Double) {
        this.id = id
        this.name = name
        this.lat = lat
        this.lon = lon
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readString()
        name = `in`.readString()
        lat = `in`.readDouble()
        lon = `in`.readDouble()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(name)
        dest.writeDouble(lat)
        dest.writeDouble(lon)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Entrance?> = object : Parcelable.Creator<Entrance?> {
            override fun createFromParcel(`in`: Parcel): Entrance {
                return Entrance(`in`)
            }

            override fun newArray(size: Int): Array<Entrance?> {
                return arrayOfNulls<Entrance>(size)
            }
        }
    }
}
