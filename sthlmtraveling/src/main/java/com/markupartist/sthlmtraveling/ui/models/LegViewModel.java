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

package com.markupartist.sthlmtraveling.ui.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.markupartist.sthlmtraveling.data.models.Leg;

public class LegViewModel implements Parcelable {
    public Leg leg;
    public boolean isExpanded;

    public LegViewModel(Leg leg) {
        this.leg = leg;
    }

    protected LegViewModel(Parcel in) {
        leg = in.readParcelable(Leg.class.getClassLoader());
        isExpanded = in.readByte() != 0;
    }

    public static final Creator<LegViewModel> CREATOR = new Creator<LegViewModel>() {
        @Override
        public LegViewModel createFromParcel(Parcel in) {
            return new LegViewModel(in);
        }

        @Override
        public LegViewModel[] newArray(int size) {
            return new LegViewModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(leg, flags);
        dest.writeByte((byte) (isExpanded ? 1 : 0));
    }
}