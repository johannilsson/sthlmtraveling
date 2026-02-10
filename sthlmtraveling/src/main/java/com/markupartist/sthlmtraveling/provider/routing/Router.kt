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
package com.markupartist.sthlmtraveling.provider.routing

import android.util.Log
import com.markupartist.sthlmtraveling.data.api.ApiService
import com.markupartist.sthlmtraveling.data.api.PlaceQuery
import com.markupartist.sthlmtraveling.data.api.TravelModeQuery
import com.markupartist.sthlmtraveling.data.models.Plan
import com.markupartist.sthlmtraveling.data.models.TravelMode
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery
import com.markupartist.sthlmtraveling.utils.DateTimeUtil
import com.markupartist.sthlmtraveling.utils.LegUtil
import retrofit.RetrofitError
import retrofit.client.Response

/**
 *
 */
class Router(private val apiService: ApiService) {
    fun plan(journeyQuery: JourneyQuery, callback: Callback) {
        if (!journeyQuery.origin!!.hasLocation() || !journeyQuery.destination!!.hasLocation()) {
            Log.w(TAG, "Origin and or destination is missing location data")
            callback.onPlan(null)
            return
        }

        val from = PlaceQuery.Builder()
            .location(journeyQuery.origin!!.name, journeyQuery.origin!!.location)
            .build()
        val to = PlaceQuery.Builder()
            .location(journeyQuery.destination!!.name, journeyQuery.destination!!.location)
            .build()
        var via: PlaceQuery? = null
        if (journeyQuery.hasVia() && journeyQuery.via!!.hasLocation()) {
            via = PlaceQuery.Builder()
                .location(journeyQuery.via!!.name, journeyQuery.via!!.location)
                .build()
        } else {
            Log.i(TAG, "Location data not present on via point")
        }

        apiService.getPlan(
            from,
            to,
            "foot,bike,car",
            false,
            via,
            !journeyQuery.isTimeDeparture,
            null,
            null,
            null,
            null,
            object : retrofit.Callback<Plan?> {
                override fun success(plan: Plan?, response: Response?) {
                    callback.onPlan(plan)
                }

                override fun failure(error: RetrofitError?) {
                    Log.w(TAG, "Could not fetch a route for foot, bike and car.")
                    callback.onPlanError(journeyQuery, null)
                }
            })
    }

    fun refreshTransit(journeyQuery: JourneyQuery, callback: Callback) {
        planTransit(journeyQuery, callback, journeyQuery.previousIdent, journeyQuery.previousDir)
    }

    @JvmOverloads
    fun planTransit(
        journeyQuery: JourneyQuery,
        callback: Callback,
        dir: ScrollDir? = null
    ) {
        val direction = if (dir != null) dir.direction else null
        journeyQuery.previousDir = direction
        journeyQuery.previousIdent = journeyQuery.ident
        planTransit(journeyQuery, callback, journeyQuery.ident, direction)
    }

    fun planTransit(
        journeyQuery: JourneyQuery,
        callback: Callback,
        ident: String?,
        dir: String?
    ) {
        val from = PlaceQuery.Builder()
            .place(journeyQuery.origin!!.asPlace())
            .build()
        val to = PlaceQuery.Builder()
            .place(journeyQuery.destination!!.asPlace())
            .build()
        var via: PlaceQuery? = null
        if (journeyQuery.hasVia()) {
            via = PlaceQuery.Builder()
                .place(journeyQuery.via!!.asPlace())
                .build()
        }

        val travelModes: List<TravelMode> = LegUtil.transportModesToTravelModes(
            journeyQuery.transportModes
        )

        var directionParam: String? = null
        var paginateRef: String? = null

        if (dir != null) {
            paginateRef = ident
            directionParam = dir
        }

        val travelModeQuery = TravelModeQuery(travelModes)

        apiService.getPlan(
            from,
            to,
            "transit",
            journeyQuery.alternativeStops,
            via,
            !journeyQuery.isTimeDeparture,
            DateTimeUtil.formatDate(journeyQuery.time!!),
            travelModeQuery,
            directionParam, paginateRef, object : retrofit.Callback<Plan?> {
                override fun success(plan: Plan?, response: Response?) {
                    if (plan != null) {
                        plan.updatedAtMillis = System.currentTimeMillis()
                        callback.onPlan(plan)
                    } else {
                        callback.onPlan(null)
                    }
                }

                override fun failure(error: RetrofitError?) {
                    Log.w(TAG, "Could not fetch a transit route.")
                    callback.onPlanError(journeyQuery, null)
                }
            })
    }

    interface Callback {
        fun onPlan(plan: Plan?)
        fun onPlanError(journeyQuery: JourneyQuery?, errorCode: String?)
    }

    companion object {
        private const val TAG = "Router"
    }
}
