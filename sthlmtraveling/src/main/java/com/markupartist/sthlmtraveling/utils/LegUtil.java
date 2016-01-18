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

package com.markupartist.sthlmtraveling.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.data.models.Leg;

import java.util.Locale;

/**
 *
 */
public class LegUtil {
    public static Drawable getTransportDrawable(Context context, Leg leg) {
        int color = ContextCompat.getColor(context, R.color.icon_default);
        Drawable drawable;

        switch (leg.getTravelMode()) {
            case "bus":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case "metro":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro);
                return ViewHelper.tintIcon(drawable, color);
            case "foot":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp);
                return ViewHelper.tintIcon(drawable, ContextCompat.getColor(context, R.color.icon_default));
            case "train":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case "tram":
            case "lightTrain":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case "boat":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case "car":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_car_20dp);
                return ViewHelper.tintIcon(drawable, color);
            case "bike":
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_bike_20dp);
                return ViewHelper.tintIcon(drawable, color);
            default:
                // What to use when we don't know..
                drawable = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp);
        }
        return ViewHelper.tintIcon(drawable, color);
    }

    @ColorInt
    public static int getColor(Context context, Leg leg) {
        if (leg.getRouteColor() == null) {
            // Add color for foot.
            return ContextCompat.getColor(context, R.color.train);
        }
        return Color.parseColor(leg.getRouteColor());
    }

    public static String getRouteName(Leg leg, boolean truncate) {
        String routeName = leg.getRouteName();
        if (!TextUtils.isEmpty(routeName)) {
            routeName = ViewHelper.uppercaseFirst(routeName, Locale.US);
        } else {
            routeName = leg.getRouteShortName();
        }
        if (TextUtils.isEmpty(routeName)) {
            return "";
        }

        if (truncate && routeName.length() > 30) {
            routeName = String.format(Locale.US, "%s…", routeName.trim().substring(0, 29));
        }
        return routeName;
    }
}
