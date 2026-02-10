package com.markupartist.sthlmtraveling.provider

import android.text.TextUtils

object TransportMode {
    const val UNKNOWN_INDEX: Int = -1
    const val METRO_INDEX: Int = 0
    const val BUS_INDEX: Int = 1
    const val TRAIN_INDEX: Int = 2
    const val TRAM_INDEX: Int = 3
    const val BOAT_INDEX: Int = 4
    const val FOOT_INDEX: Int = 5
    const val NAR_INDEX: Int = 6

    const val UNKNOWN: String = ""
    const val METRO: String = "MET"
    const val METRO_SYNONYM: String = "METRO"
    const val BUS: String = "BUS"
    const val TRAIN: String = "TRN"
    const val TRAM: String = "TRM"
    const val BOAT: String = "SHP"
    const val WAX: String = "WAX" // Used in the request.
    const val FOOT: String = "Walk"
    const val NAR: String = "NAR"

    const val FLY: String = "FLY"
    const val AEX: String = "AEX"
    const val BIKE_RENTAL: String = "bikeRental"

    @JvmStatic
    fun getIndex(transportMode: String?): Int {
        if (TextUtils.isEmpty(transportMode)) {
            return UNKNOWN_INDEX
        }
        when (transportMode) {
            METRO, METRO_SYNONYM -> return METRO_INDEX
            BUS -> return BUS_INDEX
            TRAIN -> return TRAIN_INDEX
            TRAM -> return TRAM_INDEX
            FOOT -> return FOOT_INDEX
            BOAT -> return BOAT_INDEX
            NAR -> return NAR_INDEX
        }
        return UNKNOWN_INDEX
    }
}
