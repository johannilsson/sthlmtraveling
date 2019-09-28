package com.markupartist.sthlmtraveling.provider.site;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.data.models.Place;

import org.json.JSONException;
import org.json.JSONObject;

public class Site implements Parcelable {
    public static final String TYPE_MY_LOCATION = "MY_LOCATION";

    public static final String TYPE_TRANSIT_STOP = "S";
    public static final String TYPE_ADDRESS = "A";

    public static final int CATEGORY_UNKNOWN = 0;
    public static final int CATEGORY_TRANSIT_STOP = 1;
    public static final int CATEGORY_ADDRESS = 2;

    public static int SOURCE_STHLM_TRAVELING = 0;
    public static int SOURCE_GOOGLE_PLACES = 1;
    private static String NAME_RE = "[^\\p{Alnum}\\(\\)\\s]";

    private String mId;
    private String mName;
    private String mLocality;
    private String mType;
    private Location mLocation;
    private int mSource = SOURCE_STHLM_TRAVELING;

    public Site() {
    }

    /**
     * Create a new Stop that is a copy of the given Stop.
     * @param site the site
     */
    public Site(Site site) {
        mName = site.getName();
        mLocality = site.getLocality();
        mLocation = site.getLocation();
        mId = site.getId();
        mType = site.getType();
        mSource = site.getSource();
    }

    public String getType() {
        return mType;
    }

    public Site(Parcel parcel) {
        mId = parcel.readString();
        mName = parcel.readString();
        mType = parcel.readString();
        double latitude = parcel.readDouble();
        double longitude = parcel.readDouble();
        if (latitude > 0 && longitude > 0) {
            Location location = new Location("sthlmtraveling");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            setLocation(location);
        }
        mLocality = parcel.readString();
        mSource = parcel.readInt();
    }

    /**
     * @return the id
     */
    public String getId() {
        return mId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return mName;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        if (id == 0) {
            mId = null;
        } else {
            mId = String.valueOf(id);
        }
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        mId = id;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        if (!TextUtils.isEmpty(name)) {
            if (name.equals(TYPE_MY_LOCATION)) {
                mName = name;
            } else {
                //mName = name.trim().replaceAll(NAME_RE, "");
                mName = name;
            }
        }
    }

    public void setLocality(String locality) {
        mLocality = locality;
    }

    public String getLocality() {
        return mLocality;
    }

    public int getSource() {
        return mSource;
    }

    public void setSource(int source) {
        this.mSource = source;
    }

    public void setType(String type) {
        mType = type;
    }

    public void setLocation(Location location) {
        mLocation = location;
    }

    public Location getLocation() {
        return mLocation;
    }

    public boolean isAddress() {
        return mType != null && mType.equals("A");
    }

    public boolean isTransitStop() {
        return mType != null && mType.equals("S");
    }

    public boolean hasType() {
        return !TextUtils.isEmpty(mType);
    }

    public boolean hasName() {
        return !TextUtils.isEmpty(mName);
    }

    @Override
    public String toString() {
        return mName;  // This is used by adapters.
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mId);
        parcel.writeString(mName);
        parcel.writeString(mType);
        if (this.hasLocation()) {
            parcel.writeDouble(mLocation.getLatitude());
            parcel.writeDouble(mLocation.getLongitude());
        } else {
            parcel.writeDouble(0);
            parcel.writeDouble(0);
        }
        parcel.writeString(mLocality);
        parcel.writeInt(mSource);
    }

    public static final Parcelable.Creator<Site> CREATOR = new Parcelable.Creator<Site>() {
        public Site createFromParcel(Parcel in) {
            return new Site(in);
        }

        public Site[] newArray(int size) {
            return new Site[size];
        }
    };

