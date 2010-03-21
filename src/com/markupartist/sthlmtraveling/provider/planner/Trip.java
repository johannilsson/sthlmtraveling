package com.markupartist.sthlmtraveling.provider.planner;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Time;

import com.markupartist.sthlmtraveling.provider.site.Site;

public class Trip implements Parcelable {
    private Stop mStartPoint;
    private Stop mEndPoint;
    private Time mTime;
    private boolean mIsTimeDeparture;
    private ArrayList<Site> mStartPointAlternatives;
    private ArrayList<Site> mEndPointAlternatives;
    private ArrayList<Route> mRoutes;

    public Trip(Stop startPoint, Stop endPoint, Time time, boolean isTimeDeparture) {
        mStartPoint = startPoint;
        mEndPoint = endPoint;
        mTime = time;
        mIsTimeDeparture = isTimeDeparture;
    }

    public Trip(Parcel parcel) {
        //mStartPoint = parcel.readParcelable(null);
        //mEndPoint = parcel.readParcelable(null);
        mTime = new Time();
        mTime.parse(parcel.readString());
        mIsTimeDeparture = parcel.readInt() == 1 ? true : false; 
        mStartPointAlternatives = new ArrayList<Site>();
        parcel.readTypedList(mStartPointAlternatives, Site.CREATOR);
        mEndPointAlternatives = new ArrayList<Site>();
        parcel.readTypedList(mEndPointAlternatives, Site.CREATOR);
        mRoutes = new ArrayList<Route>();
        parcel.readTypedList(mRoutes, Route.CREATOR);
    }

    public boolean hasAlternatives() {
        boolean hasAlternatives = false;
        if (getStartPointAlternatives().size() > 1 || getEndPointAlternatives().size() > 1) {
            hasAlternatives = true;
        }
        return hasAlternatives;
    }

    /**
     * @return the startPoint
     */
    public Stop getStartPoint() {
        return mStartPoint;
    }

    /**
     * @param startPoint the startPoint to set
     */
    public void setStartPoint(Stop startPoint) {
        this.mStartPoint = startPoint;
    }

    /**
     * @return the endPoint
     */
    public Stop getEndPoint() {
        return mEndPoint;
    }

    /**
     * @param endPoint the endPoint to set
     */
    public void setEndPoint(Stop endPoint) {
        this.mEndPoint = endPoint;
    }

    /**
     * @return the time
     */
    public Time getTime() {
        return mTime;
    }

    /**
     * @param time the time to set
     */
    public void setTime(Time time) {
        this.mTime = time;
    }

    /**
     * @return true if departure false otherwise.
     */
    public boolean isTimeDeparture() {
        return mIsTimeDeparture;
    }

    /**
     * @param isTimeDeparture the isTimeDeparture to set
     */
    public void setIsTimeDeparture(boolean isTimeDeparture) {
        this.mIsTimeDeparture = isTimeDeparture;
    }

    /**
     * @return the startPointAlternatives
     */
    public ArrayList<Site> getStartPointAlternatives() {
        if (mStartPointAlternatives == null) {
            mStartPointAlternatives = new ArrayList<Site>();
        }
        return mStartPointAlternatives;
    }

    /**
     * @param startPointAlternatives the startPointAlternatives to set
     */
    public void setStartPointAlternatives(ArrayList<Site> startPointAlternatives) {
        this.mStartPointAlternatives = startPointAlternatives;
    }

    /**
     * @return the endPointAlternatives
     */
    public ArrayList<Site> getEndPointAlternatives() {
        if (mEndPointAlternatives == null) {
            mEndPointAlternatives = new ArrayList<Site>();
        }
        return mEndPointAlternatives;
    }

    /**
     * @param endPointAlternatives the endPointAlternatives to set
     */
    public void setEndPointAlternatives(ArrayList<Site> endPointAlternatives) {
        this.mEndPointAlternatives = endPointAlternatives;
    }

    /**
     * @return the routes
     */
    public ArrayList<Route> getRoutes() {
        return mRoutes;
    }

    /**
     * @param routes the routes to set
     */
    public void setRoutes(ArrayList<Route> routes) {
        this.mRoutes = routes;
    }

    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Trip [mEndPoint=" + mEndPoint + ", mEndPointAlternatives="
                + mEndPointAlternatives + ", mIsTimeDeparture="
                + mIsTimeDeparture + ", mRoutes=" + mRoutes + ", mStartPoint="
                + mStartPoint + ", mStartPointAlternatives="
                + mStartPointAlternatives + ", mTime=" + mTime + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //dest.writeParcelable(mStartPoint, 0);
        //dest.writeParcelable(mEndPoint, 0);
        dest.writeString(mTime.format2445());
        dest.writeInt(mIsTimeDeparture ? 1 : 0);
        dest.writeTypedList(mStartPointAlternatives);
        dest.writeTypedList(mEndPointAlternatives);
        dest.writeTypedList(mRoutes);
    }

    public static final Creator<Trip> CREATOR = new Creator<Trip>() {
        public Trip createFromParcel(Parcel parcel) {
            return new Trip(parcel);
        }

        public Trip[] newArray(int size) {
            return new Trip[size];
        }
    };
}
