package com.markupartist.sthlmtraveling.provider.departure;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
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
        final HttpGet get = new HttpGet(apiEndpoint()
                + "/v1/departures/" + site.getId()
                + "/?key=" + get(KEY)
                + "&timewindow=20");

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
        public ArrayList<MetroDeparture> metros = new ArrayList<MetroDeparture>();
        public ArrayList<BusDeparture> buses = new ArrayList<BusDeparture>();
        public ArrayList<TramDeparture> trams = new ArrayList<TramDeparture>();
        public ArrayList<TrainDeparture> trains = new ArrayList<TrainDeparture>();

        public static Departures fromJson(JSONObject json) throws JSONException {
            Departures d = new Departures();

            JSONArray jsonMetros = json.getJSONArray("metros");
            for (int i = 0; i < jsonMetros.length(); i++) {
                d.metros.add(MetroDeparture.fromJson(jsonMetros.getJSONObject(i)));
            }

            JSONArray jsonBuses = json.getJSONArray("buses");
            for (int i = 0; i < jsonBuses.length(); i++) {
                d.buses.add(BusDeparture.fromJson(jsonBuses.getJSONObject(i)));
            }

            JSONArray jsonTrams = json.getJSONArray("trams");
            for (int i = 0; i < jsonTrams.length(); i++) {
                d.trams.add(TramDeparture.fromJson(jsonTrams.getJSONObject(i)));
            }

            JSONArray jsonTrains = json.getJSONArray("trains");
            for (int i = 0; i < jsonTrains.length(); i++) {
                d.trains.add(TrainDeparture.fromJson(jsonTrains.getJSONObject(i)));
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

            JSONArray jsonGroupOfLines = json.getJSONArray("groupOfLines");
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
            bd.stopAreaName = jsonObject.getString("stopAreaName");
            bd.stopAreaNumber = jsonObject.getString("stopAreaNumber");
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
            td.stopAreaName = jsonObject.getString("stopAreaName");
            td.stopAreaNumber = jsonObject.getString("stopAreaNumber");

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
            td.stopAreaName = jsonObject.getString("stopAreaName");
            td.stopAreaNumber = jsonObject.getString("stopAreaNumber");

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
                gol.direction1.add(DisplayRow.fromJson(jsonDirection1.getJSONObject(i)));
            }
            JSONArray jsonDirection2 = json.getJSONArray("direction2");
            for (int i = 0; i < jsonDirection2.length(); i++) {
                gol.direction2.add(DisplayRow.fromJson(jsonDirection2.getJSONObject(i)));
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
                dr.destination = json.getString("destination");
            }
            if (json.has("lineNumber")) {
                dr.lineNumber = json.getString("lineNumber");
            }
            if (json.has("displayTime")) {
                dr.displayTime = json.getString("displayTime");
            }
            if (json.has("timeTabledDateTime")) {
                dr.timeTabledDateTime = json.getString("timeTabledDateTime");
            }
            if (json.has("expectedDateTime")) {
                dr.expectedDateTime = json.getString("expectedDateTime");
            }
            if (json.has("message")) {
                dr.message = json.getString("message");
            }

            return dr;
        }
    }
}
