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

/**
 *
 */
class Step : Parcelable {
    val distance: Int
    val direction: String?
    @JvmField
    val code: String?
    val wayName: String?
    val duration: Int
    @JvmField
    val position: Int

    constructor(
        code: String?,
        distance: Int,
        direction: String?,
        wayName: String?,
        duration: Int,
        position: Int
    ) {
        this.code = code
        this.distance = distance
        this.direction = direction
        this.wayName = wayName
        this.duration = duration
        this.position = position
    }

    protected constructor(`in`: Parcel) {
        distance = `in`.readInt()
        direction = `in`.readString()
        code = `in`.readString()
        wayName = `in`.readString()
        duration = `in`.readInt()
        position = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(distance)
        dest.writeString(direction)
        dest.writeString(code)
        dest.writeString(wayName)
        dest.writeInt(duration)
        dest.writeInt(position)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Step?> = object : Parcelable.Creator<Step?> {
            override fun createFromParcel(`in`: Parcel): Step {
                return Step(`in`)
            }

            override fun newArray(size: Int): Array<Step?> {
                return arrayOfNulls<Step>(size)
            }
        }
    }
}