//    public static Site fromPlannerLocation(Planner.Location loc) {
//        Site s = new Site();
//        s.setId(String.valueOf(loc.id));
//        s.setLocation(loc.latitude, loc.longitude);
//        s.setName(loc.name);
//        return s;
//    }

    public static Site fromJson(JSONObject json) throws JSONException {
        Site site = new Site();
        site.setSource(Site.SOURCE_STHLM_TRAVELING);
        site.setId(String.valueOf(json.getInt("site_id")));

        Pair<String, String> nameAndLocality =
                SitesStore.nameAsNameAndLocality(json.getString("name"));
        site.setName(nameAndLocality.first);
        site.setLocality(nameAndLocality.second);
        if (json.has("type")) {
            site.setType(json.getString("type"));
        }
        if (json.has("location") && !json.isNull("location")) {
            JSONObject locationJson = json.getJSONObject("location");
            try {
                Location location = new Location("sthlmtraveling");
                location.setLatitude(locationJson.getDouble("latitude"));
                location.setLongitude(locationJson.getDouble("longitude"));
                site.setLocation(location);
            } catch(Exception e) {
                Log.e("SITE", e.getMessage());
            }
        }
        return site;
    }

    public boolean isMyLocation() {
        return hasName() && mName.equals(TYPE_MY_LOCATION);
    }

    public boolean looksValid() {
        if (isMyLocation()) {
            return true;
        }
        if (hasLocation() && hasName() && mId == null) {
            return true;
        }
        if (hasName() && mId != null) {
            return true;
        }
        return false;
    }

    public static boolean looksValid(String name) {
        if (TextUtils.isEmpty(name) || TextUtils.getTrimmedLength(name) == 0) {
            return false;
        }
        return !name.matches(NAME_RE);
    }

    /**
     * Fill this Site with the values from another Site. If other is null this will be nullified.
     * @param value
     */
    public void fromSite(Site value) {
        if (value != null) {
            mId = value.mId;
            mLocation = value.mLocation;
            mName = value.mName;
            mType = value.mType;
            mLocality = value.mLocality;
        } else {
            mId = null;
            mLocation = null;
            mName = null;
            mType = null;
            mLocality = null;
        }
    }

    public String getNameOrId() {
        if (hasLocation() || mId == null) {
            return mName;
        }
        return String.valueOf(mId);
    }

    public boolean hasLocation() {
        return mLocation != null;
    }

    public void setLocation(double lat, double lng) {
        mLocation = new Location("sthlmtraveling");
        mLocation.setLatitude(lat);
        mLocation.setLongitude(lng);
    }

    public void setLocation(int lat, int lng) {
        if (lat == 0 || lng == 0) {
            return;
        }
        mLocation = new Location("sthlmtraveling");
        mLocation.setLatitude(lat / 1E6);
        mLocation.setLongitude(lng / 1E6);
    }

    public String toDump() {
        return "Site [mId=" + mId
                + ", mName=" + mName
                + ", mType=" + mType
                + ", mLocation=" + mLocation
                + ", mSource=" + mSource
                + ", mLocality=" + mLocality
                + "]";
    }

    public Place asPlace() {
        // If type is sthlm traveling and id is not 0.
        String id = mSource == SOURCE_STHLM_TRAVELING && !"0".equals(mId) ? mId : null;

        double lat = 0;
        double lon = 0;
        if (hasLocation()) {
            lat = mLocation.getLatitude();
            lon = mLocation.getLongitude();
        }
        return new Place(
                id,
                mName,
                isTransitStop() ? "stop" : "place",
                lat, lon, -1, null, null);
    }

    public static Site toSite(Place place) {
        Site site = new Site();
        site.setSource(SOURCE_STHLM_TRAVELING);
        site.setId(place.getId());
        site.setName(place.getName());
        site.setType("stop".equals(place.getType()) ? TYPE_TRANSIT_STOP : TYPE_ADDRESS);
        if (place.hasLocation()) {
            site.setLocation(place.getLat(), place.getLon());
        }
        return site;
    }
}
