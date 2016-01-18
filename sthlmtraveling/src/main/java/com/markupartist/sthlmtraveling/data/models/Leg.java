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
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class Leg extends ParcelableBase {
    private final Place from;
    private final Place to;
    private final String geometry;
    private final int distance;
    private final int duration;
    private final List<Step> steps;
    private final String travelMode;
    private final Place headsign;
    private final String routeName;
    private final String routeShortName;
    private final String routeColor;
    private final Date startTime;
    private final Date endTime;
    private final Date startTimeRt;
    private final Date endTimeRt;
    private final int departureDelay;
    private final int arrivalDelay;
    private final boolean realTime;
    private final boolean cancelled;
    private final boolean unreachable;
    private final boolean invalid;
    private final boolean alternative;
    private final List<Alert> alerts;
    private final List<Alert> notes;
    private final String geometryRef;
    private final String detailRef;
    private List<IntermediateStop> intermediateStops;

    public Leg(List<Alert> alerts, Place from, Place to, String geometry, int distance,
               int duration, List<Step> steps, String travelMode, Place headsign,
               String routeName, String routeShortName, String routeColor, Date startTime,
               Date endTime, Date startTimeRt, Date endTimeRt, int departureDelay,
               int arrivalDelay, boolean realTime, boolean cancelled, boolean unreachable,
               boolean invalid, boolean alternative, List<Alert> notes,
               String geometryRef, String detailRef) {
        this.alerts = alerts;
        this.from = from;
        this.to = to;
        this.geometry = geometry;
        this.distance = distance;
        this.duration = duration;
        this.steps = steps;
        this.travelMode = travelMode;
        this.headsign = headsign;
        this.routeName = routeName;
        this.routeShortName = routeShortName;
        this.routeColor = routeColor;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startTimeRt = startTimeRt;
        this.endTimeRt = endTimeRt;
        this.departureDelay = departureDelay;
        this.arrivalDelay = arrivalDelay;
        this.realTime = realTime;
        this.cancelled = cancelled;
        this.unreachable = unreachable;
        this.invalid = invalid;
        this.alternative = alternative;
        this.notes = notes;
        this.geometryRef = geometryRef;
        this.detailRef = detailRef;
    }

    protected Leg(Parcel in) {
        from = in.readParcelable(Place.class.getClassLoader());
        to = in.readParcelable(Place.class.getClassLoader());
        geometry = in.readString();
        distance = in.readInt();
        duration = in.readInt();
        steps = in.createTypedArrayList(Step.CREATOR);
        travelMode = in.readString();
        headsign = in.readParcelable(Place.class.getClassLoader());
        routeName = in.readString();
        routeShortName = in.readString();
        routeColor = in.readString();
        startTime = readDate(in);
        endTime = readDate(in);
        startTimeRt = readDate(in);
        endTimeRt = readDate(in);
        departureDelay = in.readInt();
        arrivalDelay = in.readInt();
        realTime = in.readInt() == 1;
        cancelled = in.readInt() == 1;
        unreachable = in.readInt() == 1;
        invalid = in.readInt() == 1;
        alternative = in.readInt() == 1;
        alerts = in.createTypedArrayList(Alert.CREATOR);
        notes = in.createTypedArrayList(Alert.CREATOR);
        geometryRef = in.readString();
        detailRef = in.readString();
        intermediateStops = in.createTypedArrayList(IntermediateStop.CREATOR);
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(from, flags);
        dest.writeParcelable(to, flags);
        dest.writeString(geometry);
        dest.writeInt(distance);
        dest.writeInt(duration);
        dest.writeTypedList(steps);
        dest.writeString(travelMode);
        dest.writeParcelable(headsign, flags);
        dest.writeString(routeName);
        dest.writeString(routeShortName);
        dest.writeString(routeColor);
        dest.writeLong(startTime != null ? startTime.getTime() : -1);
        dest.writeLong(endTime != null ? endTime.getTime() : -1);
        dest.writeLong(startTimeRt != null ? startTimeRt.getTime() : -1);
        dest.writeLong(endTimeRt != null ? endTimeRt.getTime() : -1);
        dest.writeInt(departureDelay);
        dest.writeInt(arrivalDelay);
        dest.writeInt(realTime ? 1 : 0);
        dest.writeInt(cancelled ? 1 : 0);
        dest.writeInt(unreachable ? 1 : 0);
        dest.writeInt(invalid ? 1 : 0);
        dest.writeInt(alternative ? 1 : 0);
        dest.writeTypedList(alerts);
        dest.writeTypedList(notes);
        dest.writeString(geometryRef);
        dest.writeString(detailRef);
        dest.writeTypedList(intermediateStops);
    }

    public Date departsAt() {
        if (startTimeRt != null) {
            return startTimeRt;
        }
        return startTime;
    }

    public Date arrivesAt() {
        if (endTimeRt != null) {
            return endTimeRt;
        }
        return endTime;
    }

    public String getTravelMode() {
        return travelMode;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public boolean hasAlerts() {
        return alerts != null && alerts.size() > 0;
    }

    public boolean hasNotes() {
        return notes != null && notes.size() > 0;
    }

    public boolean isTransit() {
        return !"foot".equals(travelMode);
    }

    public String getRouteColor() {
        return routeColor;
    }

    public boolean isRealTime() {
        return realTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Date getEndTimeRt() {
        return endTimeRt;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getStartTimeRt() {
        return startTimeRt;
    }

    public String getRouteName() {
        return routeName;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public List<Alert> getNotes() {
        return notes;
    }

    public Place getHeadsing() {
        return headsign;
    }

    public boolean hasDepartureDelay() {
        return departureDelay != 0;
    }

    public boolean hasArrivalDelay() {
        return arrivalDelay != 0;
    }

    public boolean hasStopIndex(int stopIndex) {
        return getFrom().getStopIndex() != stopIndex
                && getTo().getStopIndex() != stopIndex;
    }

    @NonNull
    public List<IntermediateStop> getIntermediateStops() {
        if (intermediateStops == null) {
            intermediateStops = new ArrayList<>();
        }
        return intermediateStops;
    }

    public void setIntermediateStops(List<IntermediateStop> intermediateStops) {
        this.intermediateStops = intermediateStops;
    }

    public String getDetailRef() {
        return detailRef;
    }
}
