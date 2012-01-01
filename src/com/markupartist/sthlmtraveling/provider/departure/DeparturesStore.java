package com.markupartist.sthlmtraveling.provider.departure;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint2;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.HttpManager;
import com.markupartist.sthlmtraveling.utils.StreamUtils;

public class DeparturesStore {
    static String TAG = "DeparturesStore";

    public DeparturesStore() {
    }

    public Departures find(Site site) throws IllegalArgumentException, IOException {
    	if (site == null) {
            Log.w(TAG, "Site is null");
    		throw new IllegalArgumentException(TAG + ", Site is null");
    	}
    	
        Log.d(TAG, "About to get departures for " + site.getName());
        final HttpGet get = new HttpGet(apiEndpoint2()
                + "v1/departures/" + site.getId()
                + "?key=" + get(KEY)
                + "&timewindow=30");

        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Log.w(TAG, "A remote server error occurred when getting departures, status code: " +
                    response.getStatusLine().getStatusCode());
            throw new IOException("A remote server error occurred when getting departures.");
        }

        Departures departures = null;
        HttpEntity entity = response.getEntity();
        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
        case HttpStatus.SC_OK:
            String rawContent = StreamUtils.toString(entity.getContent());

            try {
                departures = Departures.fromJson(new JSONObject(rawContent));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, "Could not parse the reponse...");
                throw new IOException("Could not parse the response.");
            }
            break;
        }

    	return departures;
    }

    public static class Departures {
        public int siteId;
        public ArrayList<String> servesTypes = new ArrayList<String>();
        public ArrayList<MetroDeparture> metros = new ArrayList<MetroDeparture>();
        public ArrayList<BusDeparture> buses = new ArrayList<BusDeparture>();
        public ArrayList<TramDeparture> trams = new ArrayList<TramDeparture>();
        public ArrayList<TrainDeparture> trains = new ArrayList<TrainDeparture>();

        public static Departures fromJson(JSONObject json) throws JSONException {
            Departures d = new Departures();

            if (!json.isNull("serves_types")) {
                JSONArray jsonServesTypes = json.getJSONArray("serves_types");
                for (int i = 0; i < jsonServesTypes.length(); i++) {
                    d.servesTypes.add(jsonServesTypes.getString(i));
                }
            }

            if (!json.isNull("metros")) {
                JSONObject jsonMetros = json.getJSONObject("metros");
                if (jsonMetros.has("group_of_lines")) {
                    d.metros.add(MetroDeparture.fromJson(jsonMetros));
                }
            }

            if (!json.isNull("buses")) {
                JSONArray jsonBuses = json.getJSONArray("buses");
                for (int i = 0; i < jsonBuses.length(); i++) {
                    d.buses.add(BusDeparture.fromJson(jsonBuses.getJSONObject(i)));
                }
            }

            if (!json.isNull("trams")) {
                JSONArray jsonTrams = json.getJSONArray("trams");
                for (int i = 0; i < jsonTrams.length(); i++) {
                    d.trams.add(TramDeparture.fromJson(jsonTrams.getJSONObject(i)));
                }                
            }

            if (!json.isNull("trains")) {
                JSONArray jsonTrains = json.getJSONArray("trains");
                for (int i = 0; i < jsonTrains.length(); i++) {
                    d.trains.add(TrainDeparture.fromJson(jsonTrains.getJSONObject(i)));
                }
            }

            return d;
        }
    }

    public static class Departure {
        public String stopAreaName;
        public String stopAreaNumber;
    }

    public static class MetroDeparture extends Departure {
        public ArrayList<GroupOfLine> groupOfLines = new ArrayList<GroupOfLine>();

        public static MetroDeparture fromJson(JSONObject json) throws JSONException {
            MetroDeparture md = new MetroDeparture();
            JSONArray jsonGroupOfLines = json.getJSONArray("group_of_lines");
            for (int i = 0; i < jsonGroupOfLines.length(); i++) {
                try {
                    md.groupOfLines.add(GroupOfLine.fromJson(jsonGroupOfLines.getJSONObject(i)));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "Failed to parse group of line for metros: " + e.getMessage());
                }
            }
            return md;
        }
    }

    public static class BusDeparture extends Departure {
        public ArrayList<DisplayRow> departures = new ArrayList<DisplayRow>();

        public static BusDeparture fromJson(JSONObject jsonObject) throws JSONException {
            BusDeparture bd = new BusDeparture();
            bd.stopAreaName = jsonObject.getString("stop_area_name");
            bd.stopAreaNumber = jsonObject.getString("stop_area_number");
            JSONArray jsonObjects = jsonObject.getJSONArray("departures");
            for (int i = 0; i < jsonObjects.length(); i++) {
                bd.departures.add(DisplayRow.fromJson(jsonObjects.getJSONObject(i)));
            }
            return bd;
        }
    }

    public static class TramDeparture extends Departure {
        public ArrayList<DisplayRow> direction1 = new ArrayList<DisplayRow>();
        public ArrayList<DisplayRow> direction2 = new ArrayList<DisplayRow>();
        public static TramDeparture fromJson(JSONObject jsonObject) throws JSONException {
            TramDeparture td = new TramDeparture();
            td.stopAreaName = jsonObject.getString("stop_area_name");
            td.stopAreaNumber = jsonObject.getString("stop_area_number");

            JSONArray jsonDirection1 = jsonObject.getJSONArray("direction1");
            for (int i = 0; i < jsonDirection1.length(); i++) {
                td.direction1.add(DisplayRow.fromJson(jsonDirection1.getJSONObject(i)));
            }

            JSONArray jsonDirection2 = jsonObject.getJSONArray("direction2");
            for (int i = 0; i < jsonDirection2.length(); i++) {
                td.direction2.add(DisplayRow.fromJson(jsonDirection2.getJSONObject(i)));
            }

            return td;
        }
    }

    public static class TrainDeparture extends Departure {
        public ArrayList<DisplayRow> direction1 = new ArrayList<DisplayRow>();
        public ArrayList<DisplayRow> direction2 = new ArrayList<DisplayRow>();
        
        public static TrainDeparture fromJson(JSONObject jsonObject) throws JSONException {
            TrainDeparture td = new TrainDeparture();
            td.stopAreaName = jsonObject.getString("stop_area_name");
            td.stopAreaNumber = jsonObject.getString("stop_area_number");

            JSONArray jsonDirection1 = jsonObject.getJSONArray("direction1");
            for (int i = 0; i < jsonDirection1.length(); i++) {
                td.direction1.add(DisplayRow.fromJson(jsonDirection1.getJSONObject(i)));
            }

            JSONArray jsonDirection2 = jsonObject.getJSONArray("direction2");
            for (int i = 0; i < jsonDirection2.length(); i++) {
                td.direction2.add(DisplayRow.fromJson(jsonDirection2.getJSONObject(i)));
            }

            return td;
        }
    }

    public static class GroupOfLine {
        public String name;
        public ArrayList<DisplayRow> direction1 = new ArrayList<DisplayRow>();
        public ArrayList<DisplayRow> direction2 = new ArrayList<DisplayRow>();
        public static GroupOfLine fromJson(JSONObject json)
                throws JSONException {
            GroupOfLine gol = new GroupOfLine();

            gol.name = json.getString("name");

            JSONArray jsonDirection1 = json.getJSONArray("direction1");
            for (int i = 0; i < jsonDirection1.length(); i++) {
                DisplayRow dr = DisplayRow.fromJson(jsonDirection1.getJSONObject(i));
                if (dr.looksValid()) {
                    gol.direction1.add(dr);
                }
            }
            JSONArray jsonDirection2 = json.getJSONArray("direction2");
            for (int i = 0; i < jsonDirection2.length(); i++) {
                DisplayRow dr = DisplayRow.fromJson(jsonDirection2.getJSONObject(i));
                if (dr.looksValid()) {
                    gol.direction2.add(dr);
                }
            }

            return gol;
        }
    }

    public static class DisplayRow {
        public String destination;
        public String lineNumber;
        public String displayTime;
        public String timeTabledDateTime;
        public String expectedDateTime;
        public String message;

        @Override
        public String toString() {
            return "DisplayRow{" +
                    "destination='" + destination + '\'' +
                    ", lineNumber='" + lineNumber + '\'' +
                    ", displayTime='" + displayTime + '\'' +
                    ", timeTabledDateTime='" + timeTabledDateTime + '\'' +
                    ", expectedDateTime='" + expectedDateTime + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }

        public static DisplayRow fromJson(JSONObject json) throws JSONException {
            DisplayRow dr = new DisplayRow();
            if (json.has("destination")) {
                dr.destination = json.isNull("destination") ?
                        null : json.getString("destination");
            }
            if (json.has("line_number")) {
                dr.lineNumber = json.isNull("line_number") ?
                        null : json.getString("line_number");
            }
            if (json.has("display_time")) {
                dr.displayTime = json.isNull("display_time") ?
                        null : json.getString("display_time");
            }
            if (json.has("time_tabled_date_time")) {
                dr.timeTabledDateTime = json.isNull("time_tabled_date_time") ?
                        null : json.getString("time_tabled_date_time");
            }
            if (json.has("expected_date_time")) {
                dr.expectedDateTime = json.isNull("expected_date_time") ?
                        null : json.getString("expected_date_time");
            }
            if (json.has("message")) {
                dr.message = json.isNull("message") ?
                        null : json.getString("message");
            }

            return dr;
        }

        public boolean looksValid() {
            return (!TextUtils.isEmpty(destination)
                    || !TextUtils.isEmpty(message));
        }
    }
}
