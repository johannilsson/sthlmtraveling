/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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
import android.util.Log;

import com.markupartist.sthlmtraveling.BuildConfig;
import com.markupartist.sthlmtraveling.provider.ApiConf;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpHelper {
    private static String USER_AGENT = "STHLMTraveling-Android/" + BuildConfig.VERSION_NAME;
    static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static HttpHelper sInstance;
    OkHttpClient mClient;

    private HttpHelper(final Context context) {
        mClient = new OkHttpClient();
        mClient.setConnectTimeout(30, TimeUnit.SECONDS);
        installHttpCache(context);
    }

    public static HttpHelper getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new HttpHelper(context);
        }
        return sInstance;
    }

    /**
     * Install an HTTP cache in the application cache directory.
     */
    private void installHttpCache(final Context context) {
        // Install an HTTP cache in the application cache directory.
        try {
            // Install an HTTP cache in the application cache directory.
            File cacheDir = new File(context.getCacheDir(), "http");
            Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);
            mClient.setCache(cache);
        } catch (Throwable e) {
            Log.e("HttpHelper", "Unable to install disk cache.");
        }
    }

    public Request createRequest(final String endpoint) throws IOException {
        return new Request.Builder()
                .get()
                .url(endpoint)
                .addHeader("X-STHLMTraveling-API-Key", ApiConf.get(ApiConf.KEY))
                .header("User-Agent", USER_AGENT)
                .build();
    }

    public OkHttpClient getClient() {
        return mClient;
    }

}
