package com.markupartist.sthlmtraveling.provider.planner;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Stop implements Parcelable {
    public static String TYPE_MY_LOCATION = "MY_LOCATION";
    private static String NAME_RE = "[^\\p{Alnum}\\(\\)\\s]";
    private String mName;
    private Location mLocation;
    private int mSiteId;

    public Stop() {
    }

    public Stop(String name) {
        setName(name);
    }

    public Stop(String name, Location location) {
        setName(name);
        mLocation = location;
    }

    public Stop(String name, double latitude, double longitude) {
        setName(name);
        mLocation = new Location("sthlmtraveling");
        mLocation.setLatitude(latitude);
        mLocation.setLongitude(longitude);
    }

    public Stop(Parcel parcel) {
        mName = parcel.readString();
        mLocation = parcel.readParcelable(null);
        mSiteId = parcel.readInt();
    }

    /**
     * Create a new Stop that is a copy of the given Stop.
     * @param stop the stop
     */
    public Stop(Stop stop) {
        mName = stop.getName();
        mLocation = stop.getLocation();
        mSiteId = stop.getSiteId();
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        if (!TextUtils.isEmpty(name)) {
            if (name.equals(TYPE_MY_LOCATION)) {
                mName = name;
            } else {
                mName = name.trim().replaceAll(NAME_RE, "");
            }
        }
    }

    public boolean hasName() {
        return !TextUtils.isEmpty(mName);
    }
    
    public void setLocation(Location location) {
        mLocation = location;
    }

    public void setLocation(int latitudeE6, int longitudeE6) {
        mLocation = new Location("sthlmtraveling");
        mLocation.setLatitude(latitudeE6 / 1E6);
        mLocation.setLongitude(longitudeE6 / 1E6);
    }

    public Location getLocation() {
        return mLocation;
    }

    public boolean isMyLocation() {
        return hasName() && mName.equals(TYPE_MY_LOCATION);
    }

    public void setSiteId(int siteId) {
        mSiteId = siteId;
    }

    public int getSiteId() {
        return mSiteId;
    }

    public boolean looksValid() {
        return hasName();
    }

    public static boolean looksValid(String name) {
        if (TextUtils.isEmpty(name) || TextUtils.getTrimmedLength(name) == 0) {
            return false;
        }
        return !name.matches(NAME_RE);
    }

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeParcelable(mLocation, 0);
        parcel.writeInt(mSiteId);
    }

    public static final Creator<Stop> CREATOR = new Creator<Stop>() {
        public Stop createFromParcel(Parcel parcel) {
            return new Stop(parcel);
        }

        public Stop[] newArray(int size) {
            return new Stop[size];
        }
    };
}
