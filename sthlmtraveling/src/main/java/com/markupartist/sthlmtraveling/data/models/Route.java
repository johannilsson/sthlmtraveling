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

import java.util.List;

/**
 *
 */
public class Route {
    private final int duration;
    private final List<Leg> legs;
    private final String mode;
    private final List<Step> steps;

    public Route(int duration, List<Leg> legs, String mode, List<Step> steps) {
        this.duration = duration;
        this.legs = legs;
        this.mode = mode;
        this.steps = steps;
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

    public List<Step> getSteps() {
        return steps;
    }
}
