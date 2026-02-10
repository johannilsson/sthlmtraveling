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

import android.os.Parcelable
import androidx.core.util.Pair
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
class Route(
    val duration: Int,
    val legs: List<Leg>,
    val mode: String?,
    val fare: Fare?
) : Parcelable {

    fun departsAt(useTransitTime: Boolean): Pair<Date, RealTimeState> {
        if (useTransitTime) {
            for (leg in legs) {
                if (leg.isTransit) {
                    return Pair.create(leg.departsAt(), leg.realtimeState(true))
                }
            }
        }
        // If no transit leg, get the first.
        val leg = legs[0]
        return Pair.create(leg.departsAt(), leg.realtimeState(true))
    }

    fun arrivesAt(useTransitTime: Boolean): Pair<Date, RealTimeState> {
        if (useTransitTime) {
            for (i in legs.size - 1 downTo 0) {
                val leg = legs[i]
                if (leg.isTransit) {
                    return Pair.create(leg.arrivesAt(), leg.realtimeState(false))
                }
            }
        }
        val leg = legs[legs.size - 1]
        return Pair.create(leg.arrivesAt(), leg.realtimeState(false))
    }

    fun fromStop(): Place? {
        for (leg in legs) {
            if (leg.isTransit) {
                return leg.from
            }
        }
        // If no transit legs, get the first.
        return legs[0].from
    }

    fun toStop(): Place? {
        for (i in legs.size - 1 downTo 0) {
            val leg = legs[i]
            if (leg.isTransit) {
                return leg.to
            }
        }
        // If no transit leg, get the last.
        return legs[legs.size - 1].to
    }

    fun hasAlertsOrNotes(): Boolean {
        for (leg in legs) {
            if (leg.hasAlerts() || leg.hasNotes()) {
                return true
            }
        }
        return false
    }

    /**
     * @return returns true if this trip can be purchased with SMS.
     */
    fun canBuyTicket(): Boolean {
        return fare?.canBuyTicket() ?: false
    }
}
