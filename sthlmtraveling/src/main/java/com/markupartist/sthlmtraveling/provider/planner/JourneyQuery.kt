/*
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
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

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.markupartist.sthlmtraveling.data.api.PlaceQuery
import com.markupartist.sthlmtraveling.data.api.TravelModeQuery
import com.markupartist.sthlmtraveling.data.models.TravelMode
import com.markupartist.sthlmtraveling.provider.TransportMode
import com.markupartist.sthlmtraveling.provider.site.Site
import com.markupartist.sthlmtraveling.utils.DateTimeUtil
import com.markupartist.sthlmtraveling.utils.DateTimeUtil.parse2445
import com.markupartist.sthlmtraveling.utils.LegUtil
import com.markupartist.sthlmtraveling.utils.LegUtil.travelModesToTransportModes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Arrays
import java.util.Date

class JourneyQuery : Parcelable {
    @JvmField
    var origin: Site? = null
    @JvmField
    var destination: Site? = null
    @JvmField
    var via: Site? = null
    @JvmField
    var time: Date? = null
    @JvmField
    var isTimeDeparture: Boolean = true
    @JvmField
    var alternativeStops: Boolean = false
    @JvmField
    var transportModes: MutableList<String> = ArrayList()
    @JvmField
    var ident: String? = null
    @JvmField
    var hasPromotions: Boolean = false
    @JvmField
    var promotionNetwork: Int = -1

    // Storing the state of the current ident and scroll dir to allow refresh of paginated
    // results
    @JvmField
    var previousIdent: String? = null
    @JvmField
    var previousDir: String? = null

    constructor()

    constructor(parcel: Parcel) {
        origin = parcel.readParcelable<Site?>(Site::class.java.getClassLoader())
        destination = parcel.readParcelable<Site?>(Site::class.java.getClassLoader())
        via = parcel.readParcelable<Site?>(Site::class.java.getClassLoader())
        time = Date(parcel.readLong())
        isTimeDeparture = (parcel.readInt() == 1)
        alternativeStops = (parcel.readInt() == 1)
        transportModes = ArrayList()
        parcel.readStringList(transportModes)
        ident = parcel.readString()
        hasPromotions = (parcel.readInt() == 1)
        promotionNetwork = parcel.readInt()
        previousIdent = parcel.readString()
        previousDir = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(origin, 0)
        dest.writeParcelable(destination, 0)
        dest.writeParcelable(via, 0)
        dest.writeLong(time!!.getTime())
        dest.writeInt(if (isTimeDeparture) 1 else 0)
        dest.writeInt(if (alternativeStops) 1 else 0)
        dest.writeStringList(transportModes)
        dest.writeString(ident)
        dest.writeInt(if (hasPromotions) 1 else 0)
        dest.writeInt(promotionNetwork)
        dest.writeString(previousIdent)
        dest.writeString(previousDir)
    }

    /**
     * Checks if the query contains has via.
     *
     * @return Returns `true` if a via location is set,
     * `false` otherwise.
     */
    fun hasVia(): Boolean {
        return via != null && via!!.hasName()
    }

    /**
     * Returns true if anything has than the defaults has been modified.
     *
     * @return true if any filtering is active.
     */
    fun hasAdditionalFiltering(): Boolean {
        if (hasVia()) {
            return true
        }
        if (alternativeStops) {
            return true
        }
        if (transportModes != null) {
            return !hasDefaultTransportModes()
        }
        return false
    }

    fun hasDefaultTransportModes(): Boolean {
        val defaults = listOf(
            TransportMode.METRO, TransportMode.BUS,
            TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM
        )
        return transportModes.containsAll(defaults)
    }


    @Throws(JSONException::class)
    fun toJson(all: Boolean): JSONObject {
        val jsonOrigin: JSONObject = Companion.siteToJson(origin!!)
        val jsonDestination: JSONObject = Companion.siteToJson(destination!!)

        val jsonQuery = JSONObject()
        if (via != null) {
            val jsonVia: JSONObject = Companion.siteToJson(via!!)
            jsonQuery.put("via", jsonVia)
        }

        if (transportModes.isNotEmpty()) {
            jsonQuery.put("transportModes", JSONArray(transportModes))
        }

        jsonQuery.put("alternativeStops", alternativeStops)
        jsonQuery.put("origin", jsonOrigin)
        jsonQuery.put("destination", jsonDestination)

        //        if (all) {
//            jsonQuery.put("ident", ident);
//            jsonQuery.put("time", time.format("%F %R"));
//            jsonQuery.put("isTimeDeparture", this.isTimeDeparture);
//        }
        return jsonQuery
    }

    fun toUri(withTime: Boolean): Uri? {
        if (origin == null || destination == null) {
            return null
        }

        var routesUri: Uri

        val fromQuery = PlaceQuery.Builder().place(origin!!.asPlace()).build()
        val toQuery = PlaceQuery.Builder().place(destination!!.asPlace()).build()

        routesUri = Uri.parse("journeyplanner://routes?")

        routesUri = routesUri.buildUpon()
            .appendQueryParameter("version", "2")
            .appendQueryParameter("from", Uri.encode(fromQuery.toString()))
            .appendQueryParameter("fromSource", origin!!.source.toString())
            .appendQueryParameter("to", Uri.encode(toQuery.toString()))
            .appendQueryParameter("toSource", destination!!.source.toString())
            .appendQueryParameter("alternative", alternativeStops.toString())
            .build()

        if (withTime) {
            if (time != null) {
                val timeString = DateTimeUtil.format2445(time!!)
                routesUri = routesUri.buildUpon()
                    .appendQueryParameter("time", timeString)
                    .appendQueryParameter("isTimeDeparture", isTimeDeparture.toString())
                    .build()
            }
        }

        if (hasVia()) {
            routesUri = routesUri.buildUpon()
                .appendQueryParameter(
                    "via", Uri.encode(
                        PlaceQuery.Builder().place(via!!.asPlace()).build().toString()
                    )
                )
                .build()
        }

        if (transportModes.isNotEmpty()) {
            // Convert transport modes to travel modes.
            val travelModes: List<TravelMode> =
                LegUtil.transportModesToTravelModes(transportModes)
            val travelModeQuery = TravelModeQuery(travelModes)
            routesUri = routesUri.buildUpon()
                .appendQueryParameter("travelMode", travelModeQuery.toString())
                .build()
        }

        return routesUri
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return ("JourneyQuery [alternativeStops=" + alternativeStops
                + ", destination=" + destination + ", ident=" + ident
                + ", isTimeDeparture=" + isTimeDeparture + ", origin=" + origin
                + ", time=" + time + ", transportModes="
                + transportModes + ", via=" + via + "]")
    }

    class Builder {
        private var mOrigin: Site? = null
        private var mDestination: Site? = null
        private var mVia: Site? = null
        private var mTime: Date? = null
        private var mIsTimeDeparture = true
        private var mAlternativeStops = false
        private var mTransportModes: MutableList<String> = ArrayList()

        fun origin(origin: Site): Builder {
            mOrigin = origin
            return this
        }

        fun origin(name: String?, latitude: Int, longitude: Int): Builder {
            mOrigin = Site()
            mOrigin!!.name = name
            mOrigin!!.setLocation(latitude, longitude)
            return this
        }

        @Throws(JSONException::class)
        fun origin(jsonObject: JSONObject): Builder {
            mOrigin = jsonToSite(jsonObject)
            return this
        }

        fun destination(destination: Site): Builder {
            mDestination = destination
            return this
        }

        @Throws(JSONException::class)
        fun destination(jsonObject: JSONObject): Builder {
            mDestination = jsonToSite(jsonObject)
            return this
        }

        fun destination(name: String?, latitude: Int, longitude: Int): Builder {
            mDestination = Site()
            mDestination!!.name = name
            mDestination!!.setLocation(latitude, longitude)
            return this
        }

        fun via(via: Site?): Builder {
            if (via != null && via.hasName()) {
                mVia = via
            }
            return this
        }

        @Throws(JSONException::class)
        fun via(jsonObject: JSONObject): Builder {
            mVia = jsonToSite(jsonObject)
            return this
        }

        fun time(time: Date?): Builder {
            mTime = time
            return this
        }

        fun isTimeDeparture(isTimeDeparture: Boolean): Builder {
            mIsTimeDeparture = isTimeDeparture
            return this
        }

        fun alternativeStops(alternativeStops: Boolean): Builder {
            mAlternativeStops = alternativeStops
            return this
        }

        fun transportModes(transportModes: MutableList<String>): Builder {
            mTransportModes = transportModes
            return this
        }

        @Throws(JSONException::class)
        fun transportModes(jsonArray: JSONArray?): Builder {
            if (jsonArray == null) {
                return this
            }
            mTransportModes = ArrayList()
            for (i in 0..<jsonArray.length()) {
                mTransportModes.add(jsonArray.getString(i))
            }
            return this
        }

        fun uri(uri: Uri): Builder {
            val version = uri.getQueryParameter("version")
            if (version == null) {
                return uriV1(uri)
            }
            return uriV2(uri)
        }

        fun uriV2(uri: Uri): Builder {
            val origin = fromQueryParameter(Uri.decode(uri.getQueryParameter("from")))
            origin?.source = uri.getQueryParameter("fromSource")!!.toInt()
            origin(origin!!)

            val destination = fromQueryParameter(Uri.decode(uri.getQueryParameter("to")))
            destination?.source = uri.getQueryParameter("fromSource")!!.toInt()
            destination(destination!!)

            via(fromQueryParameter(Uri.decode(uri.getQueryParameter("via"))))
            alternativeStops(uri.getQueryParameter("alternative").toBoolean())

            val timeStr = uri.getQueryParameter("time")
            if (timeStr != null) {
                time(parse2445(timeStr))
                isTimeDeparture(uri.getQueryParameter("isTimeDeparture").toBoolean())
            } else {
                isTimeDeparture(true)
            }

            val travelModeQuery = TravelModeQuery.fromStringList(
                Uri.decode(uri.getQueryParameter("travelMode"))
            )
            val modes = travelModesToTransportModes(travelModeQuery.getModes())
            transportModes(ArrayList(modes))

            return this
        }

        fun fromQueryParameter(parameter: String?): Site? {
            if (parameter == null) {
                return null
            }
            val site = Site()
            var fromQuery = PlaceQuery.Builder().param(parameter).build()
            site.name = fromQuery!!.getName()

            if (fromQuery.getId() != null) {
                site.setId(fromQuery.getId())
            }
            if (fromQuery.getLat() != 0.0 && fromQuery.getLon() != 0.0) {
                site.setLocation(fromQuery.getLat(), fromQuery.getLon())
            }

            fromQuery = null
            return site
        }

        fun uriV1(uri: Uri): Builder {
            val origin = Site()
            origin.name = uri.getQueryParameter("start_point")
            val originId = uri.getQueryParameter("start_point_id")
            if (!TextUtils.isEmpty(originId) && "null" != originId) {
                origin.setId(originId)
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))
            ) {
                origin.setLocation(
                    (uri.getQueryParameter("start_point_lat")!!.toDouble() * 1E6).toInt(),
                    (uri.getQueryParameter("start_point_lng")!!.toDouble() * 1E6).toInt()
                )
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_source"))) {
                origin.source = uri.getQueryParameter("start_point_source")!!.toInt()
            }
            this.origin(origin)

            val destination = Site()
            destination.name = uri.getQueryParameter("end_point")
            val destinationId = uri.getQueryParameter("end_point_id")
            if (!TextUtils.isEmpty(destinationId) && "null" != destinationId) {
                destination.setId(destinationId)
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_lat"))
                && !TextUtils.isEmpty(uri.getQueryParameter("end_point_lng"))
            ) {
                destination.setLocation(
                    (uri.getQueryParameter("end_point_lat")!!.toDouble() * 1E6).toInt(),
                    (uri.getQueryParameter("end_point_lng")!!.toDouble() * 1E6).toInt()
                )
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_source"))) {
                destination.source = uri.getQueryParameter("end_point_source")!!.toInt()
            }
            this.destination(destination)

            var isTimeDeparture = true
            if (!TextUtils.isEmpty(uri.getQueryParameter("isTimeDeparture"))) {
                isTimeDeparture = uri.getQueryParameter("isTimeDeparture").toBoolean()
            }
            this.isTimeDeparture(isTimeDeparture)


            val time = Date()
            val timeString = uri.getQueryParameter("time")
            if (!TextUtils.isEmpty(timeString)) {
                // TODO: What is the format here?
                //jq.time.parse(timeString);
            } else {
                time.setTime(System.currentTimeMillis())
            }
            this.time(time)

            return this
        }

        fun create(): JourneyQuery {
            val journeyQuery = JourneyQuery()
            journeyQuery.origin = mOrigin
            journeyQuery.destination = mDestination
            journeyQuery.via = mVia

            if (mTime == null) {
                mTime = Date()
            }
            journeyQuery.time = mTime
            journeyQuery.isTimeDeparture = mIsTimeDeparture
            journeyQuery.alternativeStops = mAlternativeStops
            if (mTransportModes.isEmpty()) {
                mTransportModes = mutableListOf(
                    TransportMode.METRO, TransportMode.BUS,
                    TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM
                )
            }
            journeyQuery.transportModes = mTransportModes

            return journeyQuery
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<JourneyQuery?> =
            object : Parcelable.Creator<JourneyQuery?> {
                override fun createFromParcel(parcel: Parcel): JourneyQuery {
                    return JourneyQuery(parcel)
                }

                override fun newArray(size: Int): Array<JourneyQuery?> {
                    return arrayOfNulls<JourneyQuery>(size)
                }
            }

        @Throws(JSONException::class)
        fun siteToJson(site: Site): JSONObject {
            val json = JSONObject()

            json.put("id", site.id)
            json.put("name", site.name)
            if (site.isMyLocation) {
                json.put("latitude", 0)
                json.put("longitude", 0)
            } else if (site.hasLocation()) {
                json.put("latitude", (site.location!!.getLatitude() * 1E6).toInt())
                json.put("longitude", (site.location!!.getLongitude() * 1E6).toInt())
            }
            json.put("source", site.source)
            json.put("locality", site.locality)

            return json
        }

        @Throws(JSONException::class)
        fun jsonToSite(json: JSONObject): Site {
            val site = Site()

            if (json.has("id")) {
                site.setId(json.getString("id"))
            }
            if (json.has("locality")) {
                site.locality = json.getString("locality")
            }
            if (json.has("source")) {
                site.source = json.getInt("source")
            }
            if (json.has("latitude") && json.has("longitude")) {
                site.setLocation(json.getInt("latitude"), json.getInt("longitude"))
            }

            site.name = json.getString("name")

            return site
        }


        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(jsonObject: JSONObject): JourneyQuery {
            val journeyQuery = Builder()
                .origin(jsonObject.getJSONObject("origin"))
                .destination(jsonObject.getJSONObject("destination"))
                .transportModes(
                    if (jsonObject.has("transportModes"))
                        jsonObject.getJSONArray("transportModes")
                    else
                        null
                )
                .create()

            if (jsonObject.has("isTimeDeparture")) {
                journeyQuery.isTimeDeparture =
                    jsonObject.getBoolean("isTimeDeparture")
            }
            if (jsonObject.has("alternativeStops")) {
                journeyQuery.alternativeStops =
                    jsonObject.getBoolean("alternativeStops")
            }
            if (jsonObject.has("via")) {
                val jsonVia = jsonObject.getJSONObject("via")
                journeyQuery.via = jsonToSite(jsonVia)
            }

            return journeyQuery
        }
    }
}
