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

import com.markupartist.sthlmtraveling.data.models.IntermediateResponse;
import com.markupartist.sthlmtraveling.data.models.Plan;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 *
 */
public interface ApiService {
    @GET("/v1/planner/")
    void getPlan(
            @Query("from") PlaceQuery from,
            @Query("to") PlaceQuery to,
            @Query("mode") String mode,
            @Query("alternative") boolean alternative,
            @Query("via") PlaceQuery via,
            @Query("arriveBy") boolean arriveBy,
            @Query("time") String time,
            @Query("travelMode") TravelModeQuery travelMode,
            @Query("dir") String dir,
            @Query("paginateRef") String paginateRef,
            Callback<Plan> callback);

    @GET("/v1/planner/intermediate/")
    void getIntermediateStops(
            @Query("reference") List<String> reference,
            Callback<IntermediateResponse> callback);

}
