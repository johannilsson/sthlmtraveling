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
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint2;
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
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
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

    public static int plannerErrorCodeToStringRes(String errorCode) {
        // Make sure to have the common error codes as high as possible.
        if ("H9220".equals(errorCode)) {
            return R.string.planner_error_H9220;
        } else if ("H895".equals(errorCode)) {
            return R.string.planner_error_H895;
        } else if ("H9300".equals(errorCode)) {
            return R.string.planner_error_H9300;
        } else if ("H9360".equals(errorCode)) {
            return R.string.planner_error_H9360;
        } else if ("H9380".equals(errorCode)) {
            return R.string.planner_error_H9380;
        } else if ("H9320".equals(errorCode)) {
            return R.string.planner_error_H9320;
        } else if ("H9280".equals(errorCode)) {
            return R.string.planner_error_H9280;
        } else if ("H9260".equals(errorCode)) {
            return R.string.planner_error_H9260;
        } else if ("H9250".equals(errorCode)) {
            return R.string.planner_error_H9250;
        } else if ("H9240".equals(errorCode)) {
            return R.string.planner_error_H9240;
        } else if ("H9230".equals(errorCode)) {
            return R.string.planner_error_H9230;
        } else if ("H900".equals(errorCode)) {
            return R.string.planner_error_H900;
        } else if ("H892".equals(errorCode)) {
            return R.string.planner_error_H892;
        } else if ("H891".equals(errorCode)) {
            return R.string.planner_error_H891;
        } else if ("H890".equals(errorCode)) {
            return R.string.planner_error_H890;
        } else if ("H500".equals(errorCode)) {
            return R.string.planner_error_H500;
        } else if ("H455".equals(errorCode)) {
            return R.string.planner_error_H455;
        } else if ("H410".equals(errorCode)) {
            return R.string.planner_error_H410;
        } else if ("H390".equals(errorCode)) {
            return R.string.planner_error_H390;
        }

        return R.string.planner_error_unknown;
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

    public Response findPreviousJourney(JourneyQuery query) throws IOException, BadResponse {
        return doQuery(query, 2);
    }

    public Response findNextJourney(JourneyQuery query) throws IOException, BadResponse {
        return doQuery(query, 1);
    }

    public Response findJourney(JourneyQuery query) throws IOException, BadResponse {
        return doQuery(query, -1);
    }

    private Response doQuery(JourneyQuery query, int scrollDirection) throws IOException, BadResponse {

        Uri u = Uri.parse(apiEndpoint2());
        Uri.Builder b = u.buildUpon();
        b.appendEncodedPath("journey/v1/");
        if (scrollDirection > -1) {
            b.appendQueryParameter("dir", String.valueOf(scrollDirection));
            b.appendQueryParameter("ident", query.ident);
            b.appendQueryParameter("seq", query.seqnr);
        } else {
            b.appendQueryParameter("origin", query.origin.name);
            if (query.origin.hasLocation()) {
                b.appendQueryParameter("origin_latitude", String.valueOf(query.origin.latitude / 1E6));
                b.appendQueryParameter("origin_longitude", String.valueOf(query.origin.longitude / 1E6));
            }
            b.appendQueryParameter("destination", query.destination.name);
            if (query.destination.hasLocation()) {
                b.appendQueryParameter("destination_latitude", String.valueOf(query.destination.latitude / 1E6));
                b.appendQueryParameter("destination_longitude", String.valueOf(query.destination.longitude / 1E6));
            }
            for (String transportMode : query.transportModes) {
                b.appendQueryParameter("transport", transportMode);
            }
            if (query.time != null) {
                b.appendQueryParameter("date", query.time.format("%d.%m.%Y"));
                b.appendQueryParameter("time", query.time.format("%H:%M"));
            }
            if (!query.isTimeDeparture) {
                b.appendQueryParameter("arrival", "1");
            }
            if (query.hasVia()) {
                b.appendQueryParameter("via", query.via.name);
            }
            if (query.alternativeStops) {
                b.appendQueryParameter("alternative", "1");
            }
        }

        // Include intermediate stops.
        //b.appendQueryParameter("intermediate_stops", "1");

        u = b.build();

        final HttpGet get = new HttpGet(u.toString());
        get.addHeader("X-STHLMTraveling-API-Key", get(KEY));
        final HttpResponse response = HttpManager.execute(get);

        HttpEntity entity;
        Response r = null;
        String rawContent;
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
        case HttpStatus.SC_OK:
            entity = response.getEntity();
            rawContent = StreamUtils.toString(entity.getContent());
            try {
                JSONObject baseResponse = new JSONObject(rawContent);
                if (baseResponse.has("journey")) {
                    r = Response.fromJson(baseResponse.getJSONObject("journey"));
                } else {
                    Log.w(TAG, "Invalid response");
                    // TODO: Parse errors.
                }
                
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
            if (TextUtils.isEmpty(tariffRemark)) {
                return false;
            }
            return tariffRemark.startsWith("2") ||
                tariffRemark.startsWith("3") ||
                tariffRemark.startsWith("4");
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
            trip.departureDate = json.getString("departure_date");

            JSONArray jsonSubTrips = json.getJSONArray("sub_trips");
            for (int i = 0; i < jsonSubTrips.length(); i++) {
                try {
                    trip.subTrips.add(SubTrip.fromJson(jsonSubTrips.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Failed to parse sub trip: " + e.getMessage());
                }
            }

            trip.arrivalDate = json.getString("arrival_date");
            trip.arrivalTime = json.getString("arrival_time");
            trip.changes = json.getInt("changes");
            trip.co2 = json.getString("co2");
            trip.departureDate = json.getString("departure_date");
            trip.departureTime = json.getString("departure_time");
            trip.destination = Location.fromJson(json.getJSONObject("destination"));
            trip.duration = json.getString("duration");
            trip.mt6MessageExist = json.getBoolean("mt6_messages_exist");
            trip.origin = Location.fromJson(json.getJSONObject("origin"));
            if (json.has("tariffZones")) {
                trip.tariffZones = json.getString("tariff_zones");
            }
            if (json.has("tariffRemark")) {
                trip.tariffRemark = json.getString("tariff_remark");
            }
            trip.remarksMessageExist = json.getBoolean("remark_messages_exist");
            trip.rtuMessageExist = json.getBoolean("rtu_messages_exist");

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

    /**
     * Representation of a intermediate stop.
     */
    public static class IntermediateStop implements Parcelable {

        public String arrivalDate;
        public String arrivalTime;
        public Location location;

        public IntermediateStop(Parcel parcel) {
            arrivalDate = parcel.readString();
            arrivalTime = parcel.readString();
            location = parcel.readParcelable(Location.class.getClassLoader());
        }

        public IntermediateStop() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(arrivalDate);
            dest.writeString(arrivalTime);
            dest.writeParcelable(location, 0);
        }

        public static IntermediateStop fromJson(JSONObject json)
                throws JSONException {
            IntermediateStop is = new IntermediateStop();
            is.arrivalDate = json.getString("arrival_date");
            is.arrivalTime = json.getString("arrival_time");
            is.location = Location.fromJson(json.getJSONObject("location"));
            return is;
        }

        @Override
        public String toString() {
            return "IntermediateStop [arrivalDate=" + arrivalDate
                    + ", arrivalTime=" + arrivalTime + ", location=" + location
                    + "]";
        }

        public static final Creator<IntermediateStop> CREATOR = new Creator<IntermediateStop>() {
            public IntermediateStop createFromParcel(Parcel parcel) {
                return new IntermediateStop(parcel);
            }

            public IntermediateStop[] newArray(int size) {
                return new IntermediateStop[size];
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
        public String reference;
        public ArrayList<IntermediateStop> intermediateStop =
            new ArrayList<IntermediateStop>();

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
            reference = parcel.readString();
            intermediateStop = new ArrayList<IntermediateStop>();
            parcel.readList(intermediateStop, Location.class.getClassLoader());
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
            dest.writeParcelable(transport, 0);
            dest.writeStringList(remarks);
            dest.writeStringList(rtuMessages);
            dest.writeStringList(mt6Messages);
            dest.writeString(reference);
            dest.writeList(intermediateStop);
        }

        public static SubTrip fromJson(JSONObject json) throws JSONException {
            SubTrip st = new SubTrip();

            st.origin = Location.fromJson(json.getJSONObject("origin"));
            st.destination = Location.fromJson(json.getJSONObject("destination"));
            st.departureDate = json.getString("departure_date");
            st.departureTime = json.getString("departure_time");
            st.arrivalDate = json.getString("arrival_date");
            st.arrivalTime = json.getString("arrival_time");
            st.transport = TransportType.fromJson(json.getJSONObject("transport"));

            if (json.has("remark_messages")) {
                fromJsonArray(json.getJSONArray("remark_messages"), st.remarks);
            }
            if (json.has("rtu_messages")) {
                fromJsonArray(json.getJSONArray("rtu_messages"), st.rtuMessages);
            }
            if (json.has("mt6_messages")) {
                fromJsonArray(json.getJSONArray("mt6_messages"), st.mt6Messages);
            }
            st.reference = json.getString("reference");
            if (json.has("intermediate_stops") && !json.isNull("intermediate_stops")) {
                JSONArray intermediateStopJsonArray = json.getJSONArray("intermediate_stops");
                for (int i = 0; i < intermediateStopJsonArray.length(); i++) {
                    st.intermediateStop.add(IntermediateStop.fromJson(
                            intermediateStopJsonArray.getJSONObject(i)));
                }
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
                    + destination + ", intermediateStop=" + intermediateStop
                    + ", mt6Messages=" + mt6Messages + ", origin=" + origin
                    + ", reference=" + reference + ", remarks=" + remarks
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
            //l.id = json.getInt("id");
            l.name = json.getString("name");
            l.longitude = (int) (json.getDouble("longitude") * 1E6);
            l.latitude = (int) (json.getDouble("latitude") * 1E6);
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

        public boolean hasName() {
            return !TextUtils.isEmpty(name);
        }
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
            if ("BUS".equals(type)) {
                if (name.contains("blå")) {
                    return R.drawable.transport_bus_blue;
                }
                return R.drawable.transport_bus_red;
            } else if ("MET".equals(type)) {
                if (name.contains("grön")) {
                    return R.drawable.transport_metro_green;
                } else if (name.contains("röd")) {
                    return R.drawable.transport_metro_red;
                } else if (name.contains("blå")) {
                    return R.drawable.transport_metro_blue;
                }
            } else if ("NAR".equals(type)) {
                return R.drawable.transport_nar;
            } else if ("Walk".equals(type)) {
                return R.drawable.transport_walk;
            } else if ("TRN".equals(type)) {
                return R.drawable.transport_train;
            } else if ("TRM".equals(type)) {
                return R.drawable.transport_lokalbana;
            } else if ("SHP".equals(type)) {
                return R.drawable.transport_boat;
            } else if ("FLY".equals(type)) {
                return R.drawable.transport_fly;
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
