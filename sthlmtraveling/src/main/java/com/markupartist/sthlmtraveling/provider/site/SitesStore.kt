package com.markupartist.sthlmtraveling.provider.site

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.core.util.Pair
import com.markupartist.sthlmtraveling.data.misc.HttpHelper
import com.markupartist.sthlmtraveling.provider.ApiConf
import com.markupartist.sthlmtraveling.provider.site.Site.Companion.fromJson
import com.markupartist.sthlmtraveling.utils.LocationUtils.parseLocation
import com.squareup.okhttp.Callback
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.regex.Matcher
import java.util.regex.Pattern

class SitesStore private constructor() {
    interface SiteCallback {
        fun onSuccess(sites: ArrayList<Site>)
        fun onError(error: IOException)
    }

    @Throws(IOException::class)
    fun getSite(context: Context?, name: String?): ArrayList<Site> {
        return getSiteV2(context, name)
    }

    @Throws(IOException::class)
    fun getSiteV2(context: Context?, name: String?): ArrayList<Site> {
        return getSiteV2(context, name, true)
    }

    @Throws(IOException::class)
    fun getSiteV2(context: Context?, name: String?, onlyStations: Boolean): ArrayList<Site> {
        if (TextUtils.isEmpty(name)) {
            return ArrayList()
        }

        val httpHelper = HttpHelper.getInstance(context)
        val onlyStationsParam = if (onlyStations) "true" else "false"
        val url = (ApiConf.apiEndpoint2() + "v1/site/"
                + "?q=" + URLEncoder.encode(name, "UTF-8")
                + "&onlyStations=" + onlyStationsParam
                + "&addLocation=true")
        val response = httpHelper.getClient().newCall(
            httpHelper.createRequest(url)
        ).execute()

        if (!response.isSuccessful()) {
            throw IOException("Server error while fetching sites")
        }

        try {
            return parseSitesFromResponse(response)
        } catch (e: JSONException) {
            throw IOException("Invalid input.")
        }
    }

    fun getSiteV2Async(
        context: Context?, name: String?,
        onlyStations: Boolean, callback: SiteCallback
    ) {
        if (TextUtils.isEmpty(name)) {
            Handler(Looper.getMainLooper()).post { callback.onSuccess(ArrayList()) }
            return
        }

        val httpHelper = HttpHelper.getInstance(context)
        val onlyStationsParam = if (onlyStations) "true" else "false"
        val url: String?
        try {
            url = (ApiConf.apiEndpoint2() + "v1/site/"
                    + "?q=" + URLEncoder.encode(name, "UTF-8")
                    + "&onlyStations=" + onlyStationsParam
                    + "&addLocation=true")

            val request = httpHelper.createRequest(url)
            httpHelper.getClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(request: Request?, e: IOException?) {
                    Handler(Looper.getMainLooper()).post { callback.onError(e!!) }
                }

                @Throws(IOException::class)
                override fun onResponse(response: Response) {
                    try {
                        if (!response.isSuccessful()) {
                            val error = IOException("Server error: " + response.code())
                            Handler(Looper.getMainLooper()).post { callback.onError(error) }
                            return
                        }

                        val sites = parseSitesFromResponse(response)
                        Handler(Looper.getMainLooper()).post { callback.onSuccess(sites) }
                    } catch (e: JSONException) {
                        val error = IOException("Invalid JSON", e)
                        Handler(Looper.getMainLooper()).post { callback.onError(error) }
                    }
                }
            })
        } catch (e: IOException) {
            Handler(Looper.getMainLooper()).post { callback.onError(e) }
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun parseSitesFromResponse(response: Response): ArrayList<Site> {
        val sites = ArrayList<Site>()
        val jsonResponse = JSONObject(response.body().string())
        if (!jsonResponse.has("sites")) {
            throw IOException("Invalid input.")
        }
        val jsonSites = jsonResponse.getJSONArray("sites")
        for (i in 0..<jsonSites.length()) {
            try {
                sites.add(fromJson(jsonSites.getJSONObject(i)))
            } catch (e: JSONException) {
                // Ignore individual parse errors
            }
        }
        return sites
    }

    /**
     * Find nearby [Site]s.
     *
     * @param location The location.
     * @return A list of [Site]s.
     * @throws IOException If failed to communicate with headend or if we can
     * not parse the response.
     */
    @Throws(IOException::class)
    fun nearby(context: Context?, location: Location): ArrayList<Site> {
        val endpoint = (ApiConf.apiEndpoint2() + "semistatic/site/near/"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&max_distance=0.8"
                + "&max_results=20")

        val httpHelper = HttpHelper.getInstance(context)
        val response = httpHelper.getClient().newCall(
            httpHelper.createRequest(endpoint)
        ).execute()

        if (!response.isSuccessful()) {
            Log.w("SiteStore", "Expected 200, got " + response.code())
            throw IOException("A remote server error occurred when getting sites.")
        }

        val rawContent = response.body().string()
        val stopPoints = ArrayList<Site>()
        try {
            val jsonSites = JSONObject(rawContent)
            if (jsonSites.has("sites")) {
                val jsonSitesArray = jsonSites.getJSONArray("sites")
                for (i in 0..<jsonSitesArray.length()) {
                    try {
                        val jsonStop = jsonSitesArray.getJSONObject(i)

                        val site = Site()
                        val nameAndLocality: Pair<String?, String?> =
                            nameAsNameAndLocality(jsonStop.getString("name"))
                        site.name = nameAndLocality.first
                        site.locality = nameAndLocality.second
                        site.setId(jsonStop.getInt("site_id"))
                        site.source = Site.SOURCE_STHLM_TRAVELING
                        site.type = Site.TYPE_TRANSIT_STOP
                        val locationData = jsonStop.optString("location")
                        if (locationData != null) {
                            site.location = parseLocation(locationData)
                        }

                        stopPoints.add(site)
                    } catch (e: JSONException) {
                        // Ignore errors here.
                    }
                }
            } else {
                throw IOException("Sites is not present in response.")
            }
        } catch (e: JSONException) {
            throw IOException("Invalid response.")
        }

        return stopPoints
    }

    companion object {
        private val SITE_NAME_PATTERN: Pattern = Pattern.compile("([\\w\\s0-9-& ]+)")

        @JvmStatic
        val instance: SitesStore by lazy { SitesStore() }

        @JvmStatic
        fun nameAsNameAndLocality(name: String): Pair<String?, String?> {
            val m: Matcher = SITE_NAME_PATTERN.matcher(name)
            var title = name
            var subtitle: String? = null
            if (m.find()) {
                if (m.groupCount() > 0) {
                    title = m.group(1).trim { it <= ' ' }
                }
                if (m.find()) {
                    if (m.groupCount() > 0) {
                        subtitle = m.group(1).trim { it <= ' ' }
                    }
                }
            }
            return Pair.create<String?, String?>(title, subtitle)
        }
    }
}
