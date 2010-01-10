package com.markupartist.sthlmtraveling.provider.planner;

import android.text.format.Time;

public class RouteCriteria {
    private Stop mStartPoint;
    private Stop mEndPoint;
    private Time mTime;

    public RouteCriteria(Stop startPoint, Stop endPoint, Time time) {
        mStartPoint = startPoint;
        mEndPoint = endPoint;
        mTime = time;
    }

    public Stop getStartPoint() {
        return mStartPoint;
    }

    public Stop getEndPoint() {
        return mEndPoint;
    }

    public Time getTime() {
        return mTime;
    }
}
