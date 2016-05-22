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

/**
 *
 */
public class TravelMode {
    public final static String FOOT = "foot";
    public final static String BIKE = "bike";
    public final static String BIKE_RENTAL = "bikeRent";
    public final static String CAR = "car";
    public final static String METRO = "metro";
    public final static String BUS = "bus";
    public final static String TRAIN = "train";
    public final static String LIGHT_TRAIN = "lightTrain";
    public final static String TRAM = "tram";
    public final static String BOAT = "boat";

    private final String mode;

    public TravelMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return mode;
    }
}
