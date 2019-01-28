/*
 * Copyright (C) 2009-2019 Johan Nilsson <http://markupartist.com>
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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.crashlytics.android.Crashlytics;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.markupartist.sthlmtraveling.AppConfig;
import com.markupartist.sthlmtraveling.BuildConfig;
import com.markupartist.sthlmtraveling.R;

/**
 * Created by johan on 02/12/14.
 */
public class AdProxy {

  private final static String TAG = "AdProxy";

  private final Context mContext;
  private final boolean hasConsent;
  private final UserConsentForm userConsentForm;
  private AdView mAdView;


  public AdProxy(Context context, String id) {
    mContext = context;

    mAdView = createAdView(id);


    final SharedPreferences sharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context);
    hasConsent = sharedPreferences.getBoolean("has_consent_to_serve_personalized_ads", false);
    userConsentForm = new UserConsentForm(context);
  }

  public void onDestroy() {
    if (mAdView == null) {
      return;
    }

    mAdView.destroy();
  }

  public void onPause() {
    if (mAdView == null) {
      return;
    }

    mAdView.pause();
  }

  public void onResume() {
    if (mAdView == null) {
      return;
    }

    mAdView.resume();
  }

  public void load() {
    userConsentForm.showIfConsentIsUnknown();

    AdRequest.Builder builder = new AdRequest.Builder();
    if (BuildConfig.DEBUG) {
      String[] testDevices = AppConfig.ADMOB_TEST_DEVICES;
      for (String device : testDevices) {
        builder.addTestDevice(device);
      }
    }
    // If we don't have user consent request non-personalized ads.
    if (!hasConsent) {
      Bundle extras = new Bundle();
      extras.putString("npa", "1");
      builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
    }

    AdRequest adRequest = builder.build();

    mAdView.loadAd(adRequest);
  }

  public View getAdView() {
    return mAdView;
  }

  public ViewGroup getAdWithContainer(ViewGroup root, boolean attachToRoot) {
    int containerId = R.layout.ad_container;

    RelativeLayout mAdContainer = (RelativeLayout) LayoutInflater.from(mContext).inflate(
        containerId, root, attachToRoot);

    if (mAdView != null) {
      mAdContainer.addView(mAdView);
    }
    return mAdContainer;
  }

  @Nullable private AdView createAdView(String id) {
    try {
      return createAdMobAdView(id);
    } catch (Throwable t) {
      Log.e(TAG, "Failed to initialize AdView", t);
      Crashlytics.log("Failed to initialize AdView");
      Crashlytics.logException(t);
      return null;
    }
  }

  private AdView createAdMobAdView(String id) {
    AdView adView = new AdView(mContext);
    adView.setAdSize(AdSize.LARGE_BANNER);
    adView.setAdUnitId(id);
    adView.setAdListener(new AdListener() {

      @Override
      public void onAdLoaded() {
        Log.d(TAG, "Ad loaded");
      }

      @Override
      public void onAdFailedToLoad(int errorCode) {
        // Code to be executed when an ad request fails.
        Log.w(TAG, "Ad failed to load");
      }

      @Override
      public void onAdOpened() {
        // Code to be executed when an ad opens an overlay that
        // covers the screen.
      }

      @Override
      public void onAdLeftApplication() {
        // Code to be executed when the user has left the app.
      }

      @Override
      public void onAdClosed() {
        // Code to be executed when when the user is about to return
        // to the app after tapping on an ad.
      }
    });
    return adView;
  }

}
