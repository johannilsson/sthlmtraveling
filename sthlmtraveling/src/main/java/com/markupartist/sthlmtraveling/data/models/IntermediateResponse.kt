/*
 * Copyright (C) 2009-2016 Johan Nilsson <http://markupartist.com>
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
package com.markupartist.sthlmtraveling.data.models

/**
 *
 */
class IntermediateResponse(val result: MutableList<IntermediateStops>?) {
    fun getStops(reference: String): MutableList<IntermediateStop?>? {
        if (result == null || result.isEmpty()) {
            return mutableListOf<IntermediateStop?>()
        }
        for (`is` in result) {
            if (reference == `is`.reference) {
                return `is`.stops
            }
        }
        return mutableListOf<IntermediateStop?>()
    }
}
