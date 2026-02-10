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
import com.google.gson.annotations.SerializedName

/**
 *
 */
class NearbyStop : Parcelable {
    @JvmField
    @SerializedName("site_id")
    val siteId: Int
    @JvmField
    val distance: Int
    @JvmField
    val name: String?
    @JvmField
    val location: String? // lat,lon

    constructor(siteId: Int, distance: Int, location: String?, name: String?) {
        this.siteId = siteId
        this.distance = distance
        this.location = location
        this.name = name
    }

    protected constructor(`in`: Parcel) {
        siteId = `in`.readInt()
        distance = `in`.readInt()
        name = `in`.readString()
        location = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(siteId)
        dest.writeInt(distance)
        dest.writeString(name)
        dest.writeString(location)
    }

    companion object {
        val CREATOR: Parcelable.Creator<NearbyStop?> = object : Parcelable.Creator<NearbyStop?> {
            override fun createFromParcel(`in`: Parcel): NearbyStop {
                return NearbyStop(`in`)
            }

            override fun newArray(size: Int): Array<NearbyStop?> {
                return arrayOfNulls<NearbyStop>(size)
            }
        }
    }
}
