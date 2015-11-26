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
public class Leg implements Parcelable {
    private final Place from;
    private final Place to;
    private final String geometry;
    private final int distance;
    private final int duration;
    private final List<Step> steps;


    public Leg(Place from, Place to, String geometry, int distance, int duration, List<Step> steps) {
        this.from = from;
        this.to = to;
        this.geometry = geometry;
        this.distance = distance;
        this.duration = duration;
        this.steps = steps;
    }

    protected Leg(Parcel in) {
        from = in.readParcelable(Place.class.getClassLoader());
        to = in.readParcelable(Place.class.getClassLoader());
        geometry = in.readString();
        distance = in.readInt();
        duration = in.readInt();
        steps = new ArrayList<>();
        in.readTypedList(steps, Step.CREATOR);
    }

    public static final Creator<Leg> CREATOR = new Creator<Leg>() {
        @Override
        public Leg createFromParcel(Parcel in) {
            return new Leg(in);
        }

        @Override
        public Leg[] newArray(int size) {
            return new Leg[size];
        }
    };

    public int getDistance() {
        return distance;
    }

    public int getDuration() {
        return duration;
    }

    public Place getFrom() {
        return from;
    }

    public String getGeometry() {
        return geometry;
    }

    public Place getTo() {
        return to;
    }

    public List<Step> getSteps() {
        return steps;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(from, 0);
        dest.writeParcelable(to, 0);
        dest.writeString(geometry);
        dest.writeInt(distance);
        dest.writeInt(duration);
        dest.writeTypedList(steps);
    }
}
