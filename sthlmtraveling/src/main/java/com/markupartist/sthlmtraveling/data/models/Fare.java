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

/**
 *
 */
public class Fare implements Parcelable {
    private final String zones;

    public Fare(String zones) {
        this.zones = zones;
    }

    protected Fare(Parcel in) {
        zones = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(zones);
    }

    public boolean canBuyTicket() {
        return zones != null && (
                zones.equals("A") ||
                zones.equals("B") ||
                zones.equals("C") ||
                zones.equals("AB") ||
                zones.equals("BC") ||
                zones.equals("ABC"));
    }
}
