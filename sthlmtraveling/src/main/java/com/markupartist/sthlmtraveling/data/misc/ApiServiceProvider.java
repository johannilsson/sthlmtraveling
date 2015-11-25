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

package com.markupartist.sthlmtraveling.data.misc;

import com.markupartist.sthlmtraveling.AppConfig;
import com.markupartist.sthlmtraveling.BuildConfig;
import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 *
 */
public class ApiServiceProvider {
    public static RestAdapter getRestAdapter(OkHttpClient client) {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("User-Agent", AppConfig.USER_AGENT);
                request.addHeader("X-STHLMTraveling-API-Key", AppConfig.STHLM_TRAVELING_API_KEY);
            }
        };

        return new RestAdapter.Builder()
                .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE)
                .setEndpoint(AppConfig.STHLM_TRAVELING_API_ENDPOINT)
                .setConverter(new GsonConverter(GsonProvider.provideGson()))
                .setRequestInterceptor(requestInterceptor)
                .setClient(new OkClient(client))
                .build();
    }

    public static ApiService getApiService(OkHttpClient okHttpClient) {
        return ApiServiceProvider.getRestAdapter(okHttpClient)
                .create(ApiService.class);
    }
}