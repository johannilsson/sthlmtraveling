package com.markupartist.sthlmtraveling.provider.site;

import android.location.Location;

public class StopPoint {
    public Site site;
    public int distance;
    public String stopAreaType;
    public String name;
    public Location location;

    public String toString() {
        return String.format("%s - %sm", name, distance);
    }
}
