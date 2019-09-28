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
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Plan implements Parcelable {
    private final List<Route> routes;
    private final String paginateRef;
    private final List<RouteError> errors;
    private String tariffZones;
    private long updatedAtMillis;

    public Plan(List<Route> routes, String paginateRef, List<RouteError> errors) {
        this.routes = routes;
        this.paginateRef = paginateRef;
        this.errors = errors;
    }

    protected Plan(Parcel in) {
        routes = new ArrayList<>();
        in.readTypedList(routes, Route.CREATOR);
        paginateRef = in.readString();
        errors = new ArrayList<>();
        in.readTypedList(errors, RouteError.CREATOR);
        in.readLong();
    }

    public static final Creator<Plan> CREATOR = new Creator<Plan>() {
        @Override
        public Plan createFromParcel(Parcel in) {
            return new Plan(in);
        }

        @Override
        public Plan[] newArray(int size) {
            return new Plan[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(routes);
        dest.writeString(paginateRef);
        dest.writeTypedList(errors);
        dest.writeLong(updatedAtMillis);
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public boolean hasRoutes() {
        return routes != null && routes.size() > 0;
    }

    public boolean hasErrors(@NonNull String mode) {
        if (errors == null || errors.size() == 0) {
            return false;
        }
        for (RouteError routeError : errors) {
            if (mode.equals(routeError.getMode())) {
                return true;
            }
        }
        return false;
    }

    public List<RouteError> getErrors() {
        return errors;
    }

    public RouteError getError(@NonNull String mode) {
        if (errors == null) {
            return null;
        }
        for (RouteError routeError : errors) {
            if (mode.equals(routeError.getMode())) {
                return routeError;
            }
        }
        return null;
    }

    public String getPaginateRef() {
        return paginateRef;
    }

    public boolean canBuySmsTicket() {
        if (!hasRoutes()) {
            return false;
        }
        return false;
        // TODO: Finish this.
//        String tariffZones = null;
//        for (Route route : routes) {
//            if (!route.canBuySmsTicket()) {
//                return false;
//            }
//            if (tariffZones != null && !tariffZones.equals(route.tariffZones)) {
//                tariffZones = null;
//                return false;
//            }
//            tariffZones = trip.tariffZones;
//        }
//        return true;
    }

    public String tariffZones() {
        return tariffZones;
    }

    public boolean shouldRefresh(long timeMillis) {
        if (!hasRoutes()) {
            return false;
        }
        for (Route route : routes) {
            for (Leg leg : route.getLegs()) {
                if (leg.shouldRefresh(timeMillis)) {
                    return true;
                }
            }
        }
        if (updatedAtMillis != 0 && updatedAtMillis < timeMillis - 3600000) {
            return true;
        }
        return false;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }
}
