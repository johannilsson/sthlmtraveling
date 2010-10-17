/*
 * Copyright (C) 2009-2010 Johan Nilsson <http://markupartist.com>
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

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;
import com.markupartist.sthlmtraveling.utils.HttpManager;
import com.markupartist.sthlmtraveling.utils.StreamUtils;

/**
 * Journey planner for the sl.se API.
 */
public class Planner {
    private static final String TAG = "Planner";
    private static Planner instance = null;

    /**
     * Constructs a new Planner
     */
    private Planner() {
    }

    /**
     * Find stops that matches the provided name
     * @param name the name
     * @return a list of stops
     * @throws IOException on network problems
     */
    @Deprecated
    public ArrayList<String> findStop(String name) throws IOException{
        ArrayList<Site> sites = SitesStore.getInstance().getSite(name);
        ArrayList<String> stops = new ArrayList<String>();
        for (Site site : sites) {
            stops.add(site.getName());
        }
        return stops;
    }

    private JSONObject createQuery(JourneyQuery query) throws JSONException {
        JSONObject jsonQuery = new JSONObject();
        JSONObject origin = new JSONObject();
        origin.put("id", query.origin.id);
        origin.put("name", query.origin.name);
        origin.put("latitude", query.origin.latitude);
        origin.put("longitude", query.origin.longitude);

        JSONObject destination = new JSONObject();
        destination.put("id", query.destination.id);
        destination.put("name", query.destination.name);
        destination.put("latitude", query.destination.latitude);
        destination.put("longitude", query.destination.longitude);

        jsonQuery.put("origin", origin);
        jsonQuery.put("destination", destination);
        jsonQuery.put("ident", query.ident);
        jsonQuery.put("seqnr", query.seqnr);
        jsonQuery.put("time", query.time.format("%F %R"));
        jsonQuery.put("isTimeDeparture", query.isTimeDeparture);

        return jsonQuery;
    }

