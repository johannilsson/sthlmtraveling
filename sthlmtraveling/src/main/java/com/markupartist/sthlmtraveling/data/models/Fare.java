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

import java.util.List;

/**
 *
 */
public class Fare implements Parcelable {
    private final String zones;
    private final List<FareAttribute> attributes;

    public Fare(String zones, List<FareAttribute> attributes) {
        this.zones = zones;
        this.attributes = attributes;
    }

    protected Fare(Parcel in) {
        zones = in.readString();
        attributes = in.createTypedArrayList(FareAttribute.CREATOR);
    }

    public static final Creator<Fare> CREATOR = new Creator<Fare>() {
        @Override
        public Fare createFromParcel(Parcel in) {
            return new Fare(in);
        }

        @Override
        public Fare[] newArray(int size) {
            return new Fare[size];
        }
    };

    public String getZones() {
        return zones;
    }

    public List<FareAttribute> getAttributes() {
        return attributes;
    }

    public boolean canBuyTicket() {
        return zones != null && zones.equals("SL");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(zones);
        dest.writeTypedList(attributes);
    }
}
