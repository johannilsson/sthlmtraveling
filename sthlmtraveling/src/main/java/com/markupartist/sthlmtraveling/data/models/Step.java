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

/**
 *
 */
public class Step implements Parcelable {
    private final int distance;
    private final String direction;
    private final String code;
    private final String wayName;
    private final int duration;
    private final int position;

    public Step(String code, int distance, String direction, String wayName, int duration, int position) {
        this.code = code;
        this.distance = distance;
        this.direction = direction;
        this.wayName = wayName;
        this.duration = duration;
        this.position = position;
    }

    protected Step(Parcel in) {
        distance = in.readInt();
        direction = in.readString();
        code = in.readString();
        wayName = in.readString();
        duration = in.readInt();
        position = in.readInt();
    }

    public static final Creator<Step> CREATOR = new Creator<Step>() {
        @Override
        public Step createFromParcel(Parcel in) {
            return new Step(in);
        }

        @Override
        public Step[] newArray(int size) {
            return new Step[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(distance);
        dest.writeString(direction);
        dest.writeString(code);
        dest.writeString(wayName);
        dest.writeInt(duration);
        dest.writeInt(position);
    }

    public String getDirection() {
        return direction;
    }

    public int getDistance() {
        return distance;
    }

    public int getDuration() {
        return duration;
    }

    public int getPosition() {
        return position;
    }

    public String getWayName() {
        return wayName;
    }

    public String getCode() {
        return code;
    }
}
