/*
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling.provider.planner;

import android.os.Parcel;
import android.os.Parcelable;

import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.site.Site;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JourneyQuery implements Parcelable {
    public Site origin;
    public Site destination;
    public Site via;
    public Date time;
    public boolean isTimeDeparture = true;
    public boolean alternativeStops = true;
    public List<String> transportModes = new ArrayList<String>();
    public String ident;
    public String seqnr;
    public boolean hasPromotions;
    public int promotionNetwork = -1;

    public JourneyQuery() {
    }

    public JourneyQuery(Parcel parcel) {
        origin = parcel.readParcelable(Site.class.getClassLoader());
        destination = parcel.readParcelable(Site.class.getClassLoader());
        via = parcel.readParcelable(Site.class.getClassLoader());
        time = new Date(parcel.readLong());
        isTimeDeparture = (parcel.readInt() == 1);
        alternativeStops = (parcel.readInt() == 1);
        transportModes = new ArrayList<String>();
        parcel.readStringList(transportModes);
        ident = parcel.readString();
        seqnr = parcel.readString();
        hasPromotions = (parcel.readInt() == 1);
        promotionNetwork = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(origin, 0);
        dest.writeParcelable(destination, 0);
        dest.writeParcelable(via, 0);
        dest.writeLong(time.getTime());
        dest.writeInt(isTimeDeparture ? 1 : 0);
        dest.writeInt(alternativeStops ? 1 : 0);
        dest.writeStringList(transportModes);
        dest.writeString(ident);
        dest.writeString(seqnr);
        dest.writeInt(hasPromotions ? 1 : 0);
        dest.writeInt(promotionNetwork);
    }

    /**
     * Checks if the query contains has via.
     * 
     * @return Returns <code>true</code> if a via location is set,
     * <code>false</code> otherwise.
     */
    public boolean hasVia() {
        return via != null && via.hasName();
    }

    /**
     * Returns true if anything has than the defaults has been modified.
     *
     * @return true if any filtering is active.
     */
    public boolean hasAdditionalFiltering() {
        if (hasVia()) {
            return true;
        }
        if (alternativeStops) {
            return true;
        }
        if (transportModes != null) {
            List<String> defaults = Arrays.asList(TransportMode.METRO, TransportMode.BUS,
                    TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM);
            if (!transportModes.containsAll(defaults)) {
                return true;
            }
        }
        return false;
    }


    public static final Creator<JourneyQuery> CREATOR = new Creator<JourneyQuery>() {
        public JourneyQuery createFromParcel(Parcel parcel) {
            return new JourneyQuery(parcel);
        }

        public JourneyQuery[] newArray(int size) {
            return new JourneyQuery[size];
        }
    };

    public static JSONObject siteToJson(Site site) throws JSONException {
        JSONObject json = new JSONObject();

        json.put("id", site.getId());
        json.put("name", site.getName());
        if (site.isMyLocation()) {
            json.put("latitude", 0);
            json.put("longitude", 0);
        } else if (site.hasLocation()) {
            json.put("latitude", (int) (site.getLocation().getLatitude() * 1E6));
            json.put("longitude", (int) (site.getLocation().getLongitude() * 1E6));
        }
        json.put("source", site.getSource());
        json.put("locality", site.getLocality());

        return json;
    }

    public static Site jsonToSite(JSONObject json) throws JSONException {
        Site site = new Site();

        if (json.has("id")) {
            site.setId(json.getString("id"));
        }
        if (json.has("locality")) {
            site.setLocality(json.getString("locality"));
        }
        if (json.has("source")) {
            site.setSource(json.getInt("source"));
        }
        if (json.has("latitude") && json.has("longitude")) {
            site.setLocation(json.getInt("latitude"), json.getInt("longitude"));
        }

        site.setName(json.getString("name"));

        return site;
    }


    public JSONObject toJson(boolean all) throws JSONException {
        JSONObject jsonOrigin = siteToJson(origin);
        JSONObject jsonDestination = siteToJson(destination);

        JSONObject jsonQuery = new JSONObject();
        if (via != null) {
            JSONObject jsonVia = siteToJson(via);
            jsonQuery.put("via", jsonVia);
        }

        if (transportModes != null) {
            jsonQuery.put("transportModes", new JSONArray(transportModes));
        }

        jsonQuery.put("alternativeStops", alternativeStops);
        jsonQuery.put("origin", jsonOrigin);
        jsonQuery.put("destination", jsonDestination);

//        if (all) {
//            jsonQuery.put("ident", ident);
//            jsonQuery.put("seqnr", seqnr);
//            jsonQuery.put("time", time.format("%F %R"));
//            jsonQuery.put("isTimeDeparture", this.isTimeDeparture);
//        }

        return jsonQuery;
    }

    public static JourneyQuery fromJson(JSONObject jsonObject)
            throws JSONException {
        JourneyQuery journeyQuery = new JourneyQuery.Builder()
            .origin(jsonObject.getJSONObject("origin"))
            .destination(jsonObject.getJSONObject("destination"))
            .transportModes(jsonObject.has("transportModes")
                    ? jsonObject.getJSONArray("transportModes") : null)
            .create();

        if (jsonObject.has("isTimeDeparture")) {
            journeyQuery.isTimeDeparture =
                jsonObject.getBoolean("isTimeDeparture");
        }
        if (jsonObject.has("alternativeStops")) {
            journeyQuery.alternativeStops =
                jsonObject.getBoolean("alternativeStops");
        }
        if (jsonObject.has("via")) {
            JSONObject jsonVia = jsonObject.getJSONObject("via");
            journeyQuery.via = jsonToSite(jsonVia);
        }

        return journeyQuery;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JourneyQuery [alternativeStops=" + alternativeStops
                + ", destination=" + destination + ", ident=" + ident
                + ", isTimeDeparture=" + isTimeDeparture + ", origin=" + origin
                + ", seqnr=" + seqnr + ", time=" + time + ", transportModes="
                + transportModes + ", via=" + via + "]";
    }

    public static class Builder {
        private Site mOrigin;
        private Site mDestination;
        private Site mVia;
        private Date mTime;
        private boolean mIsTimeDeparture = true;
        private boolean mAlternativeStops = true;
        private List<String> mTransportModes;

        public Builder() {
            
        }

        public Builder origin(Site origin) {
            mOrigin = origin; //buildLocationFromStop(origin);
            return this;
        }

        public Builder origin(String name, int latitude, int longitude) {
            mOrigin = new Site();
            mOrigin.setName(name);
            mOrigin.setLocation(latitude, longitude);
            return this;
        }

        public Builder origin(JSONObject jsonObject) throws JSONException {
            mOrigin = jsonToSite(jsonObject);
//            mOrigin = new Location();
//            mOrigin.id = jsonObject.getInt("id");
//            mOrigin.name = jsonObject.getString("name");
//            mOrigin.latitude = jsonObject.getInt("latitude");
//            mOrigin.longitude = jsonObject.getInt("longitude");
            return this;
        }
        
        public Builder destination(Site destination) {
            mDestination = destination; //buildLocationFromStop(destination);
            return this;
        }

        public Builder destination(JSONObject jsonObject) throws JSONException {
//            mDestination = new Location();
//            mDestination.id = jsonObject.getInt("id");
//            mDestination.name = jsonObject.getString("name");
//            mDestination.latitude = jsonObject.getInt("latitude");
//            mDestination.longitude = jsonObject.getInt("longitude");
            mDestination = jsonToSite(jsonObject);
            return this;
        }
        
        public Builder destination(String name, int latitude, int longitude) {
            mDestination = new Site();
            mDestination.setName(name);
            mDestination.setLocation(latitude, longitude);
            return this;
        }

        public Builder via(Site via) {
            if (via != null && via.hasName()) {
                mVia = via; //buildLocationFromStop(via);
            }
            return this;
        }

        public Builder via(JSONObject jsonObject) throws JSONException {
//            mVia = new Location();
//            mVia.id = jsonObject.getInt("id");
//            mVia.name = jsonObject.getString("name");
            mVia = jsonToSite(jsonObject);
            return this;
        }

        public Builder time(Date time) {
            mTime = time;
            return this;
        }

        public Builder isTimeDeparture(boolean isTimeDeparture) {
            mIsTimeDeparture = isTimeDeparture;
            return this;
        }

        public Builder alternativeStops(boolean alternativeStops) {
            mAlternativeStops = alternativeStops;
            return this;
        }

        public Builder transportModes(ArrayList<String> transportModes) {
            mTransportModes = transportModes;
            return this;
        }

        public Builder transportModes(JSONArray jsonArray) throws JSONException {
            if (jsonArray == null) {
                return this;
            }
            mTransportModes = new ArrayList<String>();
            for (int i = 0; i < jsonArray.length(); i++) {
                mTransportModes.add(jsonArray.getString(i));
            }
            return this;
        }

        public JourneyQuery create() {
            JourneyQuery journeyQuery = new JourneyQuery();
            journeyQuery.origin = mOrigin;
            journeyQuery.destination = mDestination;
            journeyQuery.via = mVia;

            if (mTime == null) {
                mTime = new Date();
            }
            journeyQuery.time = mTime;
            journeyQuery.isTimeDeparture = mIsTimeDeparture;
            journeyQuery.alternativeStops = mAlternativeStops;
            if (mTransportModes == null) {
                mTransportModes = Arrays.asList(TransportMode.METRO, TransportMode.BUS,
                        TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM);
            }
            journeyQuery.transportModes = mTransportModes;

            return journeyQuery;
        }

//        public static Planner.Location buildLocationFromStop(Site site) {
//            Planner.Location location = new Planner.Location();
//            if (site.getSource() == Site.SOURCE_STHLM_TRAVELING && site.getId() != null) {
//                location.id = Integer.parseInt(site.getId());
//            }
//            location.name = site.getName();
//            if (site.getLocation() != null) {
//                location.latitude =
//                    (int) (site.getLocation().getLatitude() * 1E6);
//                location.longitude =
//                    (int) (site.getLocation().getLongitude() * 1E6);
//            }
//            return location;
//        }
    }

}
