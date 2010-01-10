package com.markupartist.sthlmtraveling.provider.departure;

public class DepartureFilter {

    private String mTransport;
    private String mLineNumber;

    public DepartureFilter(String transport, String lineNumber) {
        mTransport = transport;
        mLineNumber = lineNumber;
    }

    public boolean matches(Departure departure) {
        if (mTransport.equals(departure.getTransport())
                && mLineNumber.equals(departure.getLineNumber())) {
            return true;
        }
        return false;
    }
}
