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
public class Leg {
    private final Place from;
    private final Place to;
    private final String geometry;
    private final int distance;
    private final int duration;

    public Leg(Place from, Place to, String geometry, int distance, int duration) {
        this.from = from;
        this.to = to;
        this.geometry = geometry;
        this.distance = distance;
        this.duration = duration;
    }

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
}
