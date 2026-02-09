/*
 * Copyright 2008, 2013 Google Inc.
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

package com.markupartist.sthlmtraveling.utils

import com.google.android.gms.maps.model.LatLng

/**
 * Adapted from https://github.com/googlemaps/android-maps-utils
 */
object PolyUtil {
    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    @JvmStatic
    fun decode(encodedPath: String): List<LatLng> {
        val len = encodedPath.length

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        val path = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < len) {
            var result = 1
            var shift = 0
            var b: Int
            do {
                b = encodedPath[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lat += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

            result = 1
            shift = 0
            do {
                b = encodedPath[index++].code - 63 - 1
                result += b shl shift
                shift += 5
            } while (b >= 0x1f)
            lng += if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)

            path.add(LatLng(lat * 1e-5, lng * 1e-5))
        }

        return path
    }
}
