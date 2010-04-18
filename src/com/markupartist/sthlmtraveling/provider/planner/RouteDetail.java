package com.markupartist.sthlmtraveling.provider.planner;

import java.util.ArrayList;

import com.markupartist.sthlmtraveling.provider.site.Site;

public class RouteDetail {
    private Site mSite;
    private String mDescription;
    private String mZone;

    public void setSite(Site site) {
        mSite = site;
    }

    public Site getSite() {
        return mSite;
    }
    
    public void setDescription(String description) {
        mDescription = description;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setZone(String zone) {
        mZone = zone;
    }

    public String getZone() {
        return mZone;
    }

    public static String getZones(ArrayList<RouteDetail> details) {
        String zoneString = "";
        for (RouteDetail detail : details) {
            if (detail.getZone() != null && !zoneString.contains(detail.getZone())) {
                zoneString += detail.getZone();
            }
        }
        return zoneString;
    }
}
