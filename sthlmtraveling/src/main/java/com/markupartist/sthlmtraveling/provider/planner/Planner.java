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

package com.markupartist.sthlmtraveling.provider.planner;

import com.markupartist.sthlmtraveling.R;

/**
 * Journey planner for the sl.se API.
 */
public class Planner {

    /**
     * Constructs a new Planner
     */
    private Planner() {
    }

    public static int plannerErrorCodeToStringRes(String errorCode) {
        // Make sure to have the common error codes as high as possible.
        if ("H9220".equals(errorCode)) {
            return R.string.planner_error_H9220;
        } else if ("H895".equals(errorCode)) {
            return R.string.planner_error_H895;
        } else if ("H9300".equals(errorCode)) {
            return R.string.planner_error_H9300;
        } else if ("H9360".equals(errorCode)) {
            return R.string.planner_error_H9360;
        } else if ("H9380".equals(errorCode)) {
            return R.string.planner_error_H9380;
        } else if ("H9320".equals(errorCode)) {
            return R.string.planner_error_H9320;
        } else if ("H9280".equals(errorCode)) {
            return R.string.planner_error_H9280;
        } else if ("H9260".equals(errorCode)) {
            return R.string.planner_error_H9260;
        } else if ("H9250".equals(errorCode)) {
            return R.string.planner_error_H9250;
        } else if ("H9240".equals(errorCode)) {
            return R.string.planner_error_H9240;
        } else if ("H9230".equals(errorCode)) {
            return R.string.planner_error_H9230;
        } else if ("H900".equals(errorCode)) {
            return R.string.planner_error_H900;
        } else if ("H892".equals(errorCode)) {
            return R.string.planner_error_H892;
        } else if ("H891".equals(errorCode)) {
            return R.string.planner_error_H891;
        } else if ("H890".equals(errorCode)) {
            return R.string.planner_error_H890;
        } else if ("H500".equals(errorCode)) {
            return R.string.planner_error_H500;
        } else if ("H455".equals(errorCode)) {
            return R.string.planner_error_H455;
        } else if ("H410".equals(errorCode)) {
            return R.string.planner_error_H410;
        } else if ("H390".equals(errorCode)) {
            return R.string.planner_error_H390;
        }

        return R.string.planner_error_unknown;
    }

}
