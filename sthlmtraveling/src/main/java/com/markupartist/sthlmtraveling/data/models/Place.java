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

package com.markupartist.sthlmtraveling.data.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.markupartist.sthlmtraveling.provider.site.Site;

/**
 *
 */
public class Place implements Parcelable {
    private final String id;
    private final String name;
    private final String type;
    private final double lat;
    private final double lon;
    private final int stopIndex;

    public Place(String id, String name, String type, double lat, double lon, int stopIndex) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.stopIndex = stopIndex;
    }

    protected Place(Parcel in) {
        id = in.readString();
        name = in.readString();
        type = in.readString();
        lat = in.readDouble();
        lon = in.readDouble();
        stopIndex = in.readInt();
    }

    public static final Creator<Place> CREATOR = new Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel in) {
            return new Place(in);
        }

        @Override
        public Place[] newArray(int size) {
            return new Place[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeDouble(lat);
        dest.writeDouble(lon);
        dest.writeInt(stopIndex);
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isMyLocation() {
        return name != null && name.equals(Site.TYPE_MY_LOCATION);
    }

    public boolean hasLocation() {
        return lat != 0 && lon != 0;
    }

    public int getStopIndex() {
        return stopIndex;
    }
}
