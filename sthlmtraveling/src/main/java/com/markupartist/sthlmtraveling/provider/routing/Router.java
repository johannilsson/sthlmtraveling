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

import android.util.Log;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.api.PlaceQuery;
import com.markupartist.sthlmtraveling.data.models.Plan;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;

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

    public void plan(JourneyQuery journeyQuery, final Callback callback) {
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

        apiService.getPlan(from, to, "foot,bike,car", false, new retrofit.Callback<Plan>() {
            @Override
            public void success(Plan plan, Response response) {
                callback.onPlan(plan);
            }

            @Override
            public void failure(RetrofitError error) {
                Log.w(TAG, "Could not fetch a route for foot, bike and car.");
            }
        });
    }

    public interface Callback {
        void onPlan(Plan plan);
    }
}
