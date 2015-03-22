package com.markupartist.sthlmtraveling.provider.site;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.planner.Planner;

import org.json.JSONException;
import org.json.JSONObject;

public class Site implements Parcelable {
    public static String TYPE_MY_LOCATION = "MY_LOCATION";
    private static String NAME_RE = "[^\\p{Alnum}\\(\\)\\s]";

    private int mId;
    private String mName;
    private String mType;
    private Location mLocation;

    public Site() {
    }

    /**
     * Create a new Stop that is a copy of the given Stop.
     * @param site the site
     */
    public Site(Site site) {
        mName = site.getName();
        mLocation = site.getLocation();
        mId = site.getId();
        mType = site.getType();
    }

    private String getType() {
        return mType;
    }

    public Site(Parcel parcel) {
        mId = parcel.readInt();
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
    }

    /**
     * @return the id
     */
    public int getId() {
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
        if (mType == null) {
            return false;
        }
        return mType.equals("A");
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
        parcel.writeInt(mId);
        parcel.writeString(mName);
        parcel.writeString(mType);
        if (this.hasLocation()) {
            parcel.writeDouble(mLocation.getLatitude());
            parcel.writeDouble(mLocation.getLongitude());
        } else {
            parcel.writeDouble(0);
            parcel.writeDouble(0);
        }
    }

    public static final Parcelable.Creator<Site> CREATOR = new Parcelable.Creator<Site>() {
        public Site createFromParcel(Parcel in) {
            return new Site(in);
        }

        public Site[] newArray(int size) {
            return new Site[size];
        }
    };

    public static Site fromPlannerLocation(Planner.Location loc) {
        Site s = new Site();
        s.setId(loc.id);
        s.setLocation(loc.latitude, loc.longitude);
        s.setName(loc.name);
        return s;
    }

    public static Site fromJson(JSONObject json) throws JSONException {
        Site site = new Site();
        site.setId(json.getInt("site_id"));
        site.setName(json.getString("name"));
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
        if (hasLocation() && hasName() && mId == 0) {
            return true;
        }
        if (hasName() && mId > 0) {
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
        } else {
            mId = 0;
            mLocation = null;
            mName = null;
            mType = null;
        }
    }

    public String getNameOrId() {
        if (hasLocation() || mId == 0) {
            return mName;
        }
        return String.valueOf(mId);
    }

    public boolean hasLocation() {
        return mLocation != null;
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
        return "Site [mId=" + mId + ", mName=" + mName + ", mType=" + mType
                + ", mLocation=" + mLocation + "]";
    }

}
