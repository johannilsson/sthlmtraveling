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
class Alert : Parcelable {
    @JvmField
    val header: String?
    @JvmField
    val description: String?
    val language: String?

    constructor(description: String?, header: String?, language: String?) {
        this.description = description
        this.header = header
        this.language = language
    }

    protected constructor(`in`: Parcel) {
        header = `in`.readString()
        description = `in`.readString()
        language = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(header)
        dest.writeString(description)
        dest.writeString(language)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Alert?> = object : Parcelable.Creator<Alert?> {
            override fun createFromParcel(`in`: Parcel): Alert {
                return Alert(`in`)
            }

            override fun newArray(size: Int): Array<Alert?> {
                return arrayOfNulls<Alert>(size)
            }
        }
    }
}
