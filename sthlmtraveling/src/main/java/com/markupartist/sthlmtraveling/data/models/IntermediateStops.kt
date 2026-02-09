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
class IntermediateStops : ParcelableBase {
    @JvmField
    val reference: String?
    @JvmField
    val stops: MutableList<IntermediateStop?>?

    constructor(reference: String?, stops: MutableList<IntermediateStop?>?) {
        this.reference = reference
        this.stops = stops
    }

    protected constructor(`in`: Parcel) {
        reference = `in`.readString()
        stops = `in`.createTypedArrayList<IntermediateStop?>(IntermediateStop.CREATOR)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(reference)
        dest.writeTypedList<IntermediateStop?>(stops)
    }

    fun hasStops(): Boolean {
        return stops != null && !stops.isEmpty()
    }

    companion object {
        val CREATOR: Parcelable.Creator<IntermediateStops?> =
            object : Parcelable.Creator<IntermediateStops?> {
                override fun createFromParcel(`in`: Parcel): IntermediateStops {
                    return IntermediateStops(`in`)
                }

                override fun newArray(size: Int): Array<IntermediateStops?> {
                    return arrayOfNulls<IntermediateStops>(size)
                }
            }
    }
}
