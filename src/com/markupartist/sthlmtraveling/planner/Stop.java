package com.markupartist.sthlmtraveling.planner;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class Stop implements Parcelable {
    public static String TYPE_MY_LOCATION = "MY_LOCATION";
    private String mName;
    private Location mLocation;

    public Stop() {
    }

    public Stop(String name) {
        mName = name;
    }

    public Stop(String name, Location location) {
        mName = name;
        mLocation = location;
    }

    public Stop(String name, double latitude, double longitude) {
        mName = name;
        mLocation = new Location("sthlmtraveling");
        mLocation.setLatitude(latitude);
        mLocation.setLongitude(longitude);
    }

    public Stop(Parcel parcel) {
        mName = parcel.readString();
        mLocation = parcel.readParcelable(null);
    }

    /**
     * Create a new Stop that is a copy of the given Stop.
     * @param stop the stop
     */
    public Stop(Stop stop) {
        mName = stop.getName();
        mLocation = stop.getLocation();
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
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

    @Override
    public String toString() {
        return "Stop [mLocation=" + mLocation + ", mName=" + mName + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeParcelable(mLocation, 0);
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
