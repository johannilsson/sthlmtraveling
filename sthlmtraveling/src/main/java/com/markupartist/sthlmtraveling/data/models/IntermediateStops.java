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

import java.util.List;

/**
 *
 */
public class IntermediateStops extends ParcelableBase {
    private final String reference;
    private final List<IntermediateStop> stops;

    public IntermediateStops(String reference, List<IntermediateStop> stops) {
        this.reference = reference;
        this.stops = stops;
    }

    protected IntermediateStops(Parcel in) {
        reference = in.readString();
        stops = in.createTypedArrayList(IntermediateStop.CREATOR);
    }

    public static final Creator<IntermediateStops> CREATOR = new Creator<IntermediateStops>() {
        @Override
        public IntermediateStops createFromParcel(Parcel in) {
            return new IntermediateStops(in);
        }

        @Override
        public IntermediateStops[] newArray(int size) {
            return new IntermediateStops[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reference);
        dest.writeTypedList(stops);
    }

    public boolean hasStops() {
        return stops != null && !stops.isEmpty();
    }

    public List<IntermediateStop> getStops() {
        return stops;
    }

    public String getReference() {
        return reference;
    }
}
