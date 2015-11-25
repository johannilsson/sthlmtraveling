package com.markupartist.sthlmtraveling.provider;

import android.text.TextUtils;

public class TransportMode {
    public final static int UNKNOWN_INDEX = -1;
    public final static int METRO_INDEX = 0;
    public final static int BUS_INDEX = 1;
    public final static int TRAIN_INDEX = 2;
    public final static int TRAM_INDEX = 3;
    public final static int BOAT_INDEX = 4;
    public final static int FOOT_INDEX = 5;
    public final static int NAR_INDEX = 6;

    public static final String UNKNOWN = "";
    public static final String METRO = "MET";
    public static final String METRO_SYNONYM = "METRO";
    public static final String BUS = "BUS";
    public static final String TRAIN = "TRN";
    public static final String TRAM = "TRM";
    public static final String BOAT = "SHP";
    public static final String WAX = "WAX"; // Used in the request.
    public static final String FOOT = "Walk";
    public static final String NAR = "NAR";

    public static final String FLY = "FLY";
    public static final String AEX = "AEX";

    public static int getIndex(String transportMode) {
        if (TextUtils.isEmpty(transportMode)) {
            return UNKNOWN_INDEX;
        }
        switch (transportMode) {
            case METRO:
            case METRO_SYNONYM:
                return METRO_INDEX;
            case BUS:
                return BUS_INDEX;
            case TRAIN:
                return TRAIN_INDEX;
            case TRAM:
                return TRAM_INDEX;
            case FOOT:
                return FOOT_INDEX;
            case BOAT:
                return BOAT_INDEX;
            case NAR:
                return NAR_INDEX;
        }
        return UNKNOWN_INDEX;
    }
}
