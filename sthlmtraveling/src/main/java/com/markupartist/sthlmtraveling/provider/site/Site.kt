package com.markupartist.sthlmtraveling.provider.site

import android.location.Location
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import com.markupartist.sthlmtraveling.data.models.Place
import org.json.JSONException
import org.json.JSONObject

class Site : Parcelable {
    /**
     * @return the id
     */
    var id: String? = null
        private set
    private var mName: String? = null
    var locality: String? = null
    var type: String? = null
    var location: Location? = null
    var source: Int = SOURCE_STHLM_TRAVELING

    constructor()

    /**
     * Create a new Stop that is a copy of the given Stop.
     * @param site the site
     */
    constructor(site: Site) {
        mName = site.name
        this.locality = site.locality
        this.location = site.location
        this.id = site.id
        this.type = site.type
        this.source = site.source
    }

    constructor(parcel: Parcel) {
        this.id = parcel.readString()
        mName = parcel.readString()
        this.type = parcel.readString()
        val latitude = parcel.readDouble()
        val longitude = parcel.readDouble()
        if (latitude > 0 && longitude > 0) {
            val location = Location("sthlmtraveling")
            location.setLatitude(latitude)
            location.setLongitude(longitude)
            this.location = location
        }
        this.locality = parcel.readString()
        this.source = parcel.readInt()
    }

    var name: String?
        /**
         * @return the name
         */
        get() = mName
        /**
         * @param name the name to set
         */
        set(name) {
            if (!TextUtils.isEmpty(name)) {
                if (name == TYPE_MY_LOCATION) {
                    mName = name
                } else {
                    //mName = name.trim().replaceAll(NAME_RE, "");
                    mName = name
                }
            }
        }

    /**
     * @param id the id to set
     */
    fun setId(id: Int) {
        if (id == 0) {
            this.id = null
        } else {
            this.id = id.toString()
        }
    }

    /**
     * @param id the id to set
     */
    fun setId(id: String?) {
        this.id = id
    }

    val isAddress: Boolean
        get() = this.type != null && this.type == "A"

    val isTransitStop: Boolean
        get() = this.type != null && this.type == "S"

    fun hasType(): Boolean {
        return !TextUtils.isEmpty(this.type)
    }

    fun hasName(): Boolean {
        return !TextUtils.isEmpty(mName)
    }

    override fun toString(): String {
        return mName!! // This is used by adapters.
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(this.id)
        parcel.writeString(mName)
        parcel.writeString(this.type)
        if (this.hasLocation()) {
            parcel.writeDouble(location!!.getLatitude())
            parcel.writeDouble(location!!.getLongitude())
        } else {
            parcel.writeDouble(0.0)
            parcel.writeDouble(0.0)
        }
        parcel.writeString(this.locality)
        parcel.writeInt(this.source)
    }

    val isMyLocation: Boolean
        get() = hasName() && mName == TYPE_MY_LOCATION

    fun looksValid(): Boolean {
        if (this.isMyLocation) {
            return true
        }
        if (hasLocation() && hasName() && this.id == null) {
            return true
        }
        if (hasName() && this.id != null) {
            return true
        }
        return false
    }

    /**
     * Fill this Site with the values from another Site. If other is null this will be nullified.
     * @param value
     */
    fun fromSite(value: Site?) {
        if (value != null) {
            this.id = value.id
            this.location = value.location
            mName = value.mName
            this.type = value.type
            this.locality = value.locality
        } else {
            this.id = null
            this.location = null
            mName = null
            this.type = null
            this.locality = null
        }
    }

    val nameOrId: String?
        get() {
            if (hasLocation() || this.id == null) {
                return mName
            }
            return id.toString()
        }

    fun hasLocation(): Boolean {
        return this.location != null
    }

    fun setLocation(lat: Double, lng: Double) {
        this.location = Location("sthlmtraveling")
        location!!.setLatitude(lat)
        location!!.setLongitude(lng)
    }

