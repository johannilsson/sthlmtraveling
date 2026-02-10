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
package com.markupartist.sthlmtraveling.provider.departure

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.markupartist.sthlmtraveling.data.misc.HttpHelper
import com.markupartist.sthlmtraveling.data.models.RealTimeState
import com.markupartist.sthlmtraveling.provider.ApiConf
import com.markupartist.sthlmtraveling.provider.site.Site
import com.markupartist.sthlmtraveling.utils.DateTimeUtil.fromDateTime
import com.markupartist.sthlmtraveling.utils.DateTimeUtil.getDelay
import com.markupartist.sthlmtraveling.utils.DateTimeUtil.getRealTimeStateFromDelay
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.Serializable

class DeparturesStore {
    @Throws(IllegalArgumentException::class, IOException::class)
    fun find(context: Context?, site: Site?): Departures {
        if (site == null) {
            Log.w(TAG, "Site is null")
            throw IllegalArgumentException(TAG + ", Site is null")
        }

        Log.d(TAG, "About to get departures for " + site.name)
        val endpoint = (ApiConf.apiEndpoint2()
                + "v1/departures/" + site.id
                + "?key=" + ApiConf.get(ApiConf.KEY)
                + "&timewindow=30")

        val httpHelper = HttpHelper.getInstance(context)
        val request = httpHelper.createRequest(endpoint)

        val client = httpHelper.getClient()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful()) {
            Log.w(
                TAG, "A remote server error occurred when getting departures, status code: " +
                        response.code()
            )
            throw IOException("A remote server error occurred when getting departures.")
        }

        val departures: Departures
        val rawContent = response.body().string()
        try {
            departures = Departures.Companion.fromJson(JSONObject(rawContent))
        } catch (e: JSONException) {
            Log.d(TAG, "Could not parse the departure reponse.")
            throw IOException("Could not parse the response.")
        }

