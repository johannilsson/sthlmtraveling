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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.crashlytics.android.Crashlytics;
import com.markupartist.sthlmtraveling.R;
import com.widespace.AdInfo;
import com.widespace.AdSpace;
import com.widespace.adspace.models.PrefetchStatus;
import com.widespace.exception.ExceptionTypes;
import com.widespace.interfaces.AdErrorEventListener;
import com.widespace.interfaces.AdEventListener;

/**
 * Created by johan on 02/12/14.
 */
public class AdProxy {

    private final static String TAG = "AdProxy";
    private static final String AD_DISMISSED_AT_KEY = "ad_dismissed_at";
    private static final long AD_DISMISSED_GRACE_PERIOD = 60 * 1 * 1000;

    private final Provider mProvider;
    private final Context mContext;
    private View mAdView;
    private boolean mIsDestroyed;

    public enum Provider {
        WIDESPACE,
    }

    public AdProxy(Context context, Provider provider, String id) {
        mContext = context;
        mProvider = provider;

        mAdView = createAdView(id);
    }

    public Provider getProvider() {
        return mProvider;
    }

    public void onDestroy() {
        mIsDestroyed = true;

        if (mProvider == Provider.WIDESPACE) {
            mAdView = null;
        }
    }

    public void onPause() {
        if (mAdView == null) {
            return;
        }

        if (mProvider == Provider.WIDESPACE) {
            ((AdSpace) mAdView).pause();
        }
    }

    public void onResume() {
        if (mAdView == null) {
            return;
        }

        if (mProvider == Provider.WIDESPACE) {

            if (shouldShowAfterDismiss()) {
                ((AdSpace) mAdView).setAutoStart(true);
                ((AdSpace) mAdView).setAutoUpdate(true);
            }

            ((AdSpace) mAdView).resume();
        }
    }

    public void load() {
        if (mProvider == Provider.WIDESPACE) {
            //((AdSpace) mAdView).runAd();
        }
    }

    public View getAdView() {
        return mAdView;
    }

    public ViewGroup getAdWithContainer(ViewGroup root, boolean attachToRoot) {
        int containerId = R.layout.ad_container_no_margins;

        RelativeLayout mAdContainer = (RelativeLayout) LayoutInflater.from(mContext).inflate(
                containerId, root, attachToRoot);

        if (mAdView != null) {
            mAdContainer.addView(mAdView);
        }
        return mAdContainer;
    }

    @Nullable
    protected View createAdView(String id) {
        switch (mProvider) {
            case WIDESPACE:
                try {
                    return createAdSpace(id);
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to initialize Widespace", t);
                    Crashlytics.log("Failed to initialize Widespace");
                    Crashlytics.logException(t);
                    return null;
                }
        }
        throw new IllegalArgumentException("Unknown ad provider");
    }

    public boolean shouldShowAfterDismiss() {
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext.getApplicationContext());
        long dismissedAt = sharedPreferences.getLong(AD_DISMISSED_AT_KEY, 0);
        long timePassed = System.currentTimeMillis() - dismissedAt;
        return timePassed >= AD_DISMISSED_GRACE_PERIOD;
    }

    protected AdSpace createAdSpace(String id) {
        if (!shouldShowAfterDismiss()) {
            Analytics.getInstance(mContext).event("Ads", "Ad view grace period");
            return null;
        }

        AdSpace adSpace = new AdSpace(mContext, id, true, true);

//        if (adSpace.getQueueSize() < 1) {
//            adSpace.prefetchAd();
//        }

        adSpace.setAdEventListener(new AdEventListener() {
            @Override
            public void onAdClosing(AdSpace adSpace, AdInfo.AdType adType) {
                Log.d(TAG, "onAdClosing");
            }

            @Override
            public void onAdClosed(AdSpace adSpace, AdInfo.AdType adType) {
                Log.d(TAG, "onAdClosed");
            }

            @Override
            public void onAdLoading(AdSpace adSpace) {
                Log.d(TAG, "onAdLoading");
            }

            @Override
            public void onAdLoaded(AdSpace adSpace, AdInfo.AdType adType) {
            }

            @Override
            public void onNoAdRecieved(AdSpace adSpace) {
                Log.d(TAG, "onNoAdRecieved");
            }

            @Override
            public void onPrefetchAd(AdSpace adSpace, PrefetchStatus prefetchStatus) {
                Log.d(TAG, "onPrefetchAd");
            }

            @Override
            public void onAdPresenting(AdSpace adSpace, boolean b, AdInfo.AdType adType) {
                Log.d(TAG, "onAdPresenting");
            }

            @Override
            public void onAdPresented(final AdSpace adSpace, boolean b, AdInfo.AdType adType) {
                Log.d(TAG, "onAdPresented");
            }

            @Override
            public void onAdDismissing(AdSpace adSpace, boolean b, AdInfo.AdType adType) {

            }

            @Override
            public void onAdDismissed(AdSpace adSpace, boolean b, AdInfo.AdType adType) {
                Log.e(TAG, "onAdDismissed");
                final SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(mContext.getApplicationContext());
                sharedPreferences.edit()
                        .putLong(AD_DISMISSED_AT_KEY, System.currentTimeMillis()).apply();
                adSpace.setAutoStart(false);
                adSpace.setAutoUpdate(false);
            }
        });
        adSpace.setAdErrorEventListener(new AdErrorEventListener() {
            @Override
            public void onFailedWithError(Object o, ExceptionTypes exceptionTypes, String s, Exception e) {
                Crashlytics.log("Widespace: " + s);
                Crashlytics.logException(e);
            }
        });

        return adSpace;
    }

}
