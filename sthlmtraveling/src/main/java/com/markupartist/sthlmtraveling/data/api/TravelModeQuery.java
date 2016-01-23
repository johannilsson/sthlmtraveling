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

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.markupartist.sthlmtraveling.data.models.TravelMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TravelModeQuery {

    private List<TravelMode> modes;

    public TravelModeQuery(@NonNull List<TravelMode> mode) {
        this.modes = mode;
    }

    public void addMode(TravelMode travelMode) {
        this.modes.add(travelMode);
    }

    public List<TravelMode> getModes() {
        return modes;
    }

    public static TravelModeQuery fromStringList(String travelModes) {
        if (travelModes == null) {
            return new TravelModeQuery(Collections.<TravelMode>emptyList());
        }
        String[] rawModes = travelModes.split(",");
        List<TravelMode> modes = new ArrayList<>();
        for (String mode : rawModes) {
            modes.add(new TravelMode(mode));
        }
        return new TravelModeQuery(modes);
    }

    @Override
    public String toString() {
        return TextUtils.join(",", modes);
    }
}
