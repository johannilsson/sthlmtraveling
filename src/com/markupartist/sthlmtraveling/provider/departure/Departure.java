package com.markupartist.sthlmtraveling.provider.departure;

import android.text.format.Time;

public class Departure {
    private String mTransport;
    private String mLineNumber;
    private String mDestination;
    private Time mTimeTabledDateTime;
    private Time mExpectedDateTime;
    private String mDisplayTime;
    private int mJourneyDirection;
    private String mGroupOfLine; 

    public boolean matches(DepartureFilter filter) {
        return filter.matches(this);
    }

    /**
     * @return the transport
     */
    public String getTransport() {
        return mTransport;
    }

    /**
     * @param transport the transport to set
     */
    public void setTransport(String transport) {
        mTransport = transport;
    }

    /**
     * @return the lineNumber
     */
    public String getLineNumber() {
        return mLineNumber;
    }

    /**
     * @param lineNumber the lineNumber to set
     */
    public void setLineNumber(String lineNumber) {
        mLineNumber = lineNumber;
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return mDestination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(String destination) {
        mDestination = destination;
    }

    /**
     * @return the timeTabledDateTime
     */
    public Time getTimeTabledDateTime() {
        return mTimeTabledDateTime;
    }

    /**
     * @param timeTabledDateTime the timeTabledDateTime to set
     */
    public void setTimeTabledDateTime(Time timeTabledDateTime) {
        this.mTimeTabledDateTime = timeTabledDateTime;
    }

    /**
     * @return the expectedDateTime
     */
    public Time getExpectedDateTime() {
        return mExpectedDateTime;
    }

    /**
     * @param expectedDateTime the expectedDateTime to set
     */
    public void setExpectedDateTime(Time expectedDateTime) {
        this.mExpectedDateTime = expectedDateTime;
    }

    /**
     * @return the displayTime
     */
    public String getDisplayTime() {
        return mDisplayTime;
    }

    /**
     * @param displayTime the displayTime to set
     */
    public void setDisplayTime(String displayTime) {
        this.mDisplayTime = displayTime;
    }

    /**
     * @param groupOfLine the groupOfLine to set
     */
    public void setGroupOfLine(String groupOfLine) {
        mGroupOfLine = groupOfLine;
    }

    /**
     * @return the groupOfLine
     */
    public String getGroupOfLine() {
        return mGroupOfLine;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Departure [mDestination=" + mDestination + ", mDisplayTime="
                + mDisplayTime + ", mExpectedDateTime=" + mExpectedDateTime
                + ", mGroupOfLine=" + mGroupOfLine + ", mJourneyDirection="
                + mJourneyDirection + ", mLineNumber=" + mLineNumber
                + ", mTimeTabledDateTime=" + mTimeTabledDateTime
                + ", mTransport=" + mTransport + "]";
    }

}
