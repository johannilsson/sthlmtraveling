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

package com.markupartist.sthlmtraveling.data.api;

import android.location.Location;
import android.text.TextUtils;

import com.markupartist.sthlmtraveling.data.models.Place;

import java.util.Locale;

/**
 *
 */
public class PlaceQuery {
    private String id;
    private String name;
    private double lon;
    private double lat;

    private PlaceQuery(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.lat = builder.lat;
        this.lon = builder.lon;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        if (id != null) {
            if (this.lat != 0D && this.lon != 0D) {
                return String.format(Locale.US, "%s,%s:%s:%s", lat, lon, id, name);
            }
            return String.format(Locale.US, "%s:%s", id, name);
        }
        return String.format(Locale.US, "%s,%s:%s", lat, lon, name);
    }

    public static final class Builder {
        private String id;
        private String name;
        private double lon;
        private double lat;

        public Builder place(Place place) {
            this.id = place.getId();
            this.name = place.getName();
            this.lat = place.getLat();
            this.lon = place.getLon();
            return this;
        }

        public Builder location(String name, Location location) {
            this.name = name;
            this.lat = location.getLatitude();
            this.lon = location.getLongitude();
            return this;
        }

        public Builder location(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            return this;
        }

        public Builder param(String param) {
            name = param.substring(param.lastIndexOf(":") + 1);
            String idAndLocation = param.substring(0, param.lastIndexOf(":"));
            if (idAndLocation.contains(",")) {
                String locationStr;
                if (idAndLocation.contains(":")) {
                    locationStr = idAndLocation.substring(0, idAndLocation.indexOf(":"));
                    id = idAndLocation.substring(idAndLocation.indexOf(":") + 1);
                } else {
                    locationStr = idAndLocation;
                }
                String[] locationParts = TextUtils.split(locationStr, ",");
                lat = Double.parseDouble(locationParts[0]);
                lon = Double.parseDouble(locationParts[1]);
            } else {
                id = idAndLocation;
            }
            return this;
        }

        public PlaceQuery build() {
            return new PlaceQuery(this);
        }
    }
}
