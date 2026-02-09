/*
 * Copyright (C) 2009-2015 Johan Nilsson <http://markupartist.com>
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
class TravelMode(@JvmField val mode: String) {
    override fun toString(): String {
        return mode
    }

    companion object {
        const val FOOT: String = "foot"
        const val BIKE: String = "bike"
        const val BIKE_RENTAL: String = "bikeRent"
        const val CAR: String = "car"
        const val METRO: String = "metro"
        const val BUS: String = "bus"
        const val TRAIN: String = "train"
        const val LIGHT_TRAIN: String = "lightTrain"
        const val TRAM: String = "tram"
        const val BOAT: String = "boat"
    }
}
