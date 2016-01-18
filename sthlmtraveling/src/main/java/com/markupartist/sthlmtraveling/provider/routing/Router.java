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

package com.markupartist.sthlmtraveling.provider.routing;

import android.support.annotation.Nullable;
import android.util.Log;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.api.PlaceQuery;
import com.markupartist.sthlmtraveling.data.api.TravelModeQuery;
import com.markupartist.sthlmtraveling.data.models.Plan;
import com.markupartist.sthlmtraveling.data.models.TravelMode;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 *
 */
public class Router {
    private static final String TAG = "Router";
    private final ApiService apiService;

    public Router(ApiService apiService) {
        this.apiService = apiService;
    }

    public void plan(final JourneyQuery journeyQuery, final Callback callback) {
        if (!journeyQuery.origin.hasLocation() || !journeyQuery.destination.hasLocation()) {
            Log.w(TAG, "Origin and or destination is missing location data");
            callback.onPlan(null);
            return;
        }

        PlaceQuery from = new PlaceQuery.Builder()
                .location(journeyQuery.origin.getName(), journeyQuery.origin.getLocation())
                .build();
        PlaceQuery to = new PlaceQuery.Builder()
                .location(journeyQuery.destination.getName(), journeyQuery.destination.getLocation())
                .build();
        PlaceQuery via = null;
        if (journeyQuery.hasVia() && journeyQuery.via.hasLocation()) {
            via = new PlaceQuery.Builder()
                    .location(journeyQuery.via.getName(), journeyQuery.via.getLocation())
                    .build();
        } else {
            Log.i(TAG, "Location data not present on via point");
        }

        apiService.getPlan(from, to, "foot,bike,car", false, via,
                !journeyQuery.isTimeDeparture, null, null, null, null, new retrofit.Callback<Plan>() {
            @Override
            public void success(Plan plan, Response response) {
                callback.onPlan(plan);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.w(TAG, "Could not fetch a route for foot, bike and car.");
                callback.onPlanError(journeyQuery, null);
            }
        });
    }

    public void planTransit(final JourneyQuery journeyQuery, final Callback callback) {
        planTransit(journeyQuery, callback, null);
    }

    public void planTransit(final JourneyQuery journeyQuery,
                            final Callback callback,
                            final @Nullable ScrollDir dir) {
        PlaceQuery from = new PlaceQuery.Builder()
                .place(journeyQuery.origin.asPlace())
                .build();
        PlaceQuery to = new PlaceQuery.Builder()
                .place(journeyQuery.destination.asPlace())
                .build();
        PlaceQuery via = null;
        if (journeyQuery.hasVia() && journeyQuery.via.hasLocation()) {
            via = new PlaceQuery.Builder()
                    .place(journeyQuery.via.asPlace())
                    .build();
        }

        List<TravelMode> travelModes = new ArrayList<>();
        for (String transportMode : journeyQuery.transportModes) {
            switch (transportMode) {
                case TransportMode.BOAT:
                    travelModes.add(new TravelMode(TravelMode.BOAT));
                    break;
                case TransportMode.TRAIN:
                    travelModes.add(new TravelMode(TravelMode.TRAIN));
                    break;
                case TransportMode.BUS:
                    travelModes.add(new TravelMode(TravelMode.BUS));
                    break;
                case TransportMode.TRAM:
                    travelModes.add(new TravelMode(TravelMode.TRAM));
                    travelModes.add(new TravelMode(TravelMode.LIGHT_TRAIN));
                    break;
                case TransportMode.METRO:
                    travelModes.add(new TravelMode(TravelMode.METRO));
                    break;
            }
        }

        String directionParam = null;
        String paginateRef = null;

        if (dir != null) {
            paginateRef = journeyQuery.ident;
            directionParam = dir.getDirection();
        }

        TravelModeQuery travelModeQuery = new TravelModeQuery(travelModes);

        apiService.getPlan(
                from,
                to,
                "transit",
                true,
                via,
                !journeyQuery.isTimeDeparture,
                DateTimeUtil.formatDate(journeyQuery.time),
                travelModeQuery,
                directionParam, paginateRef, new retrofit.Callback<Plan>() {
                    @Override
                    public void success(Plan plan, Response response) {
                        callback.onPlan(plan);
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.w(TAG, "Could not fetch a transit route.");
                        callback.onPlanError(journeyQuery, null);
                    }
                });
    }

    public interface Callback {
        void onPlan(Plan plan);
        void onPlanError(JourneyQuery journeyQuery, String errorCode);
    }
}