    fun setLocation(lat: Int, lng: Int) {
        if (lat == 0 || lng == 0) {
            return
        }
        this.location = Location("sthlmtraveling")
        location!!.setLatitude(lat / 1E6)
        location!!.setLongitude(lng / 1E6)
    }

    fun toDump(): String {
        return ("Site [mId=" + this.id
                + ", mName=" + mName
                + ", mType=" + this.type
                + ", mLocation=" + this.location
                + ", mSource=" + this.source
                + ", mLocality=" + this.locality
                + "]")
    }

    fun asPlace(): Place {
        // If type is sthlm traveling and id is not 0.
        val id = if (this.source == SOURCE_STHLM_TRAVELING && "0" != this.id) this.id else null

        var lat = 0.0
        var lon = 0.0
        if (hasLocation()) {
            lat = location!!.getLatitude()
            lon = location!!.getLongitude()
        }
        return Place(
            id,
            mName,
            if (this.isTransitStop) "stop" else "place",
            lat, lon, -1, null, null
        )
    }

    companion object {
        const val TYPE_MY_LOCATION: String = "MY_LOCATION"

        const val TYPE_TRANSIT_STOP: String = "S"
        const val TYPE_ADDRESS: String = "A"

        const val CATEGORY_UNKNOWN: Int = 0
        const val CATEGORY_TRANSIT_STOP: Int = 1
        const val CATEGORY_ADDRESS: Int = 2

        const val SOURCE_STHLM_TRAVELING: Int = 0
        const val SOURCE_GOOGLE_PLACES: Int = 1
        private const val NAME_RE = "[^\\p{Alnum}\\(\\)\\s]"

        @JvmField
        val CREATOR: Parcelable.Creator<Site?> = object : Parcelable.Creator<Site?> {
            override fun createFromParcel(`in`: Parcel): Site {
                return Site(`in`)
            }

            override fun newArray(size: Int): Array<Site?> {
                return arrayOfNulls<Site>(size)
            }
        }

        //    public static Site fromPlannerLocation(Planner.Location loc) {
        //        Site s = new Site();
        //        s.setId(String.valueOf(loc.id));
        //        s.setLocation(loc.latitude, loc.longitude);
        //        s.setName(loc.name);
        //        return s;
        //    }
        @JvmStatic
        @Throws(JSONException::class)
        fun fromJson(json: JSONObject): Site {
            val site = Site()
            site.source =
                SOURCE_STHLM_TRAVELING
            site.setId(json.getInt("site_id").toString())

            val nameAndLocality =
                SitesStore.nameAsNameAndLocality(json.getString("name"))
            site.name = nameAndLocality.first
            site.locality = nameAndLocality.second
            if (json.has("type")) {
                site.type = json.getString("type")
            }
            if (json.has("location") && !json.isNull("location")) {
                val locationJson = json.getJSONObject("location")
                try {
                    val location = Location("sthlmtraveling")
                    location.setLatitude(locationJson.getDouble("latitude"))
                    location.setLongitude(locationJson.getDouble("longitude"))
                    site.location = location
                } catch (e: Exception) {
                    Log.e("SITE", e.message!!)
                }
            }
            return site
        }

        @JvmStatic
        fun looksValid(name: String?): Boolean {
            if (TextUtils.isEmpty(name) || TextUtils.getTrimmedLength(name) == 0) {
                return false
            }
            return !name!!.matches(NAME_RE.toRegex())
        }

        @JvmStatic
        fun toSite(place: Place): Site {
            val site = Site()
            site.source =
                SOURCE_STHLM_TRAVELING
            site.setId(place.id)
            site.name = place.name
            site.type =
                if ("stop" == place.type) TYPE_TRANSIT_STOP else TYPE_ADDRESS
            if (place.hasLocation()) {
                site.setLocation(place.lat, place.lon)
            }
            return site
        }
    }
}
