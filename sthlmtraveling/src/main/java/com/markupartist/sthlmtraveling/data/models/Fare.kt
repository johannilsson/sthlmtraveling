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
class Fare : Parcelable {
    @JvmField
    val zones: String?
    @JvmField
    val attributes: MutableList<FareAttribute?>?

    constructor(zones: String?, attributes: MutableList<FareAttribute?>?) {
        this.zones = zones
        this.attributes = attributes
    }

    protected constructor(`in`: Parcel) {
        zones = `in`.readString()
        attributes = `in`.createTypedArrayList<FareAttribute?>(FareAttribute.CREATOR)
    }

    fun canBuyTicket(): Boolean {
        return zones != null && zones == "SL"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(zones)
        dest.writeTypedList<FareAttribute?>(attributes)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Fare?> = object : Parcelable.Creator<Fare?> {
            override fun createFromParcel(`in`: Parcel): Fare {
                return Fare(`in`)
            }

            override fun newArray(size: Int): Array<Fare?> {
                return arrayOfNulls<Fare>(size)
            }
        }
    }
}
