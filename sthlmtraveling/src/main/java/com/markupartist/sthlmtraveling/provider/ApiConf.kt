package com.markupartist.sthlmtraveling.provider

import com.markupartist.sthlmtraveling.AppConfig

object ApiConf {
    @JvmField
    var KEY: String = AppConfig.STHLM_TRAVELING_API_KEY

    fun apiEndpoint2(): String {
        return AppConfig.STHLM_TRAVELING_API_ENDPOINT
    }

    @JvmStatic
    fun get(s: String?): String? {
        return s
    }
}
