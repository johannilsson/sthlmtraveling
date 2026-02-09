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
class FareAttribute : Parcelable {
    val isReduced: Boolean
    @JvmField
    val text: String?
    @JvmField
    val action: String?

    constructor(action: String?, reduced: Boolean, text: String?) {
        this.action = action
        this.isReduced = reduced
        this.text = text
    }

    protected constructor(`in`: Parcel) {
        this.isReduced = `in`.readByte().toInt() != 0
        text = `in`.readString()
        action = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte((if (this.isReduced) 1 else 0).toByte())
        dest.writeString(text)
        dest.writeString(action)
    }

    companion object {
        val CREATOR: Parcelable.Creator<FareAttribute?> =
            object : Parcelable.Creator<FareAttribute?> {
                override fun createFromParcel(`in`: Parcel): FareAttribute {
                    return FareAttribute(`in`)
                }

                override fun newArray(size: Int): Array<FareAttribute?> {
                    return arrayOfNulls<FareAttribute>(size)
                }
            }
    }
}
