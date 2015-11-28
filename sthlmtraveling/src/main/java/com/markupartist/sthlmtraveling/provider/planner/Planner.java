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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.text.BidiFormatter;
import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.DateTimeUtil;
import com.markupartist.sthlmtraveling.data.misc.HttpHelper;
import com.markupartist.sthlmtraveling.utils.RtlUtils;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

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
                Log.w(TAG, "Could not parse the response for intermediate stops.");
            }
            break;
        case 400:  // Bad request
            rawContent = response.body().string();
            try {
                BadResponse br = BadResponse.fromJson(new JSONObject(rawContent));
                Log.e(TAG, "Invalid error response for intermediate stops: " + br.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Could not parse the error response for intermediate stops.");
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

    private boolean shouldUseIdInQuery(Site site) {
        return !site.isMyLocation()
                && site.getSource() == Site.SOURCE_STHLM_TRAVELING
                && !TextUtils.isEmpty(site.getId())
                && !site.getId().equals("0");
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
            if (shouldUseIdInQuery(query.origin)) {
                b.appendQueryParameter("origin", query.origin.getId());
            } else if (query.origin.hasLocation()) {
                b.appendQueryParameter("origin", query.origin.getName());
                b.appendQueryParameter("origin_latitude", String.valueOf(query.origin.getLocation().getLatitude()));
                b.appendQueryParameter("origin_longitude", String.valueOf(query.origin.getLocation().getLongitude()));
            } else {
                b.appendQueryParameter("origin", query.origin.getNameOrId());
            }
            if (shouldUseIdInQuery(query.destination)) {
                b.appendQueryParameter("destination", query.destination.getId());
            } else if (query.destination.hasLocation()) {
                b.appendQueryParameter("destination", query.destination.getName());
                b.appendQueryParameter("destination_latitude", String.valueOf(query.destination.getLocation().getLatitude()));
                b.appendQueryParameter("destination_longitude", String.valueOf(query.destination.getLocation().getLongitude()));
            } else {
                b.appendQueryParameter("destination", query.destination.getNameOrId());
            }
            for (String transportMode : query.transportModes) {
                b.appendQueryParameter("transport", transportMode);
            }
            if (query.time != null) {
                b.appendQueryParameter("date", DATE_FORMAT.format(query.time));
                b.appendQueryParameter("time", TIME_FORMAT.format(query.time));
            }
            if (!query.isTimeDeparture) {
                b.appendQueryParameter("arrival", "1");
            }
            if (query.hasVia()) {
                if (query.via.getId() == null) {
                    b.appendQueryParameter("via", query.via.getName());
                } else {
                    b.appendQueryParameter("via", String.valueOf(query.via.getId()));
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


        //Log.e(TAG, "Query: " + query.toString());
        //Log.e(TAG, "Query: " + u.toString());

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

    static Site fromJson(JSONObject json) throws JSONException {
        Site site = new Site();

        if (json.has("id")) {
            site.setId(json.getString("id"));
        }
        if (json.has("latitude") && json.has("longitude")) {
            site.setLocation(json.getDouble("latitude"), json.getDouble("longitude"));
        }

        site.setName(json.getString("name"));

        return site;
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

        public Site origin;
        public Site destination;
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
            origin = parcel.readParcelable(Site.class.getClassLoader());
            destination = parcel.readParcelable(Site.class.getClassLoader());
            departureDate = parcel.readString();
            departureTime = parcel.readString();
            arrivalDate = parcel.readString();
            arrivalTime = parcel.readString();
            changes = parcel.readInt();
            duration = parcel.readString();
            tariffZones = parcel.readString();
            tariffRemark = parcel.readString();
            co2 = parcel.readString();
            mt6MessageExist = (parcel.readInt() == 1);
            rtuMessageExist = (parcel.readInt() == 1);
            remarksMessageExist = (parcel.readInt() == 1);
            subTrips = new ArrayList<>();
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
            dest.writeInt((mt6MessageExist) ? 1 : 0);
            dest.writeInt((rtuMessageExist) ? 1 : 0);
            dest.writeInt((remarksMessageExist) ? 1 : 0);
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
            trip.destination = Planner.fromJson(json.getJSONObject("destination"));
            trip.duration = json.getString("duration");
            trip.mt6MessageExist = json.getBoolean("mt6_messages_exist");
            trip.origin = Planner.fromJson(json.getJSONObject("origin"));
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
            BidiFormatter bidiFormatter = BidiFormatter.getInstance(RtlUtils.isRtl(Locale.getDefault()));
            return String.format("%s – %s",
                    bidiFormatter.unicodeWrap(format.format(departure())),
                    bidiFormatter.unicodeWrap(format.format(arrival())));
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

        public Site location;
        private Date scheduledArrivalDateTime;
        private Date expectedArrivalDateTime;
        private Date scheduledDepartureDateTime;
        private Date expectedDepartureDateTime;

        public IntermediateStop(Parcel parcel) {
            scheduledArrivalDateTime = readDate(parcel);
            expectedArrivalDateTime = readDate(parcel);
            scheduledDepartureDateTime = readDate(parcel);
            expectedDepartureDateTime = readDate(parcel);
            location = parcel.readParcelable(Site.class.getClassLoader());
        }

        public IntermediateStop() {
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(scheduledArrivalDateTime == null ? -1 : scheduledArrivalDateTime.getTime());
            dest.writeLong(expectedArrivalDateTime == null ? -1 : expectedArrivalDateTime.getTime());
            dest.writeLong(scheduledDepartureDateTime == null ? -1 : scheduledDepartureDateTime.getTime());
            dest.writeLong(expectedDepartureDateTime == null ? -1 : expectedDepartureDateTime.getTime());
            dest.writeParcelable(location, 0);
        }

        Date readDate(Parcel parcel) {
            Date date = null;
            long time = parcel.readLong();
            if (time > 0) {
                date = new Date(time);
            }
            return date;
        }

        public static IntermediateStop fromJson(JSONObject json)
                throws JSONException {
            IntermediateStop is = new IntermediateStop();
            if (!json.isNull("scheduled_departure_time")) {
                is.scheduledDepartureDateTime = DateTimeUtil.fromDateTime(json.getString("scheduled_departure_time"));
            }
            if (!json.isNull("scheduled_arrival_time")) {
                is.scheduledArrivalDateTime = DateTimeUtil.fromDateTime(json.getString("scheduled_arrival_time"));
            }
            if (!json.isNull("expected_departure_time")) {
                is.expectedDepartureDateTime = DateTimeUtil.fromDateTime(json.getString("expected_departure_time"));
            }
            if (!json.isNull("expected_arrival_time")) {
                is.expectedArrivalDateTime = DateTimeUtil.fromDateTime(json.getString("expected_arrival_time"));
            }

            if (is.scheduledDepartureDateTime != null && is.scheduledDepartureDateTime.equals(is.expectedDepartureDateTime)) {
                is.expectedDepartureDateTime = null;
            }
            if (is.scheduledArrivalDateTime != null && is.scheduledArrivalDateTime.equals(is.expectedArrivalDateTime)) {
                is.expectedArrivalDateTime = null;
            }

            is.location = Planner.fromJson(json.getJSONObject("location"));
            return is;
        }

        @Override
        public String toString() {
            return "IntermediateStop{" +
                    "expectedArrivalDateTime=" + expectedArrivalDateTime +
                    ", location=" + location +
                    ", scheduledArrivalDateTime=" + scheduledArrivalDateTime +
                    ", scheduledDepartureDateTime=" + scheduledDepartureDateTime +
                    ", expectedDepartureDateTime=" + expectedDepartureDateTime +
                    '}';
        }

        public Date getExpectedArrivalDateTime() {
            return expectedArrivalDateTime;
        }

        public Date getExpectedDepartureDateTime() {
            return expectedDepartureDateTime;
        }

        public Date getScheduledArrivalDateTime() {
            return scheduledArrivalDateTime;
        }

        public Date getScheduledDepartureDateTime() {
            return scheduledDepartureDateTime;
        }

        public static final Creator<IntermediateStop> CREATOR = new Creator<IntermediateStop>() {
            public IntermediateStop createFromParcel(Parcel parcel) {
                return new IntermediateStop(parcel);
            }

            public IntermediateStop[] newArray(int size) {
                return new IntermediateStop[size];
            }
        };

        public Date arrivalTime() {
            if (expectedArrivalDateTime != null) {
                return expectedArrivalDateTime;
            }
            return scheduledArrivalDateTime;
        }
    }

    public static class SubTrip implements Parcelable {
        public Site origin;
        public Site destination;
        public Date scheduledDepartureDateTime;
        public Date expectedDepartureDateTime;
        public Date scheduledArrivalDateTime;
        public Date expectedArrivalDateTime;
        public TransportType transport;
        public ArrayList<String> remarks = new ArrayList<>();
        public ArrayList<String> rtuMessages = new ArrayList<>();
        public ArrayList<String> mt6Messages = new ArrayList<>();
        public String reference;
        public ArrayList<IntermediateStop> intermediateStop = new ArrayList<IntermediateStop>();

        public SubTrip() {
        }

        public SubTrip(Parcel parcel) {
            origin = parcel.readParcelable(Site.class.getClassLoader());
            destination = parcel.readParcelable(Site.class.getClassLoader());
            scheduledDepartureDateTime = readDate(parcel);
            expectedDepartureDateTime = readDate(parcel);
            scheduledArrivalDateTime = readDate(parcel);
            expectedArrivalDateTime = readDate(parcel);
            transport = parcel.readParcelable(TransportType.class.getClassLoader());
            remarks = new ArrayList<>();
            parcel.readStringList(remarks);
            rtuMessages = new ArrayList<>();
            parcel.readStringList(rtuMessages);
            mt6Messages = new ArrayList<>();
            parcel.readStringList(mt6Messages);
            reference = parcel.readString();
            intermediateStop = new ArrayList<>();
            parcel.readList(intermediateStop, Site.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(origin, 0);
            dest.writeParcelable(destination, 0);
            dest.writeLong(scheduledDepartureDateTime != null ? scheduledDepartureDateTime.getTime() : -1);
            dest.writeLong(expectedDepartureDateTime != null ? expectedDepartureDateTime.getTime() : -1);
            dest.writeLong(scheduledArrivalDateTime != null ? scheduledArrivalDateTime.getTime() : -1);
            dest.writeLong(expectedArrivalDateTime != null ? expectedArrivalDateTime.getTime() : -1);
            dest.writeParcelable(transport, 0);
            dest.writeStringList(remarks);
            dest.writeStringList(rtuMessages);
            dest.writeStringList(mt6Messages);
            dest.writeString(reference);
            dest.writeList(intermediateStop);
        }

        Date readDate(Parcel parcel) {
            Date date = null;
            long time = parcel.readLong();
            if (time > 0) {
                date = new Date(time);
            }
            return date;
        }

        public static SubTrip fromJson(JSONObject json) throws JSONException {
            SubTrip st = new SubTrip();

            st.origin = Planner.fromJson(json.getJSONObject("origin"));
            st.destination = Planner.fromJson(json.getJSONObject("destination"));

            if (!json.isNull("scheduled_departure_time")) {
                st.scheduledDepartureDateTime = DateTimeUtil.fromDateTime(json.getString("scheduled_departure_time"));
            }
            if (!json.isNull("expected_departure_time")) {
                st.expectedDepartureDateTime = DateTimeUtil.fromDateTime(json.getString("expected_departure_time"));
            }
            if (!json.isNull("scheduled_arrival_time")) {
                st.scheduledArrivalDateTime = DateTimeUtil.fromDateTime(json.getString("scheduled_arrival_time"));
            }
            if (!json.isNull("expected_arrival_time")) {
                st.expectedArrivalDateTime = DateTimeUtil.fromDateTime(json.getString("expected_arrival_time"));
            }
            if (st.scheduledDepartureDateTime != null && st.scheduledDepartureDateTime.equals(st.expectedDepartureDateTime)) {
                st.expectedDepartureDateTime = null;
            }
            if (st.scheduledArrivalDateTime != null && st.scheduledArrivalDateTime.equals(st.expectedArrivalDateTime)) {
                st.expectedArrivalDateTime = null;
            }

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
            if (expectedDepartureDateTime == null) {
                return scheduledDepartureDateTime;
            }
            return expectedDepartureDateTime;
        }

        public Date getArrival() {
            if (expectedArrivalDateTime == null) {
                return scheduledArrivalDateTime;
            }
            return expectedArrivalDateTime;
        }

        @Override
        public String toString() {
            return "SubTrip{" +
                    "origin=" + origin +
                    ", destination=" + destination +
                    ", scheduledDepartureDateTime=" + scheduledDepartureDateTime +
                    ", expectedDepartureDateTime=" + expectedDepartureDateTime +
                    ", scheduledArrivalDateTime=" + scheduledArrivalDateTime +
                    ", expectedArrivalDateTime=" + expectedArrivalDateTime +
                    ", transport=" + transport +
                    ", remarks=" + remarks +
                    ", rtuMessages=" + rtuMessages +
                    ", mt6Messages=" + mt6Messages +
                    ", reference='" + reference + '\'' +
                    ", intermediateStop=" + intermediateStop +
                    '}';
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


    public static class TransportType implements Parcelable {
        public String type = "";
        public String name = "";
        public String towards = "";
        public String line;

        public TransportType() { }

        public TransportType(Parcel parcel) {
            type = parcel.readString();
            name = parcel.readString();
            towards = parcel.readString();
            line = parcel.readString();
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
            dest.writeString(line);
        }

        public static TransportType fromJson(JSONObject json) throws JSONException {
            TransportType t = new TransportType();
            if (json.has("name")) {
                t.name = json.getString("name");
            }
            if (json.has("towards")) {
                t.towards = json.getString("towards");
            }
            if (json.has("line")) {
                t.line = json.getString("line");
            }
            t.type = json.getString("type");
            return t;
        }

        public Drawable getDrawable(Context context) {
            return ViewHelper.getDrawableForTransport(context, TransportMode.getIndex(type), name, line);
        }

        public int getColor(final Context context) {
            return ViewHelper.getLineColor(context.getResources(), TransportMode.getIndex(type), line);
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