    public Response findPreviousJourney(JourneyQuery query) throws IOException, BadResponse {
        try {
            JSONObject json = createQuery(query);
            json.put("isPreviousQuery", true);
            return doQuery(json);
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Response findNextJourney(JourneyQuery query) throws IOException, BadResponse {
        try {
            JSONObject json = createQuery(query);
            json.put("isNextQuery", true);
            return doQuery(json);
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    public Response findJourney(JourneyQuery query) throws IOException, BadResponse {
        try {
            return doQuery(createQuery(query));
        } catch (JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    private Response doQuery(JSONObject jsonQuery) throws IOException, BadResponse {
        final HttpPost post = new HttpPost(apiEndpoint()
                + "/journeyplanner/?key=" + get(KEY));

        post.setEntity(new StringEntity(jsonQuery.toString()));

        final HttpResponse response = HttpManager.execute(post);

        HttpEntity entity;
        Response r = null;
        String rawContent;
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
        case HttpStatus.SC_OK:
            entity = response.getEntity();
            rawContent = StreamUtils.toString(entity.getContent());
            try {
                r = Response.fromJson(new JSONObject(rawContent));
            } catch (JSONException e) {
                Log.d(TAG, "Could not parse the reponse...");
                throw new IOException("Could not parse the response.");
            }
            break;
        case HttpStatus.SC_BAD_REQUEST:
            entity = response.getEntity();
            rawContent = StreamUtils.toString(entity.getContent());
            BadResponse br;
            try {
                br = BadResponse.fromJson(new JSONObject(rawContent));
            } catch (JSONException e) {
                Log.d(TAG, "Could not parse the reponse...");
                throw new IOException("Could not parse the response.");
            }
            throw br;
        default:
            Log.d(TAG, "Status code not OK from API, was " + statusCode);
            throw new IOException("A remote server error occurred when getting deviations.");                
        }

        return r;
    }

    /**
     * Get an instance of Planner.
     * @return a Planner
     */
    public static Planner getInstance() {
        if (instance == null)
            instance = new Planner();
        return instance;
    }

    public static class BadResponse extends Exception {
        public String errorCode;
        public String description;

        public static BadResponse fromJson(JSONObject json) throws JSONException {
            BadResponse br = new BadResponse();
            if (json.has("errorCode")) {
                br.errorCode = json.getString("errorCode");
            } else {
                br.errorCode = "-1";
            }
            if (json.has("description")) {
                br.description = json.getString("description");
            } else {
                br.description = "Unknown error";
            }
            return br;
        }
    }

    public static class Response implements Parcelable {
        // TODO: Parse out the ident.
        public String ident;
        public String seqnr;
        public int numberOfTrips;
        public ArrayList<Trip2> trips = new ArrayList<Trip2>();

        public Response() {}

        public Response(Parcel parcel) {
            ident = parcel.readString();
            seqnr = parcel.readString();
            trips = new ArrayList<Trip2>();
            parcel.readTypedList(trips, Trip2.CREATOR);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(ident);
            dest.writeString(seqnr);
            dest.writeTypedList(trips);
        }

        @Override
        public String toString() {
            return "Response{" +
                    "ident='" + ident + '\'' +
                    ", seqnr='" + seqnr + '\'' +
                    ", numberOfTrips=" + numberOfTrips +
                    ", trips=" + trips +
                    '}';
        }

        public static Response fromJson(JSONObject json) throws JSONException {
            Response r = new Response();

            if (json.has("ident")) {
                r.ident = json.getString("ident");
            }
            if (json.has("seqnr")) {
                r.seqnr = json.getString("seqnr");
            }

            JSONArray jsonTrips = json.getJSONArray("trips");
            for (int i = 0; i < jsonTrips.length(); i++) {
                try {
                    r.trips.add(Trip2.fromJson(jsonTrips.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Failed to parse trip: " + e.getMessage());
                }
            }

            return r;
        }

        public static final Creator<Response> CREATOR = new Creator<Response>() {
            public Response createFromParcel(Parcel parcel) {
                return new Response(parcel);
            }

            public Response[] newArray(int size) {
                return new Response[size];
            }
        };
    }

    public static class Trip2 implements Parcelable {
        public Location origin;
        public Location destination;
        public String departureDate; // TODO: Combine date and time
        public String departureTime;
        public String arrivalDate; // TODO: Combine date and time
        public String arrivalTime;
        public int changes;
        public String duration;
        public String tariffZones;
        public String tariffRemark;
        public String co2;
        public boolean mt6MessageExist;
        public boolean rtuMessageExist;
        public boolean remarksMessageExist;
        public ArrayList<SubTrip> subTrips = new ArrayList<SubTrip>();

        public Trip2() {}

        public Trip2(Parcel parcel) {
            origin = parcel.readParcelable(Location.class.getClassLoader());
            destination = parcel.readParcelable(Location.class.getClassLoader());
            departureDate = parcel.readString();
            departureTime = parcel.readString();
            arrivalDate = parcel.readString();
            arrivalTime = parcel.readString();
            changes = parcel.readInt();
            duration = parcel.readString();
            tariffZones = parcel.readString();
            tariffRemark = parcel.readString();
            co2 = parcel.readString();
            mt6MessageExist = (parcel.readInt() == 1) ? true : false;
            rtuMessageExist = (parcel.readInt() == 1) ? true : false;
            remarksMessageExist = (parcel.readInt() == 1) ? true : false;
            subTrips = new ArrayList<SubTrip>();
            parcel.readTypedList(subTrips, SubTrip.CREATOR);
        }

        /**
         * @return returns true if this trip can be purchased with SMS.
         */
        public boolean canBuySmsTicket() {
            return "2".equals(tariffRemark) ||
                   "3".equals(tariffRemark) ||
                   "4".equals(tariffRemark);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(origin, 0);
            dest.writeParcelable(destination, 0);
            dest.writeString(departureDate);
            dest.writeString(departureTime);
            dest.writeString(arrivalDate);
            dest.writeString(arrivalTime);
            dest.writeInt(changes);
            dest.writeString(duration);
            dest.writeString(tariffZones);
            dest.writeString(tariffRemark);
            dest.writeString(co2);
            dest.writeInt((mt6MessageExist == true) ? 1 : 0);
            dest.writeInt((rtuMessageExist == true) ? 1 : 0);
            dest.writeInt((remarksMessageExist == true) ? 1 : 0);
            dest.writeTypedList(subTrips);
        }

        public static Trip2 fromJson(JSONObject json) throws JSONException {
            //parseLocation(json.getJSONObject("origin"));

            Trip2 trip = new Trip2();
            trip.departureDate = json.getString("departureDate");

            JSONArray jsonSubTrips = json.getJSONArray("subTrips");
            for (int i = 0; i < jsonSubTrips.length(); i++) {
                try {
                    trip.subTrips.add(SubTrip.fromJson(jsonSubTrips.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Failed to parse sub trip: " + e.getMessage());
                }
            }

            trip.arrivalDate = json.getString("arrivalDate");
            trip.arrivalTime = json.getString("arrivalTime");
            trip.changes = json.getInt("changes");
            trip.co2 = json.getString("co2");
            trip.departureDate = json.getString("departureDate");
            trip.departureTime = json.getString("departureTime");
            trip.destination = Location.fromJson(json.getJSONObject("destination"));
            trip.duration = json.getString("duration");
            trip.mt6MessageExist = json.getBoolean("mt6MessageExist");
            trip.origin = Location.fromJson(json.getJSONObject("origin"));
            if (json.has("tariffZones")) {
                trip.tariffZones = json.getString("tariffZones");
            }
            if (json.has("tariffRemark")) {
                trip.tariffRemark = json.getString("tariffRemark");
            }
            trip.remarksMessageExist = json.getBoolean("remarksMessageExist");
            trip.rtuMessageExist = json.getBoolean("rtuMessageExist");

            return trip;
        }
        
        @Override
        public String toString() {
            return "Trip{" +
                    "origin=" + origin +
                    ", destination=" + destination +
                    ", departureDate='" + departureDate + '\'' +
                    ", departureTime='" + departureTime + '\'' +
                    ", arrivalDate='" + arrivalDate + '\'' +
                    ", arrivalTime='" + arrivalTime + '\'' +
                    ", changes=" + changes +
                    ", duration='" + duration + '\'' +
                    ", tariffZones='" + tariffZones + '\'' +
                    ", tariffRemark='" + tariffRemark + '\'' +
                    ", co2='" + co2 + '\'' +
                    ", mt6MessageExist=" + mt6MessageExist +
                    ", rtuMessageExist=" + rtuMessageExist +
                    ", remarksMessageExist=" + remarksMessageExist +
                    ", subTrips=" + subTrips +
                    '}';
        }

        public String toText() {
            String durationInMinutes = duration;
            try {
                DateFormat df = new SimpleDateFormat("H:mm");
                Date tripDate = df.parse(duration);
                if (tripDate.getHours() == 0) {
                    int start = duration.indexOf(":") + 1;
                    if (duration.substring(start).startsWith("0")) {
                        start++;
                    }
                    durationInMinutes = duration.substring(start) + " min";
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing duration, " + e.getMessage());
            }

            /*int end = departureDate.lastIndexOf(".");
            String departureDateString =
                departureDate.substring(0, end).replace(".", "/");*/

            return String.format("%s - %s (%s)",
                    departureTime, arrivalTime, durationInMinutes);
        }

        public static final Creator<Trip2> CREATOR = new Creator<Trip2>() {
            public Trip2 createFromParcel(Parcel parcel) {
                return new Trip2(parcel);
            }

            public Trip2[] newArray(int size) {
                return new Trip2[size];
            }
        };
    }

    public static class SubTrip implements Parcelable {
        public Location origin;
        public Location destination;
        public String departureDate; // TODO: Combine date and time
        public String departureTime;
        public String arrivalDate; // TODO: Combine date and time
        public String arrivalTime;
        public TransportType transport;
        public ArrayList<String> remarks = new ArrayList<String>();
        public ArrayList<String> rtuMessages = new ArrayList<String>();
        public ArrayList<String> mt6Messages = new ArrayList<String>();

        public SubTrip() {}

        public SubTrip(Parcel parcel) {
            origin = parcel.readParcelable(Location.class.getClassLoader());
            destination = parcel.readParcelable(Location.class.getClassLoader());
            departureDate = parcel.readString();
            departureTime = parcel.readString();
            arrivalDate = parcel.readString();
            arrivalTime = parcel.readString();
            transport = parcel.readParcelable(TransportType.class.getClassLoader());
            remarks = new ArrayList<String>();
            parcel.readStringList(remarks);
            rtuMessages = new ArrayList<String>();
            parcel.readStringList(rtuMessages);
            mt6Messages = new ArrayList<String>();
            parcel.readStringList(mt6Messages);
        }

        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(origin, 0);
            dest.writeParcelable(destination, 0);
            dest.writeString(departureDate);
            dest.writeString(departureTime);
            dest.writeString(arrivalDate);
            dest.writeString(arrivalTime);
            dest.writeParcelable(transport, 0);
            dest.writeStringList(remarks);
            dest.writeStringList(rtuMessages);
            dest.writeStringList(mt6Messages);
        }

        public static SubTrip fromJson(JSONObject json) throws JSONException {
            SubTrip st = new SubTrip();

            st.origin = Location.fromJson(json.getJSONObject("origin"));
            st.destination = Location.fromJson(json.getJSONObject("destination"));
            st.departureDate = json.getString("departureDate");
            st.departureTime = json.getString("departureTime");
            st.arrivalDate = json.getString("arrivalDate");
            st.arrivalTime = json.getString("arrivalTime");
            st.transport = TransportType.fromJson(json.getJSONObject("transport"));

            if (json.has("remarks")) {
                fromJsonArray(json.getJSONArray("remarks"), st.remarks);
            }
            if (json.has("rtuMessages")) {
                fromJsonArray(json.getJSONArray("rtuMessages"), st.rtuMessages);
            }
            if (json.has("mt6Messages")) {
                fromJsonArray(json.getJSONArray("mt6Messages"), st.mt6Messages);
            }

            return st;
        }

        private static void fromJsonArray(JSONArray jsonArray, ArrayList<String> list)
                throws JSONException {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        }

        @Override
        public String toString() {
            return "SubTrip [arrivalDate=" + arrivalDate + ", arrivalTime="
                    + arrivalTime + ", departureDate=" + departureDate
                    + ", departureTime=" + departureTime + ", destination="
                    + destination + ", mt6Messages=" + mt6Messages
                    + ", origin=" + origin + ", remarks=" + remarks
                    + ", rtuMessages=" + rtuMessages + ", transport="
                    + transport + "]";
        }



        public static final Creator<SubTrip> CREATOR = new Creator<SubTrip>() {
            public SubTrip createFromParcel(Parcel parcel) {
                return new SubTrip(parcel);
            }

            public SubTrip[] newArray(int size) {
                return new SubTrip[size];
            }
        };
    }


    public static class Location implements Parcelable {
        public static String TYPE_MY_LOCATION = "MY_LOCATION";
        public int id = 0;
        public String name;
        public int latitude;
        public int longitude;

        public Location() {}

        public Location(Location location) {
            id = location.id;
            name = location.name;
            latitude = location.latitude;
            longitude = location.longitude;
        }
        
        public Location(Parcel parcel) {
            id = parcel.readInt();
            name = parcel.readString();
            latitude = parcel.readInt();
            longitude = parcel.readInt();
        }

        public boolean isMyLocation() {
            return TYPE_MY_LOCATION.equals(name);
        }

        public boolean hasLocation() {
            return latitude != 0 && longitude != 0;
        }

        public static Location fromJson(JSONObject json) throws JSONException {
            Location l = new Location();
            l.id = json.getInt("id");
            l.name = json.getString("name");
            l.longitude = json.getInt("longitude");
            l.latitude = json.getInt("latitude");
            return l;
        }

        @Override
        public String toString() {
            return "Location{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    '}';
        }

        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(id);
            dest.writeString(name);
            dest.writeInt(latitude);
            dest.writeInt(longitude);
        }

        public static final Creator<Location> CREATOR = new Creator<Location>() {
            public Location createFromParcel(Parcel parcel) {
                return new Location(parcel);
            }

            public Location[] newArray(int size) {
                return new Location[size];
            }
        };
    }

    public static class TransportType implements Parcelable {
        public String type = "";
        public String name = "";
        public String towards = "";

        public TransportType() { }

        public TransportType(Parcel parcel) {
            type = parcel.readString();
            name = parcel.readString();
            towards = parcel.readString();
        }

        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(type);
            dest.writeString(name);
            dest.writeString(towards);
        }

        public static TransportType fromJson(JSONObject json) throws JSONException {
            TransportType t = new TransportType();
            if (json.has("name")) {
                t.name = json.getString("name");
            }
            if (json.has("towards")) {
                t.towards = json.getString("towards");
            }
            t.type = json.getString("type");
            return t;
        }

        public int getImageResource() {
            if ("BUS".equals(type) || "NAR".equals(type)) {
                return R.drawable.transport_bus;
            } else if ("MET".equals(type)) {
                if (name.contains("grön")) {
                    return R.drawable.transport_metro_green;
                } else if (name.contains("röd")) {
                    return R.drawable.transport_metro_red;
                } else if (name.contains("blå")) {
                    return R.drawable.transport_metro_blue;
                }
            } else if ("Walk".equals(type)) {
                return R.drawable.transport_walk;
            } else if ("TRN".equals(type)) {
                return R.drawable.transport_train;
            } else if ("TRM".equals(type)) {
                return R.drawable.transport_lokalbana;
            } else if ("SHP".equals(type)) {
                return R.drawable.transport_boat;
            }

            Log.d(TAG, "Unknown transport type " + type);
            return R.drawable.transport_unkown;
        }

        @Override
        public String toString() {
            return "Transport{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", towards='" + towards + '\'' +
                    '}';
        }

        public static final Creator<TransportType> CREATOR = new Creator<TransportType>() {
            public TransportType createFromParcel(Parcel parcel) {
                return new TransportType(parcel);
            }

            public TransportType[] newArray(int size) {
                return new TransportType[size];
            }
        };
    }
}
