package com.markupartist.sthlmtraveling.provider.deviation

import android.content.Context
import android.text.format.Time
import android.util.Log
import android.util.TimeFormatException
import com.markupartist.sthlmtraveling.data.misc.HttpHelper
import com.markupartist.sthlmtraveling.provider.ApiConf
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

class DeviationStore {
    @Throws(IOException::class)
    fun getDeviations(context: Context?): ArrayList<Deviation> {
        val deviations = ArrayList<Deviation>()

        try {
            val deviationsRawJson = retrieveDeviations(context)

            val jsonDeviations = JSONObject(deviationsRawJson)

            val jsonArray = jsonDeviations.getJSONArray("deviations")
            for (i in 0..<jsonArray.length()) {
                try {
                    val jsonDeviation = jsonArray.getJSONObject(i)

                    val created = Time()
                    created.parse(jsonDeviation.getString("created"))

                    val deviation = Deviation()
                    deviation.created = created
                    deviation.details =
                        stripNewLinesAtTheEnd(jsonDeviation.getString("description"))
                    deviation.header = jsonDeviation.getString("header")
                    //deviation.setLink(jsonDeviation.getString("link"));
                    //deviation.setMessageVersion(jsonDeviation.getInt("messageVersion"));
                    deviation.reference = jsonDeviation.getLong("reference")
                    deviation.scope = jsonDeviation.getString("scope")
                    deviation.scopeElements = jsonDeviation.getString("scope_elements")

                    //deviation.setSortOrder(jsonDeviation.getInt("sortOrder"));
                    deviations.add(deviation)
                } catch (e: TimeFormatException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return deviations
    }

    @Throws(IOException::class)
    private fun retrieveDeviations(context: Context?): String {
        val endpoint = ApiConf.apiEndpoint2() + "v1/deviation/"

        val httpHelper = HttpHelper.getInstance(context)
        val response = httpHelper.getClient().newCall(
            httpHelper.createRequest(endpoint)
        ).execute()

        if (!response.isSuccessful()) {
            throw IOException("A remote server error occurred when getting deviations.")
        }

        return response.body().string()
    }

    private fun stripNewLinesAtTheEnd(value: String): String {
        var value = value
        if (value.endsWith("\n")) {
            value = value.substring(0, value.length - 2)
            stripNewLinesAtTheEnd(value)
        }
        return value
    }

    @Throws(IOException::class)
    fun getTrafficStatus(context: Context?): TrafficStatus {
        val endpoint = ApiConf.apiEndpoint2() + "v1/trafficstatus/"

        val httpHelper = HttpHelper.getInstance(context)
        val response = httpHelper.getClient().newCall(
            httpHelper.createRequest(endpoint)
        ).execute()

        if (!response.isSuccessful()) {
            throw IOException("A remote server error occurred when getting traffic status.")
        }

        val rawContent = response.body().string()

        var ts: TrafficStatus? = null
        try {
            ts = TrafficStatus.Companion.fromJson(JSONObject(rawContent))
        } catch (e: JSONException) {
            Log.d(TAG, "Could not parse the reponse...")
            throw IOException("Could not parse the response.")
        }

        return ts
    }

    /**
     *
     */
    class TrafficStatus {
        @JvmField
        var trafficTypes: ArrayList<TrafficType> = ArrayList()

        override fun toString(): String {
            return "TrafficStatus{" +
                    "trafficTypes=" + trafficTypes +
                    '}'
        }

        companion object {
            const val GOOD: Int = 1
            const val MINOR: Int = 2
            const val MAJOR: Int = 3

            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): TrafficStatus {
                val ts = TrafficStatus()
                val jsonArray = jsonObject.getJSONArray("traffic_status")
                for (i in 0..<jsonArray.length()) {
                    try {
                        ts.trafficTypes.add(TrafficType.Companion.fromJson(jsonArray.getJSONObject(i)))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.d(TAG, "Failed to parse traffic type: " + e.message)
                    }
                }
                return ts
            }
        }
    }

    /**
     * Represents a traffic type.
     */
    class TrafficType {
        @JvmField
        var type: String? = null
        var expanded: Boolean = false
        var hasPlannedEvent: Boolean = false
        var status: Int = 0
        @JvmField
        var events: ArrayList<TrafficEvent> = ArrayList()

        override fun toString(): String {
            return "TrafficType{" +
                    "type='" + type + '\'' +
                    ", expanded=" + expanded +
                    ", hasPlannedEvent=" + hasPlannedEvent +
                    ", status=" + status +
                    ", events=" + events +
                    '}'
        }

        companion object {
            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): TrafficType {
                val tt = TrafficType()

                tt.type = jsonObject.getString("type")
                tt.expanded = jsonObject.getBoolean("expanded")
                tt.hasPlannedEvent = jsonObject.getBoolean("has_planned_event")
                tt.status = jsonObject.getInt("status")

                val jsonArray = jsonObject.getJSONArray("events")
                for (i in 0..<jsonArray.length()) {
                    try {
                        tt.events.add(TrafficEvent.Companion.fromJson(jsonArray.getJSONObject(i)))
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.d(TAG, "Failed to parse event: " + e.message)
                    }
                }

                return tt
            }
        }
    }

    /**
     * Represents a traffic event.
     */
    class TrafficEvent {
        @JvmField
        var message: String? = null
        var expanded: Boolean = false
        var planned: Boolean = false
        var sortIndex: Int = 0
        var infoUrl: String? = null
        @JvmField
        var status: Int = 0

        override fun toString(): String {
            return "TrafficEvent{" +
                    "message='" + message + '\'' +
                    ", expanded=" + expanded +
                    ", planned=" + planned +
                    ", sortIndex=" + sortIndex +
                    ", infoUrl='" + infoUrl + '\'' +
                    ", status='" + status + '\'' +
                    '}'
        }

        companion object {
            @Throws(JSONException::class)
            fun fromJson(jsonObject: JSONObject): TrafficEvent {
                val te = TrafficEvent()
                te.message = jsonObject.getString("message")
                te.expanded = jsonObject.getBoolean("expanded")
                te.planned = jsonObject.getBoolean("planned")
                te.sortIndex = jsonObject.getInt("sort_index")
                te.infoUrl = jsonObject.getString("info_url")
                te.status = jsonObject.getInt("status")
                return te
            }
        }
    }

    companion object {
        private const val TAG = "DeviationStore"
        private const val LINE_PATTERN = "[A-Za-zåäöÅÄÖ ]?([\\d]+)[ A-Z]?"
        private val sLinePattern: Pattern = Pattern.compile(LINE_PATTERN)

        @JvmStatic
        fun filterByLineNumbers(
            deviations: ArrayList<Deviation>, lineNumbers: ArrayList<Int>
        ): ArrayList<Deviation> {
            if (lineNumbers.isEmpty()) {
                return deviations
            }

            val filteredList = ArrayList<Deviation>()
            for (deviation in deviations) {
                val lines: ArrayList<Int> = Companion.extractLineNumbers(
                    deviation.scopeElements!!, null
                )
                //Log.d(TAG, "Matching " + lineNumbers.toString() + " against " + lines);
                for (line in lineNumbers) {
                    if (lines.contains(line)) {
                        filteredList.add(deviation)
                        // A deviation can span over several line numbers.
                        // Break on the first criteria match to not add the same
                        // deviation more than once.
                        break
                    }
                }
            }

            return filteredList
        }

        /**
         * Extract integer from the passed string recursively.
         * @param scope the string
         * @param foundIntegers previous found integer, pass null if you want to
         * start from scratch
         * @return the found integers or a empty ArrayList if none found
         */
        @JvmStatic
        fun extractLineNumbers(
            scope: String,
            foundIntegers: ArrayList<Int>?
        ): ArrayList<Int> {
            var scope = scope
            var foundIntegers = foundIntegers
            if (foundIntegers == null) foundIntegers = ArrayList()

            val matcher: Matcher = sLinePattern.matcher(scope)
            val matchFound = matcher.find()

            if (matchFound) {
                foundIntegers.add(matcher.group(1).toInt())
                scope = scope.replaceFirst(matcher.group(1).toRegex(), "") // remove what we found.
            } else {
                return foundIntegers
            }

            return extractLineNumbers(scope, foundIntegers)
        }
    }
}
