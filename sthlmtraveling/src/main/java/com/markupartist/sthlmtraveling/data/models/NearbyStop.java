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

package com.markupartist.sthlmtraveling.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 *
 */
public class NearbyStop implements Parcelable {
    @SerializedName("site_id")
    private final int siteId;
    private final int distance;
    private final String name;
    private final String location; // lat,lon

    public NearbyStop(int siteId, int distance, String location, String name) {
        this.siteId = siteId;
        this.distance = distance;
        this.location = location;
        this.name = name;
    }

    protected NearbyStop(Parcel in) {
        siteId = in.readInt();
        distance = in.readInt();
        name = in.readString();
        location = in.readString();
    }

    public static final Creator<NearbyStop> CREATOR = new Creator<NearbyStop>() {
        @Override
        public NearbyStop createFromParcel(Parcel in) {
            return new NearbyStop(in);
        }

        @Override
        public NearbyStop[] newArray(int size) {
            return new NearbyStop[size];
        }
    };

    public int getDistance() {
        return distance;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public int getSiteId() {
        return siteId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(siteId);
        dest.writeInt(distance);
        dest.writeString(name);
        dest.writeString(location);
    }
}
