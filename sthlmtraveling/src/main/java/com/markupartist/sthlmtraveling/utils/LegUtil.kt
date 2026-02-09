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

package com.markupartist.sthlmtraveling.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.Leg
import com.markupartist.sthlmtraveling.data.models.TravelMode
import com.markupartist.sthlmtraveling.provider.TransportMode
import java.util.Locale

/**
 *
 */
object LegUtil {
    @JvmStatic
    fun getTransportDrawable(context: Context, leg: Leg): Drawable? {
        val color = ContextCompat.getColor(context, R.color.icon_default)
        val drawable: Drawable? = when (leg.travelMode) {
            "bus" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_bus_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            "metro" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_sl_metro)
                return ViewHelper.tintIcon(d, color)
            }
            "foot" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_walk_20dp)
                return ViewHelper.tintIcon(d, ContextCompat.getColor(context, R.color.icon_default))
            }
            "train" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            "tram", "lightTrain" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_light_train_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            "boat" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_boat_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            "car" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_car_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            "bike" -> {
                val d = ContextCompat.getDrawable(context, R.drawable.ic_transport_bike_20dp)
                return ViewHelper.tintIcon(d, color)
            }
            else -> {
                // What to use when we don't know..
                ContextCompat.getDrawable(context, R.drawable.ic_transport_train_20dp)
            }
        }
        return ViewHelper.tintIcon(drawable, color)
    }

    @ColorInt
    @JvmStatic
    fun getColor(context: Context, leg: Leg): Int {
        if (TextUtils.isEmpty(leg.routeColor)) {
            // Add color for foot or when color is missing/empty.
            return ContextCompat.getColor(context, R.color.train)
        }
        try {
            // Ensure color starts with # for hex format
            val color = if (leg.routeColor!!.startsWith("#")) leg.routeColor else "#${leg.routeColor}"
            return Color.parseColor(color)
        } catch (e: IllegalArgumentException) {
            // Invalid color format, use fallback
            Log.w("LegUtil", "Invalid routeColor format: ${leg.routeColor}", e)
            return ContextCompat.getColor(context, R.color.train)
        }
    }

    @JvmStatic
    fun getRouteName(leg: Leg, truncate: Boolean): String {
        var routeName = leg.routeName
        if (!TextUtils.isEmpty(routeName)) {
            routeName = ViewHelper.uppercaseFirst(routeName, Locale.US)
        } else {
            routeName = leg.routeShortName
        }
        if (TextUtils.isEmpty(routeName)) {
            return ""
        }

        if (truncate && routeName!!.length > 30) {
            routeName = String.format(Locale.US, "%s…", routeName.trim().substring(0, 29))
        }
        return routeName ?: ""
    }

    @JvmStatic
    fun transportModesToTravelModes(transportModes: List<String>): List<TravelMode> {
        val travelModes = mutableListOf<TravelMode>()
        for (transportMode in transportModes) {
            when (transportMode) {
                TransportMode.BOAT, TransportMode.WAX -> {
                    travelModes.add(TravelMode(TravelMode.BOAT))
                }
                TransportMode.TRAIN -> {
                    travelModes.add(TravelMode(TravelMode.TRAIN))
                }
                TransportMode.BUS -> {
                    travelModes.add(TravelMode(TravelMode.BUS))
                }
                TransportMode.TRAM -> {
                    travelModes.add(TravelMode(TravelMode.TRAM))
                    travelModes.add(TravelMode(TravelMode.LIGHT_TRAIN))
                }
                TransportMode.METRO -> {
                    travelModes.add(TravelMode(TravelMode.METRO))
                }
                TransportMode.BIKE_RENTAL -> {
                    travelModes.add(TravelMode(TravelMode.BIKE_RENTAL))
                }
            }
        }
        return travelModes
    }

    @JvmStatic
    fun travelModesToTransportModes(travelModes: List<TravelMode>): List<String> {
        val transportModes = mutableListOf<String>()
        for (travelMode in travelModes) {
            when (travelMode.mode) {
                TravelMode.BOAT -> {
                    transportModes.add(TransportMode.BOAT)
                    transportModes.add(TransportMode.WAX)
                }
                TravelMode.TRAIN -> {
                    transportModes.add(TransportMode.TRAIN)
                }
                TravelMode.BUS -> {
                    transportModes.add(TransportMode.BUS)
                }
                TravelMode.TRAM, TravelMode.LIGHT_TRAIN -> {
                    transportModes.add(TransportMode.TRAM)
                }
                TravelMode.METRO -> {
                    transportModes.add(TransportMode.METRO)
                }
            }
        }
        return transportModes
    }
}
