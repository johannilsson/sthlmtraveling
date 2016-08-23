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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.markupartist.sthlmtraveling.data.api.PlaceQuery;
import com.markupartist.sthlmtraveling.data.api.TravelModeQuery;
import com.markupartist.sthlmtraveling.data.models.TravelMode;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.LegUtil;

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
    public boolean alternativeStops = false;
    public List<String> transportModes = new ArrayList<>();
    public String ident;
    public boolean hasPromotions;
    public int promotionNetwork = -1;
    // Storing the state of the current ident and scroll dir to allow refresh of paginated
    // results
    @Nullable  public String previousIdent;
    @Nullable  public String previousDir;

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
        hasPromotions = (parcel.readInt() == 1);
        promotionNetwork = parcel.readInt();
        previousIdent = parcel.readString();
        previousDir = parcel.readString();
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
        dest.writeInt(hasPromotions ? 1 : 0);
        dest.writeInt(promotionNetwork);
        dest.writeString(previousIdent);
        dest.writeString(previousDir);
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
            return !hasDefaultTransportModes();
        }
        return false;
    }

    public boolean hasDefaultTransportModes() {
        if (transportModes != null) {
            List<String> defaults = Arrays.asList(TransportMode.METRO, TransportMode.BUS,
                    TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM);
            if (transportModes.containsAll(defaults)) {
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

    public Uri toUri(boolean withTime) {
        if (origin == null || destination == null) {
            return null;
        }

        Uri routesUri;

        PlaceQuery fromQuery = new PlaceQuery.Builder().place(origin.asPlace()).build();
        PlaceQuery toQuery = new PlaceQuery.Builder().place(destination.asPlace()).build();

        routesUri = Uri.parse("journeyplanner://routes?");

        routesUri = routesUri.buildUpon()
                .appendQueryParameter("version", "2")
                .appendQueryParameter("from", Uri.encode(fromQuery.toString()))
                .appendQueryParameter("fromSource", String.valueOf(origin.getSource()))
                .appendQueryParameter("to", Uri.encode(toQuery.toString()))
                .appendQueryParameter("toSource", String.valueOf(destination.getSource()))
                .appendQueryParameter("alternative", String.valueOf(alternativeStops))
                .build();

        if (withTime) {
            if (time != null) {
                String timeString = DateTimeUtil.format2445(time);
                routesUri = routesUri.buildUpon()
                        .appendQueryParameter("time", timeString)
                        .appendQueryParameter("isTimeDeparture", String.valueOf(isTimeDeparture))
                        .build();
            }
        }

        if (hasVia()) {
            routesUri = routesUri.buildUpon()
                    .appendQueryParameter("via", Uri.encode(
                            new PlaceQuery.Builder().place(via.asPlace()).build().toString()))
                    .build();
        }

        if (transportModes != null && !transportModes.isEmpty()) {
            // Convert transport modes to travel modes.
            List<TravelMode> travelModes = LegUtil.transportModesToTravelModes(transportModes);
            TravelModeQuery travelModeQuery = new TravelModeQuery(travelModes);
            routesUri = routesUri.buildUpon()
                    .appendQueryParameter("travelMode", travelModeQuery.toString())
                    .build();
        }

        return routesUri;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "JourneyQuery [alternativeStops=" + alternativeStops
                + ", destination=" + destination + ", ident=" + ident
                + ", isTimeDeparture=" + isTimeDeparture + ", origin=" + origin
                + ", time=" + time + ", transportModes="
                + transportModes + ", via=" + via + "]";
    }

    public static class Builder {
        private Site mOrigin;
        private Site mDestination;
        private Site mVia;
        private Date mTime;
        private boolean mIsTimeDeparture = true;
        private boolean mAlternativeStops = false;
        private List<String> mTransportModes;

        public Builder() {
            
        }

        public Builder origin(Site origin) {
            mOrigin = origin;
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
            return this;
        }
        
        public Builder destination(Site destination) {
            mDestination = destination;
            return this;
        }

        public Builder destination(JSONObject jsonObject) throws JSONException {
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
                mVia = via;
            }
            return this;
        }

        public Builder via(JSONObject jsonObject) throws JSONException {
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

        public Builder transportModes(List<String> transportModes) {
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

        public Builder uri(Uri uri) {
            String version = uri.getQueryParameter("version");
            if (version == null) {
                return uriV1(uri);
            }
            return uriV2(uri);
        }

        public Builder uriV2(Uri uri) {
            Site origin = fromQueryParameter(Uri.decode(uri.getQueryParameter("from")));
            origin.setSource(Integer.parseInt(uri.getQueryParameter("fromSource")));
            origin(origin);

            Site destination = fromQueryParameter(Uri.decode(uri.getQueryParameter("to")));
            destination.setSource(Integer.parseInt(uri.getQueryParameter("fromSource")));
            destination(destination);

            via(fromQueryParameter(Uri.decode(uri.getQueryParameter("via"))));
            alternativeStops(Boolean.parseBoolean(uri.getQueryParameter("alternative")));

            String timeStr = uri.getQueryParameter("time");
            if (timeStr != null) {
                time(DateTimeUtil.parse2445(timeStr));
                isTimeDeparture(Boolean.parseBoolean(uri.getQueryParameter("isTimeDeparture")));
            } else {
                isTimeDeparture(true);
            }

            TravelModeQuery travelModeQuery = TravelModeQuery.fromStringList(
                    Uri.decode(uri.getQueryParameter("travelMode")));
            transportModes(LegUtil.travelModesToTransportModes(travelModeQuery.getModes()));

            return this;
        }

        Site fromQueryParameter(String parameter) {
            if (parameter == null) {
                return null;
            }
            Site site = new Site();
            PlaceQuery fromQuery = new PlaceQuery.Builder().param(parameter).build();
            site.setName(fromQuery.getName());
            if (fromQuery.getId() == null) {
                if (fromQuery.getLon() != 0 || fromQuery.getLat() != 0) {
                    site.setLocation(fromQuery.getLat(), fromQuery.getLon());
                }
            } else {
                site.setId(fromQuery.getId());
            }
            fromQuery = null;
            return site;
        }

        public Builder uriV1(Uri uri) {
            Site origin = new Site();
            origin.setName(uri.getQueryParameter("start_point"));
            String originId = uri.getQueryParameter("start_point_id");
            if (!TextUtils.isEmpty(originId) && !"null".equals(originId)) {
                origin.setId(originId);
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_lat"))
                    && !TextUtils.isEmpty(uri.getQueryParameter("start_point_lng"))) {
                origin.setLocation(
                        (int) (Double.parseDouble(uri.getQueryParameter("start_point_lat")) * 1E6),
                        (int) (Double.parseDouble(uri.getQueryParameter("start_point_lng")) * 1E6));
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("start_point_source"))) {
                origin.setSource(Integer.parseInt(uri.getQueryParameter("start_point_source")));
            }
            this.origin(origin);

            Site destination = new Site();
            destination.setName(uri.getQueryParameter("end_point"));
            String destinationId = uri.getQueryParameter("end_point_id");
            if (!TextUtils.isEmpty(destinationId) && !"null".equals(destinationId)) {
                destination.setId(destinationId);
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_lat"))
                    && !TextUtils.isEmpty(uri.getQueryParameter("end_point_lng"))) {
                destination.setLocation(
                        (int) (Double.parseDouble(uri.getQueryParameter("end_point_lat")) * 1E6),
                        (int) (Double.parseDouble(uri.getQueryParameter("end_point_lng")) * 1E6));
            }
            if (!TextUtils.isEmpty(uri.getQueryParameter("end_point_source"))) {
                destination.setSource(Integer.parseInt(uri.getQueryParameter("end_point_source")));
            }
            this.destination(destination);

            boolean isTimeDeparture = true;
            if (!TextUtils.isEmpty(uri.getQueryParameter("isTimeDeparture"))) {
                isTimeDeparture = Boolean.parseBoolean(
                        uri.getQueryParameter("isTimeDeparture"));
            }
            this.isTimeDeparture(isTimeDeparture);


            Date time = new Date();
            String timeString = uri.getQueryParameter("time");
            if (!TextUtils.isEmpty(timeString)) {
                // TODO: What is the format here?
                //jq.time.parse(timeString);
            } else {
                time.setTime(System.currentTimeMillis());
            }
            this.time(time);

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
            if (mTransportModes == null || mTransportModes.isEmpty()) {
                mTransportModes = Arrays.asList(TransportMode.METRO, TransportMode.BUS,
                        TransportMode.WAX, TransportMode.TRAIN, TransportMode.TRAM);
            }
            journeyQuery.transportModes = mTransportModes;

            return journeyQuery;
        }

    }

}
