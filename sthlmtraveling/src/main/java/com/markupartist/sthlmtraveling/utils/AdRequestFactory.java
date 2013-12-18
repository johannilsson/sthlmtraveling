package com.markupartist.sthlmtraveling.utils;

import android.location.Location;

import com.google.ads.AdRequest;

public class AdRequestFactory {
    public static AdRequest createRequest() {
        AdRequest adRequest = new AdRequest();
        //adRequest.addTestDevice("1B4BB612FC60F16E02C390CFC5BBA502");
        return adRequest;
    }

    public static AdRequest createRequest(Location location) {
        AdRequest adRequest = createRequest();
        adRequest.setLocation(location);
        return adRequest;
    }
}
