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

import com.markupartist.sthlmtraveling.provider.ApiConf;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {
    static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static HttpHelper sInstance;
    OkHttpClient mClient;

    private HttpHelper(final Context context) {
        mClient = new OkHttpClient();
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
            File cacheDir = new File(context.getCacheDir(), "http");
            HttpResponseCache cache = new HttpResponseCache(cacheDir, DISK_CACHE_SIZE);
            mClient.setResponseCache(cache);
        } catch (IOException e) {
            Log.e("HttpHelper", "Unable to install disk cache.");
        }
    }

    public OkHttpClient getHttpClient() {
        return mClient;
    }

    public HttpURLConnection getConnection(final String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = mClient.open(url);
        connection.setRequestProperty("X-STHLMTraveling-API-Key", ApiConf.get(ApiConf.KEY));
        //connection.setUseCaches(true);
        return connection;
    }

    public String getBody(final HttpURLConnection connection) throws IOException {
        InputStream in = null;
        try {
            in = connection.getInputStream();
            return StreamUtils.toString(in);
        } finally {
            if (in != null) in.close();
        }
    }

    public String getErrorBody(final HttpURLConnection connection) throws IOException {
        InputStream in = null;
        try {
            in = connection.getErrorStream();
            return StreamUtils.toString(in);
        } finally {
            if (in != null) in.close();
        }
    }

    public String get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection connection = mClient.open(url);
        connection.setRequestProperty("X-STHLMTraveling-API-Key", ApiConf.get(ApiConf.KEY));

        InputStream in = null;
        try {
            in = connection.getInputStream();
            return StreamUtils.toString(in);
        } finally {
            if (in != null) in.close();
        }
    }
}
