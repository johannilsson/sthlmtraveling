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

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Route implements Parcelable {
    private final int duration;
    private final List<Leg> legs;
    private final String mode;

    public Route(int duration, List<Leg> legs, String mode) {
        this.duration = duration;
        this.legs = legs;
        this.mode = mode;
    }

    protected Route(Parcel in) {
        duration = in.readInt();
        legs = new ArrayList<>();
        in.readTypedList(legs, Leg.CREATOR);
        mode = in.readString();
    }

    public static final Creator<Route> CREATOR = new Creator<Route>() {
        @Override
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        @Override
        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

    public int getDuration() {
        return duration;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(duration);
        dest.writeTypedList(legs);
        dest.writeString(mode);
    }
}
