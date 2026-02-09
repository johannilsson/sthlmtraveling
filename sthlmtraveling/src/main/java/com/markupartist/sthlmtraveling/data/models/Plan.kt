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
package com.markupartist.sthlmtraveling.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *
 */
@Parcelize
data class Plan(
    val routes: MutableList<Route>?,
    val paginateRef: String?,
    val errors: MutableList<RouteError>?,
    private val tariffZones: String? = null,
    var updatedAtMillis: Long = 0
) : Parcelable {

    fun hasRoutes(): Boolean {
        return routes != null && routes.size > 0
    }

    fun hasErrors(mode: String): Boolean {
        if (errors == null || errors.size == 0) {
            return false
        }
        for (routeError in errors) {
            if (mode == routeError.getMode()) {
                return true
            }
        }
        return false
    }

    fun getError(mode: String): RouteError? {
        if (errors == null) {
            return null
        }
        for (routeError in errors) {
            if (mode == routeError.getMode()) {
                return routeError
            }
        }
        return null
    }

    fun canBuySmsTicket(): Boolean {
        if (!hasRoutes()) {
            return false
        }
        return false
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

    fun tariffZones(): String? {
        return tariffZones
    }

    fun shouldRefresh(timeMillis: Long): Boolean {
        if (!hasRoutes()) {
            return false
        }
        for (route in routes!!) {
            for (leg in route.legs) {
                if (leg.shouldRefresh(timeMillis)) {
                    return true
                }
            }
        }
        if (updatedAtMillis != 0L && updatedAtMillis < timeMillis - 3600000) {
            return true
        }
        return false
    }
}
