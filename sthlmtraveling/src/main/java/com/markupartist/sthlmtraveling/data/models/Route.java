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
import java.util.Date;
import java.util.List;

/**
 *
 */
public class Route implements Parcelable {
    private final int duration;
    private final List<Leg> legs;
    private final String mode;
    private final Fare fare;

    public Route(int duration, List<Leg> legs, String mode, Fare fare) {
        this.duration = duration;
        this.legs = legs;
        this.mode = mode;
        this.fare = fare;
    }

    protected Route(Parcel in) {
        duration = in.readInt();
        legs = new ArrayList<>();
        in.readTypedList(legs, Leg.CREATOR);
        mode = in.readString();
        fare = in.readParcelable(Fare.class.getClassLoader());
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(duration);
        dest.writeTypedList(legs);
        dest.writeString(mode);
        dest.writeParcelable(fare, flags);
    }

    public int getDuration() {
        return duration;
    }

    public List<Leg> getLegs() {
        return legs;
    }

    public String getMode() {
        return mode;
    }

    public Date departsAt(boolean useTransitTime) {
        if (useTransitTime) {
            for (Leg leg : legs) {
                if (leg.isTransit()) {
                    return leg.departsAt();
                }
            }
        }
        // If no transit leg, get the first.
        Leg leg = legs.get(0);
        return leg.departsAt();
    }

    public Date arrivesAt(boolean useTransitTime) {
        if (useTransitTime) {
            for (int i = legs.size() - 1; i >= 0; i--) {
                Leg leg = legs.get(i);
                if (leg.isTransit()) {
                    return leg.arrivesAt();
                }
            }
        }
        Leg leg = legs.get(legs.size() - 1);
        return leg.arrivesAt();
    }

    public Place fromStop() {
        for (Leg leg : legs) {
            if (leg.isTransit()) {
                return leg.getFrom();
            }
        }
        // If no transit legs, get the first.
        return legs.get(0).getFrom();
    }

    public Place toStop() {
        for (int i = legs.size() - 1; i >= 0; i--) {
            Leg leg = legs.get(i);
            if (leg.isTransit()) {
                return leg.getTo();
            }
        }
        // If no transit leg, get the last.
        return legs.get(legs.size() - 1).getTo();
    }

    public boolean hasAlertsOrNotes() {
        for (Leg leg : legs) {
            if (leg.hasAlerts() || leg.hasNotes()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return returns true if this trip can be purchased with SMS.
     */
    public boolean canBuyTicket() {
        if (fare == null) {
            return false;
        }
        return fare.canBuyTicket();
    }

    public Fare getFare() {
        return fare;
    }
}
