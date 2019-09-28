package com.markupartist.sthlmtraveling;

import android.text.TextUtils;

public class AppConfig {
    public static final String ANALYTICS_PROPERTY_KEY = BuildConfig.APP_ANALYTICS_PROPERTY_KEY;
    public static final String APP_VERSION = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    public static final String WIDESPACE_SID = BuildConfig.DEBUG ? BuildConfig.APP_WIDESPACE_DEBUG_SID : BuildConfig.APP_WIDESPACE_SID;
    public static final String STHLM_TRAVELING_API_KEY = BuildConfig.APP_STHLMTRAVELING_API_KEY;
    public static final String USER_AGENT = "STHLMTraveling-Android/" + BuildConfig.VERSION_NAME;
    public static final String STHLM_TRAVELING_API_ENDPOINT = "http://api.sthlmtraveling.se/";
    public static final String ADMOB_ROUTE_DETAILS_AD_UNIT_ID = BuildConfig.ADMOB_ROUTE_DETAILS_AD_UNIT_ID;
    public static final String[] ADMOB_TEST_DEVICES = TextUtils.split(BuildConfig.ADMOB_TEST_DEVICES, ",");

    public static boolean shouldServeAds() {
        return BuildConfig.APP_IS_ADS_ENABLED;
    }
}