        return departures
    }

    // TODO: Make this implement Parcelable
    class Departures : Serializable {
        var siteId: Int = 0
        @JvmField
        var servesTypes: ArrayList<String> = ArrayList()
        @JvmField
        var metros: ArrayList<MetroDeparture> = ArrayList()
        @JvmField
        var buses: ArrayList<BusDeparture> = ArrayList()
        @JvmField
        var trams: ArrayList<TramDeparture> = ArrayList()
        @JvmField
        var trains: ArrayList<TrainDeparture> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(json: JSONObject): Departures {
                val d = Departures()

                if (!json.isNull("serves_types")) {
                    val jsonServesTypes = json.getJSONArray("serves_types")
                    for (i in 0..<jsonServesTypes.length()) {
                        d.servesTypes.add(jsonServesTypes.getString(i))
                    }
                }

                if (!json.isNull("metros")) {
                    val jsonMetros = json.getJSONObject("metros")
                    if (jsonMetros.has("group_of_lines")) {
                        d.metros.add(MetroDeparture.Companion.fromJson(jsonMetros))
                    }
                }

                if (!json.isNull("buses")) {
                    val jsonBuses = json.getJSONArray("buses")
                    for (i in 0..<jsonBuses.length()) {
                        d.buses.add(BusDeparture.Companion.fromJson(jsonBuses.getJSONObject(i)))
                    }
                }

                if (!json.isNull("trams")) {
                    val jsonTrams = json.getJSONArray("trams")
                    for (i in 0..<jsonTrams.length()) {
                        d.trams.add(TramDeparture.Companion.fromJson(jsonTrams.getJSONObject(i)))
                    }
                }

                if (!json.isNull("trains")) {
                    val jsonTrains = json.getJSONArray("trains")
                    for (i in 0..<jsonTrains.length()) {
                        d.trains.add(TrainDeparture.Companion.fromJson(jsonTrains.getJSONObject(i)))
                    }
                }

                return d
            }
        }
    }

    open class Departure : Serializable {
        @JvmField
        var stopAreaName: String? = null
        var stopAreaNumber: String? = null
    }

    class MetroDeparture : Departure() {
        @JvmField
        var groupOfLines: ArrayList<GroupOfLine> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(json: JSONObject): MetroDeparture {
                val md = MetroDeparture()
                val jsonGroupOfLines = json.getJSONArray("group_of_lines")
                for (i in 0..<jsonGroupOfLines.length()) {
                    try {
                        md.groupOfLines.add(
                            GroupOfLine.Companion.fromJson(
                                jsonGroupOfLines.getJSONObject(
                                    i
                                )
                            )
                        )
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.d(TAG, "Failed to parse group of line for metros: " + e.message)
                    }
                }
                return md
            }
        }
    }

    class BusDeparture : Departure() {
        @JvmField
        var departures: ArrayList<DisplayRow> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): BusDeparture {
                val bd = BusDeparture()
                bd.stopAreaName = jsonObject.getString("stop_area_name")
                bd.stopAreaNumber = jsonObject.getString("stop_area_number")
                val jsonObjects = jsonObject.getJSONArray("departures")
                for (i in 0..<jsonObjects.length()) {
                    bd.departures.add(DisplayRow.Companion.fromJson(jsonObjects.getJSONObject(i)))
                }
                return bd
            }
        }
    }

    class TramDeparture : Departure() {
        @JvmField
        var direction1: ArrayList<DisplayRow> = ArrayList()
        @JvmField
        var direction2: ArrayList<DisplayRow> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): TramDeparture {
                val td = TramDeparture()
                td.stopAreaName = jsonObject.getString("stop_area_name")
                td.stopAreaNumber = jsonObject.getString("stop_area_number")

                val jsonDirection1 = jsonObject.getJSONArray("direction1")
                for (i in 0..<jsonDirection1.length()) {
                    td.direction1.add(DisplayRow.Companion.fromJson(jsonDirection1.getJSONObject(i)))
                }

                val jsonDirection2 = jsonObject.getJSONArray("direction2")
                for (i in 0..<jsonDirection2.length()) {
                    td.direction2.add(DisplayRow.Companion.fromJson(jsonDirection2.getJSONObject(i)))
                }

                return td
            }
        }
    }

    class TrainDeparture : Departure() {
        @JvmField
        var direction1: ArrayList<DisplayRow> = ArrayList()
        @JvmField
        var direction2: ArrayList<DisplayRow> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): TrainDeparture {
                val td = TrainDeparture()
                td.stopAreaName = jsonObject.getString("stop_area_name")
                td.stopAreaNumber = jsonObject.getString("stop_area_number")

                val jsonDirection1 = jsonObject.getJSONArray("direction1")
                for (i in 0..<jsonDirection1.length()) {
                    td.direction1.add(DisplayRow.Companion.fromJson(jsonDirection1.getJSONObject(i)))
                }

                val jsonDirection2 = jsonObject.getJSONArray("direction2")
                for (i in 0..<jsonDirection2.length()) {
                    td.direction2.add(DisplayRow.Companion.fromJson(jsonDirection2.getJSONObject(i)))
                }

                return td
            }
        }
    }

    class GroupOfLine : Serializable {
        var name: String? = null
        @JvmField
        var direction1: ArrayList<DisplayRow> = ArrayList()
        @JvmField
        var direction2: ArrayList<DisplayRow> = ArrayList()

        companion object {
            @Throws(JSONException::class)
            fun fromJson(json: JSONObject): GroupOfLine {
                val gol = GroupOfLine()

                gol.name = json.getString("name")

                val jsonDirection1 = json.getJSONArray("direction1")
                for (i in 0..<jsonDirection1.length()) {
                    val dr = DisplayRow.Companion.fromJson(jsonDirection1.getJSONObject(i))
                    if (dr.looksValid()) {
                        gol.direction1.add(dr)
                    }
                }
                val jsonDirection2 = json.getJSONArray("direction2")
                for (i in 0..<jsonDirection2.length()) {
                    val dr = DisplayRow.Companion.fromJson(jsonDirection2.getJSONObject(i))
                    if (dr.looksValid()) {
                        gol.direction2.add(dr)
                    }
                }

                return gol
            }
        }
    }

    class DisplayRow : Serializable {
        @JvmField
        var destination: String? = null
        @JvmField
        var lineNumber: String? = null
        @JvmField
        var lineName: String? = null
        @JvmField
        var displayTime: String? = null
        var timeTabledDateTime: String? = null
        var expectedDateTime: String? = null
        @JvmField
        var message: String? = null

        override fun toString(): String {
            return "DisplayRow{" +
                    "destination='" + destination + '\'' +
                    ", lineNumber='" + lineNumber + '\'' +
                    ", lineNamer='" + lineName + '\'' +
                    ", displayTime='" + displayTime + '\'' +
                    ", timeTabledDateTime='" + timeTabledDateTime + '\'' +
                    ", expectedDateTime='" + expectedDateTime + '\'' +
                    ", message='" + message + '\'' +
                    '}'
        }

        val realTimeState: RealTimeState
            get() {
                if (TextUtils.isEmpty(timeTabledDateTime) && TextUtils.isEmpty(expectedDateTime)) {
                    // We only have display time present, assume it is real-time and on time.
                    return RealTimeState.ON_TIME
                }

                if (!TextUtils.isEmpty(displayTime) && displayTime!!.contains(":")) {
                    return RealTimeState.NOT_SET
                }

                if (!TextUtils.isEmpty(timeTabledDateTime) && !TextUtils.isEmpty(expectedDateTime)) {
                    val scheduled = fromDateTime(timeTabledDateTime)
                    val expected = fromDateTime(expectedDateTime)
                    val delay = getDelay(scheduled, expected)
                    return getRealTimeStateFromDelay(delay)
                }

                return RealTimeState.NOT_SET
            }

        fun looksValid(): Boolean {
            return (!TextUtils.isEmpty(destination)
                    || !TextUtils.isEmpty(message))
        }

        companion object {
            @Throws(JSONException::class)
            fun fromJson(json: JSONObject): DisplayRow {
                val dr = DisplayRow()
                if (json.has("destination")) {
                    dr.destination =
                        if (json.isNull("destination")) null else json.getString("destination")
                }
                if (json.has("line_number")) {
                    dr.lineNumber =
                        if (json.isNull("line_number")) null else json.getString("line_number")
                }
                if (json.has("line_name")) {
                    dr.lineName =
                        if (json.isNull("line_name")) null else json.getString("line_name")
                }
                if (json.has("display_time")) {
                    dr.displayTime =
                        if (json.isNull("display_time")) null else json.getString("display_time")
                }
                if (json.has("time_tabled_date_time")) {
                    dr.timeTabledDateTime =
                        if (json.isNull("time_tabled_date_time")) null else json.getString("time_tabled_date_time")
                }
                if (json.has("expected_date_time")) {
                    dr.expectedDateTime =
                        if (json.isNull("expected_date_time")) null else json.getString("expected_date_time")
                }
                if (json.has("message")) {
                    dr.message = if (json.isNull("message")) null else json.getString("message")
                }

                return dr
            }
        }
    }

    companion object {
        private const val TAG = "DeparturesStore"
    }
}
