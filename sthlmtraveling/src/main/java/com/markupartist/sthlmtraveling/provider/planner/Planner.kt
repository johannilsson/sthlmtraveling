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
package com.markupartist.sthlmtraveling.provider.planner

import com.markupartist.sthlmtraveling.R

/**
 * Journey planner for the sl.se API.
 */
object Planner {
    @JvmStatic
    fun plannerErrorCodeToStringRes(errorCode: String?): Int {
        // Make sure to have the common error codes as high as possible.
        return when (errorCode) {
            "H9220" -> R.string.planner_error_H9220
            "H895" -> R.string.planner_error_H895
            "H9300" -> R.string.planner_error_H9300
            "H9360" -> R.string.planner_error_H9360
            "H9380" -> R.string.planner_error_H9380
            "H9320" -> R.string.planner_error_H9320
            "H9280" -> R.string.planner_error_H9280
            "H9260" -> R.string.planner_error_H9260
            "H9250" -> R.string.planner_error_H9250
            "H9240" -> R.string.planner_error_H9240
            "H9230" -> R.string.planner_error_H9230
            "H900" -> R.string.planner_error_H900
            "H892" -> R.string.planner_error_H892
            "H891" -> R.string.planner_error_H891
            "H890" -> R.string.planner_error_H890
            "H500" -> R.string.planner_error_H500
            "H455" -> R.string.planner_error_H455
            "H410" -> R.string.planner_error_H410
            "H390" -> R.string.planner_error_H390
            else -> R.string.planner_error_unknown
        }
    }
}
