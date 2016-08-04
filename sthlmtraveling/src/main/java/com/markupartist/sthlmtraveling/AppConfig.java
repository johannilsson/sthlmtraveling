package com.markupartist.sthlmtraveling;

import android.support.v4.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.markupartist.sthlmtraveling.utils.AdProxy;

public class AppConfig {
    public static final String ANALYTICS_PROPERTY_KEY = BuildConfig.APP_ANALYTICS_PROPERTY_KEY;
    public static final String APP_VERSION = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    public static final String WIDESPACE_SID = BuildConfig.DEBUG ? BuildConfig.APP_WIDESPACE_DEBUG_SID : BuildConfig.APP_WIDESPACE_SID;
    public static final String STHLM_TRAVELING_API_KEY = BuildConfig.APP_STHLMTRAVELING_API_KEY;
    public static final String USER_AGENT = "STHLMTraveling-Android/" + BuildConfig.VERSION_NAME;
    public static final String STHLM_TRAVELING_API_ENDPOINT = "http://api.sthlmtraveling.se/";
//    public static final String STHLM_TRAVELING_API_ENDPOINT = "http://192.168.1.2:7000/";
//    public static final String STHLM_TRAVELING_API_ENDPOINT = "http://192.168.0.10:7000/";

    public static boolean shouldServeAds() {
        return BuildConfig.APP_IS_ADS_ENABLED;
    }

    public static AdProxy.Provider AD_DEFAULT_PROVIDER = AdProxy.Provider.WIDESPACE;
    public static String AD_DEFAULT_ID = WIDESPACE_SID;

    public static final int[] TICKET_FULL_PRICE = new int[]{36, 54, 72};
    public static final int[] TICKET_REDUCED_PRICE = new int[]{20, 30, 40};
    public static final String TICKET_SMS_NUMBER = "0767201010";

    public static final LatLngBounds GREATER_STHLM_BOUNDS = new LatLngBounds(
            new LatLng(59.039795, 17.297760), new LatLng(60.053231, 19.086177));

    public static Pair<AdProxy.Provider, String> getAdConfForRouteDetails(int preferredNetwork) {
        AdProxy.Provider network = AD_DEFAULT_PROVIDER;
        String id = AD_DEFAULT_ID;

        switch (preferredNetwork) {
            case 1:
                network = AdProxy.Provider.WIDESPACE;
                id = WIDESPACE_SID;
                break;
        }

        return Pair.create(network, id);
    }
}
