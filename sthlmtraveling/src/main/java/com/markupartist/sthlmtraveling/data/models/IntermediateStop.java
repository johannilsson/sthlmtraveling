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
import android.support.v4.util.Pair;

import com.markupartist.sthlmtraveling.utils.DateTimeUtil;

import java.util.Date;

/**
 *
 */
public class IntermediateStop extends ParcelableBase {
    private final Place location;
    private final Date startTime;
    private Date startTimeRt;
    private final Date endTime;
    private Date endTimeRt;

    public IntermediateStop(Date endTimeRt, Place location, Date startTime, Date startTimeRt, Date endTime) {
        this.endTimeRt = endTimeRt;
        this.location = location;
        this.startTime = startTime;
        this.startTimeRt = startTimeRt;
        this.endTime = endTime;
    }

    protected IntermediateStop(Parcel in) {
        location = in.readParcelable(Place.class.getClassLoader());
        startTime = readDate(in);
        startTimeRt = readDate(in);
        endTime = readDate(in);
        endTimeRt = readDate(in);
    }

    public static final Creator<IntermediateStop> CREATOR = new Creator<IntermediateStop>() {
        @Override
        public IntermediateStop createFromParcel(Parcel in) {
            return new IntermediateStop(in);
        }

        @Override
        public IntermediateStop[] newArray(int size) {
            return new IntermediateStop[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
        writeDate(dest, startTime);
        writeDate(dest, startTimeRt);
        writeDate(dest, endTime);
        writeDate(dest, endTimeRt);
    }

    public Place getLocation() {
        return location;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Date getEndTimeRt() {
        return endTimeRt;
    }

    public Date getStartTimeRt() {
        return startTimeRt;
    }

    public Date getTime() {
        if (startTime != null) {
            return startTime;
        }
        return endTime;
    }

    public Date getTimeRt() {
        if (startTime != null) {
            return startTimeRt;
        }
        return endTimeRt;
    }

    public boolean hasDelay() {
        if (startTime != null) {
            return hasDepartureDelay();
        }
        return hasArrivalDelay();
    }

    public boolean hasDepartureDelay() {
        if (startTimeRt != null && startTime != null) {
            return startTimeRt.getTime() != startTime.getTime();
        }
        return false;
    }

    public boolean hasArrivalDelay() {
        if (endTimeRt != null && endTime != null) {
            return endTimeRt.getTime() != endTime.getTime();
        }
        return false;
    }

    public int startTimeDelay() {
        return DateTimeUtil.getDelay(startTime, startTimeRt);
    }

    public int endTimeDelay() {
        return DateTimeUtil.getDelay(endTime, endTimeRt);
    }

    RealTimeState realTimeStateFromDelay(int delay) {
        if (delay > 0) {
            return RealTimeState.BEHIND_SCHEDULE;
        } else if (delay < 0) {
            return RealTimeState.AHEAD_OF_SCHEDULE;
        }
        return RealTimeState.ON_TIME;
    }

    public Pair<Integer, RealTimeState> delay() {
        if (startTime != null && startTimeRt != null) {
            int delay = startTimeDelay();
            return Pair.create(delay, realTimeStateFromDelay(delay));
        } else if (endTime != null && endTimeRt != null) {
            int delay = endTimeDelay();
            return Pair.create(delay, realTimeStateFromDelay(delay));
        }
        return Pair.create(0, RealTimeState.NOT_SET);
    }

    public void setStartTimeRt(Date startTimeRt) {
        this.startTimeRt = startTimeRt;
    }

    public void setEndTimeRt(Date endTimeRt) {
        this.endTimeRt = endTimeRt;
    }
}
