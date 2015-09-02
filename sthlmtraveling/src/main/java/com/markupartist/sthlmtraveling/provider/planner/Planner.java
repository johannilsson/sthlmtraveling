/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.utils.HttpHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint2;

/**
 * Journey planner for the sl.se API.
 */
public class Planner {
    private static final String TAG = "Planner";

    private static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.US);

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

    public Response findPreviousJourney(final Context context, JourneyQuery query) throws IOException, BadResponse {
        return doJourneyQuery(context, query, 2);
    }

    public Response findNextJourney(final Context context, JourneyQuery query) throws IOException, BadResponse {
        return doJourneyQuery(context, query, 1);
    }

    public Response findJourney(final Context context, JourneyQuery query) throws IOException, BadResponse {
        return doJourneyQuery(context, query, -1);
    }

    public Trip2 addIntermediateStops(final Context context, Trip2 trip, JourneyQuery query)
            throws IOException{
        Uri u = Uri.parse(apiEndpoint2());
        Uri.Builder b = u.buildUpon();
        b.appendEncodedPath("journey/v1/intermediate/");
        b.appendQueryParameter("ident", query.ident);
        b.appendQueryParameter("seqnr", query.seqnr);
        int references = 0;
        String reference = null;
        for (SubTrip st : trip.subTrips) {
            if ((!TextUtils.isEmpty(st.reference))
                    && st.intermediateStop.isEmpty()) {
                b.appendQueryParameter("reference", st.reference);
                references++;
                reference = st.reference;
            }
        }
        u = b.build();

        if (references == 0) {
            return trip;
        }

        HttpHelper httpHelper = HttpHelper.getInstance(context);
        com.squareup.okhttp.Response response = httpHelper.getClient().newCall(
                httpHelper.createRequest(u.toString())).execute();

        String rawContent;
        int statusCode = response.code();
        switch (statusCode) {
        case 200:
            rawContent = response.body().string();
            try {
                JSONObject baseResponse = new JSONObject(rawContent);
                if (baseResponse.has("stops")) {
                    if (baseResponse.isNull("stops")) {
                        Log.d(TAG, "stops was null, ignoring.");
                    } else if (references == 1) {
                        JSONArray intermediateStopsJson = baseResponse.getJSONArray("stops");
                        for (SubTrip st : trip.subTrips) {
                            if (reference.equals(st.reference)) {
                                for (int i = 0; i < intermediateStopsJson.length(); i++) {
                                    st.intermediateStop.add(IntermediateStop.fromJson(
                                            intermediateStopsJson.getJSONObject(i)));
                                }
                            }
                        }
                    } else {
                        JSONObject intermediateStopsJson = baseResponse.getJSONObject("stops");
                        for (SubTrip st : trip.subTrips) {
                            if (intermediateStopsJson.has(st.reference)) {
                                JSONArray jsonArray = intermediateStopsJson.getJSONArray(st.reference);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    st.intermediateStop.add(IntermediateStop.fromJson(
                                            jsonArray.getJSONObject(i)));
                                }
                            }
                        }
                    }

                } else {
                    Log.w(TAG, "Invalid response when fetching intermediate stops.");
                }
            } catch (JSONException e) {
                Log.w(TAG, "Could not parse the reponse for intermediate stops.");
            }
            break;
        case 400:  // Bad request
            rawContent = response.body().string();
            try {
                BadResponse br = BadResponse.fromJson(new JSONObject(rawContent));
                Log.e(TAG, "Invalid response for intermediate stops: " + br.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse the reponse for intermediate stops.");
            }
        default:
            Log.e(TAG, "Status code not OK from intermediate stops API, was " + statusCode);
        }

        return trip;
    }

    public SubTrip addIntermediateStops(Context context, SubTrip subTrip, JourneyQuery query)
            throws IOException {
        Uri u = Uri.parse(apiEndpoint2());
        Uri.Builder b = u.buildUpon();
        b.appendEncodedPath("journey/v1/intermediate/");
        b.appendQueryParameter("ident", query.ident);
        b.appendQueryParameter("seqnr", query.seqnr);
        b.appendQueryParameter("reference", subTrip.reference);

        u = b.build();

        HttpHelper httpHelper = HttpHelper.getInstance(context);
        com.squareup.okhttp.Response response = httpHelper.getClient().newCall(
                httpHelper.createRequest(u.toString())).execute();

        String rawContent;
        int statusCode = response.code();
        switch (statusCode) {
        case 200:
            rawContent = response.body().string();
            try {
                JSONObject baseResponse = new JSONObject(rawContent);
                if (baseResponse.has("stops")) {
                    JSONArray intermediateStopJsonArray = baseResponse.getJSONArray("stops");
                    for (int i = 0; i < intermediateStopJsonArray.length(); i++) {
                        subTrip.intermediateStop.add(IntermediateStop.fromJson(
                                intermediateStopJsonArray.getJSONObject(i)));
                    }
                } else {
                    Log.w(TAG, "Invalid response when fetching intermediate stops.");
                }
            } catch (JSONException e) {
                Log.w(TAG, "Could not parse the reponse for intermediate stops.");
            }
            break;
        case 400:
            rawContent = response.body().string();
            try {
                BadResponse br = BadResponse.fromJson(new JSONObject(rawContent));
                Log.e(TAG, "Invalid response for intermediate stops: " + br.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse the reponse for intermediate stops.");
            }
        default:
            Log.e(TAG, "Status code not OK from intermediate stops API, was " + statusCode);
        }

        return subTrip;
    }

    private Response doJourneyQuery(final Context context, JourneyQuery query, int scrollDirection) throws IOException, BadResponse {

        Uri u = Uri.parse(apiEndpoint2());
        Uri.Builder b = u.buildUpon();
        b.appendEncodedPath("v1/journey/");
        if (scrollDirection > -1) {
            b.appendQueryParameter("dir", String.valueOf(scrollDirection));
            b.appendQueryParameter("ident", query.ident);
            b.appendQueryParameter("seq", query.seqnr);
        } else {
            if (query.origin.hasLocation()) {
                b.appendQueryParameter("origin", query.origin.name);
                b.appendQueryParameter("origin_latitude", String.valueOf(query.origin.latitude / 1E6));
                b.appendQueryParameter("origin_longitude", String.valueOf(query.origin.longitude / 1E6));
            } else {
                b.appendQueryParameter("origin", String.valueOf(query.origin.getNameOrId()));
            }
            if (query.destination.hasLocation()) {
                b.appendQueryParameter("destination", query.destination.name);
                b.appendQueryParameter("destination_latitude", String.valueOf(query.destination.latitude / 1E6));
                b.appendQueryParameter("destination_longitude", String.valueOf(query.destination.longitude / 1E6));
            } else {
                b.appendQueryParameter("destination", String.valueOf(query.destination.getNameOrId()));
            }
            for (String transportMode : query.transportModes) {
                b.appendQueryParameter("transport", transportMode);
            }
            if (query.time != null) {
                b.appendQueryParameter("date", DATE_FORMAT.format(query.time));//query.time.format("%d.%m.%Y"));
                b.appendQueryParameter("time", TIME_FORMAT.format(query.time)); //query.time.format("%H:%M"));
            }
            if (!query.isTimeDeparture) {
                b.appendQueryParameter("arrival", "1");
            }
            if (query.hasVia()) {
                if (query.via.id == 0) {
                    b.appendQueryParameter("via", query.via.name);
                } else {
                    b.appendQueryParameter("via", String.valueOf(query.via.id));
                }
            }
            if (query.alternativeStops) {
                b.appendQueryParameter("alternative", "1");
            }
        }

        b.appendQueryParameter("with_site_id", "1");

        // Include intermediate stops.
        //b.appendQueryParameter("intermediate_stops", "1");

        u = b.build();

        Log.e(TAG, "Query: " + u.toString());

        HttpHelper httpHelper = HttpHelper.getInstance(context);
        com.squareup.okhttp.Response response = httpHelper.getClient().newCall(
                httpHelper.createRequest(u.toString())).execute();

        Response r = null;
        String rawContent;
        int statusCode = response.code();
        switch (statusCode) {
        case 200:
            rawContent = response.body().string();
            try {
                JSONObject baseResponse = new JSONObject(rawContent);
                if (baseResponse.has("journey")) {
                    r = Response.fromJson(baseResponse.getJSONObject("journey"));
                } else {
                    Log.w(TAG, "Invalid response");
                }
            } catch (JSONException e) {
                Log.d(TAG, "Could not parse the reponse...");
                throw new IOException("Could not parse the response.");
            }
            break;
        case 400:
            rawContent = response.body().string();
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
            if (json.has("code")) {
                br.errorCode = json.getString("code");
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

        @Override
        public String toString() {
            return "BadResponse [description=" + description + ", errorCode="
                    + errorCode + "]";
        }

    }
    
    public static class Response implements Parcelable {
        // TODO: Parse out the ident.
        public String ident;
        public String seqnr;
        public int    numberOfTrips;
        public ArrayList<Trip2> trips = new ArrayList<Trip2>();
        private String tariffZones;
        public boolean hasPromotions;
        public int promotionNetwork = -1;

        public Response() {
        }

        public Response(Parcel parcel) {
            ident = parcel.readString();
            seqnr = parcel.readString();
            trips = new ArrayList<Trip2>();
            parcel.readTypedList(trips, Trip2.CREATOR);
            tariffZones = parcel.readString();
            hasPromotions = (parcel.readInt() == 1);
            promotionNetwork = parcel.readInt();
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
            dest.writeString(tariffZones);
            dest.writeInt(hasPromotions ? 1 : 0);
            dest.writeInt(promotionNetwork);
        }

        public boolean canBuySmsTicket() {
            tariffZones = null;
            for (Trip2 trip : trips) {
                if (!trip.canBuySmsTicket()) {
                    return false;
                }
                if (tariffZones != null && !tariffZones.equals(trip.tariffZones)) {
                    tariffZones = null;
                    return false;
                }
                tariffZones = trip.tariffZones;
            }
            return true;
        }

        public String getTariffZones() {
            if (tariffZones == null) {
                for (Trip2 trip : trips) {
                    if (tariffZones != null && !tariffZones.equals(trip.tariffZones)) {
                        tariffZones = null;
                        return null;
                    }
                    tariffZones = trip.tariffZones;
                }
            }
            return tariffZones;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "ident='" + ident + '\'' +
                    ", seqnr='" + seqnr + '\'' +
                    ", numberOfTrips=" + numberOfTrips +
                    ", trips=" + trips +
                    ", hasPromotions=" + hasPromotions +
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
            if (json.has("has_promotions")) {
                r.hasPromotions = json.getBoolean("has_promotions");
            }
            if (json.has("promotion_network")) {
                r.promotionNetwork = json.getInt("promotion_network");
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

        private static DateFormat DURATION_FORMAT = new SimpleDateFormat("H:mm", Locale.US);

        public Location origin;
        public Location destination;
        public String   departureDate; // TODO: Combine date and time
        public String   departureTime;
        public String   arrivalDate; // TODO: Combine date and time
        public String   arrivalTime;
        public int      changes;
        public String   duration;
        public String   tariffZones;
        public String   tariffRemark;
        public String   co2;
        public boolean  mt6MessageExist;
        public boolean  rtuMessageExist;
        public boolean  remarksMessageExist;
        public ArrayList<SubTrip> subTrips = new ArrayList<SubTrip>();
        private Date departureDateTime;
        private Date arrivalDateTime;

        public Trip2() {
        }

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
            // TODO: Add support for checking tariff messages
            return tariffZones.equals("A") ||
                    tariffZones.equals("B") ||
                    tariffZones.equals("C") ||
                    tariffZones.equals("AB") ||
                    tariffZones.equals("BC") ||
                    tariffZones.equals("ABC");
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
            if (json.has("tariff_zones")) {
                trip.tariffZones = json.getString("tariff_zones");
                trip.tariffZones = trip.tariffZones.trim();
            }
            if (json.has("tariff_remark")) {
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

        public Date departure() {
            if (departureDateTime == null) {
                departureDateTime = DateTimeUtil.fromSlDateTime(departureDate, departureTime);
            }
            return departureDateTime;
        }

        public Date arrival() {
            if (arrivalDateTime == null) {
                arrivalDateTime = DateTimeUtil.fromSlDateTime(arrivalDate, arrivalTime);
            }
            return arrivalDateTime;
        }

        public String toTimeDisplay(Context context) {
            DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
            return context.getResources().getString(R.string.trip_time_origin_destination,
                    format.format(departure()), format.format(arrival()));
        }

        public String getDurationText(Resources resources) {
            String durationInMinutes = duration;
            try {
                DateFormat df = new SimpleDateFormat("H:mm", Locale.US);
                Date tripDate = df.parse(duration);
                if (tripDate.getHours() == 0) {
                    int start = duration.indexOf(":") + 1;
                    if (duration.substring(start).startsWith("0")) {
                        start++;
                    }
                    int minutes = Integer.parseInt(duration.substring(start));
                    durationInMinutes = resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing duration, " + e.getMessage());
            }
            return durationInMinutes;
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

        public String   arrivalDate;
        public String   arrivalTime;
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
        public Location      origin;
        public Location      destination;
        public String        departureDate; // TODO: Combine date and time
        public String        departureTime;
        public String        arrivalDate; // TODO: Combine date and time
        public String        arrivalTime;
        public TransportType transport;
        public ArrayList<String> remarks     = new ArrayList<String>();
        public ArrayList<String> rtuMessages = new ArrayList<String>();
        public ArrayList<String> mt6Messages = new ArrayList<String>();
        public String reference;
        public ArrayList<IntermediateStop> intermediateStop =
                new ArrayList<IntermediateStop>();
        private Date departureDateTime;
        private Date arrivalDateTime;

        public SubTrip() {
        }

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
            if (!json.isNull("reference")) {
                st.reference = json.getString("reference");
            }
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

        public long getDurationMillis() {
            return getArrival().getTime() - getDeparture().getTime();
        }

        public Date getDeparture() {
            if (departureDateTime == null) {
                departureDateTime = DateTimeUtil.fromSlDateTime(departureDate, departureTime);
            }
            return departureDateTime;
        }

        public Date getArrival() {
            if (arrivalDateTime == null) {
                arrivalDateTime = DateTimeUtil.fromSlDateTime(arrivalDate, arrivalTime);
            }
            return arrivalDateTime;
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

        public String getNameOrId() {
            if (hasLocation() || id == 0) {
                return name;
            }
            return String.valueOf(id);
        }

        public static Location fromJson(JSONObject json) throws JSONException {
            Location l = new Location();
            if (json.has("id")) {
                l.id = json.getInt("id");
            }
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

        /**
         * Get a clean representation of the stop name.
         *
         * @return The name
         */
        public CharSequence getCleanName() {
            if (TextUtils.isEmpty(name)) {
                return "";
            }
            return name.replaceAll("\\(.*\\)", "");
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
                return R.drawable.transport_tram_car;
            } else if ("SHP".equals(type)) {
                return R.drawable.transport_boat;
            } else if ("FLY".equals(type)) {
                return R.drawable.transport_fly;
            } else if ("AEX".equals(type)) {
                return R.drawable.transport_aex;
            }

            Log.d(TAG, "Unknown transport type " + type);
            return R.drawable.transport_unkown;
        }

        public int getColor(final Context context) {
            if ("BUS".equals(type)) {
                if (name.contains("blå")) {
                    return context.getResources().getColor(R.color.bus_blue);
                }
                return context.getResources().getColor(R.color.metro_red);
            } else if ("MET".equals(type)) {
                if (name.contains("grön")) {
                    return context.getResources().getColor(R.color.metro_green);
                } else if (name.contains("röd")) {
                    return context.getResources().getColor(R.color.metro_red);
                } else if (name.contains("blå")) {
                    return context.getResources().getColor(R.color.metro_blue);
                }
            } else if ("NAR".equals(type)) {
                return Color.WHITE;
            } else if ("Walk".equals(type)) {
                return Color.BLACK;
            } else if ("TRN".equals(type)) {
                return context.getResources().getColor(R.color.train);
            } else if ("TRM".equals(type)) {
                return context.getResources().getColor(R.color.train);
            } else if ("SHP".equals(type)) {
                return 0xff09693E;
            } else if ("FLY".equals(type)) {
                return Color.DKGRAY;
            } else if ("AEX".equals(type)) {
                return Color.YELLOW;
            }

            Log.d(TAG, "Unknown transport type " + type);
            return Color.DKGRAY;
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
