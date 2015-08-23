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

package com.markupartist.sthlmtraveling.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.katalysator.sdk.engine.KATAssets;
import com.katalysator.sdk.engine.KATEvent;
import com.katalysator.sdk.engine.KATManager;
import com.markupartist.sthlmtraveling.AppConfig;

import java.util.ArrayList;
import java.util.HashMap;

public class BeaconManager implements KATEvent {

    private final static String TAG = "BeaconManager";
    private final static String API_TOKEN = AppConfig.KATALYST_API_KEY;

    private static BeaconManager instance;
    private Context mContext;
    private KATManager mKatManager;

    private BeaconManager(Context context) {
        mContext = context;
        mKatManager = KATManager.getInstance(mContext, API_TOKEN, this);
    }

    public static BeaconManager getInstance(Context context) {
        if (instance == null) {
            instance = new BeaconManager(context.getApplicationContext());
        }
        return instance;
    }

    protected boolean isAdsActive() {
        if (!AppConfig.shouldServeAds()) {
            return false;
        }
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext.getApplicationContext());
        boolean isDisabled = sharedPreferences.getBoolean("is_ads_disabled", false);
        return !isDisabled;
    }

    public void start() {
        if (!isAdsActive()) {
            return;
        }

        mKatManager.startMonitoring();
        mKatManager.getAudiencesAndGeotags();
        HashMap keyValue = new HashMap();
        mKatManager.collect(keyValue);
    }

    public void stop() {
        if (!isAdsActive()) {
            return;
        }

        mKatManager.stopMonitoring();
    }

    @Override
    public void regionDataReceived(KATAssets katAssets, boolean isDelayed) {
        Log.d(TAG, "regionDataReceived: " + katAssets);
    }

    @Override
    public void notificationShouldDisplay(KATAssets katAssets, Class aClass) {
        Log.d(TAG, "notificationShouldDisplay: " + katAssets + ", " + aClass);
    }

    @Override
    public void fullScreenImageShouldDisplay(Class aClass, KATAssets katAssets) {
        Log.d(TAG, "fullScreenImageShouldDisplay: " + katAssets + ", " + aClass);
    }

    @Override
    public void availableAudiencesUpdated(ArrayList<String> strings) {
        Log.d(TAG, "availableAudiencesUpdated: " + strings);
    }


}
