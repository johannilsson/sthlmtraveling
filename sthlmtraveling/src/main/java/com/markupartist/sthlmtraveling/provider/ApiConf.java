package com.markupartist.sthlmtraveling.provider;

import com.markupartist.sthlmtraveling.AppConfig;

public class ApiConf {
   public static String KEY = AppConfig.STHLM_TRAVELING_API_KEY;

   public static String apiEndpoint2() {
       return AppConfig.STHLM_TRAVELING_API_ENDPOINT;
   }

   public static String get(String s) {
       return s;
   }

}
