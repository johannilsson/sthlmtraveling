package com.markupartist.sthlmtraveling.provider.site;

import android.os.Parcel;
import android.os.Parcelable;

public class Site implements Parcelable {
    private int mId;
    private String mName;

    public Site() {
    }

    public Site(Parcel parcel) {
        mId = parcel.readInt();
        mName = parcel.readString();
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
        mName = name;
    }

    /**
     * @see java.lang.Object#toString()
     */
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
        parcel.writeInt(mId);
        parcel.writeString(mName);
    }

    public static final Creator<Site> CREATOR = new Creator<Site>() {
        public Site createFromParcel(Parcel parcel) {
            return new Site(parcel);
        }

        public Site[] newArray(int size) {
            return new Site[size];
        }
    };
}
