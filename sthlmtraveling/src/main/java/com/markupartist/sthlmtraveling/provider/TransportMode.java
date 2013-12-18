package com.markupartist.sthlmtraveling.provider;

import java.util.ArrayList;

import android.text.TextUtils;

public class TransportMode {
    public final static int UNKNOWN_INDEX = -1;
    public final static int METRO_INDEX = 0;
    public final static int BUS_INDEX = 1;
    public final static int TRAIN_INDEX = 2;
    public final static int LOKALBANA_INDEX = 3;

    public static final String UNKNOWN = "";
    public static final String BUS = "BUS";
    public static final String METRO = "MET";
    public static final String TRAIN = "TRAIN";
    public static final String TRAM = "TRAM";
    public static final String FLY = "FLY";
    public static final String NAR = "NAR";
    public static final String WAX = "WAX";
    public static final String AEX = "AEX";

    public static final ArrayList<String> getDefaultTransportModes() {
        ArrayList<String> transportModes = new ArrayList<String>();
        transportModes.add(BUS);
        transportModes.add(METRO);
        transportModes.add(TRAIN);
        transportModes.add(TRAM);
        transportModes.add(FLY);
        transportModes.add(NAR);
        transportModes.add(WAX);
        return transportModes;
    }

    public static final int getIndex(String transportMode) {
        if (TextUtils.isEmpty(transportMode)) {
            return UNKNOWN_INDEX;
        }
        if (transportMode.equals(METRO) || transportMode.equals("METRO")) {
            return METRO_INDEX;
        } else if (transportMode.equals(BUS)) {
            return BUS_INDEX;
        } else if (transportMode.equals(TRAIN)) {
            return TRAIN_INDEX;
        } else if (transportMode.equals(TRAM)) {
            return LOKALBANA_INDEX;
        }
        return UNKNOWN_INDEX;
    }
}
