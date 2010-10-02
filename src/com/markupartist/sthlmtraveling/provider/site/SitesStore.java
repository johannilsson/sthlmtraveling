package com.markupartist.sthlmtraveling.provider.site;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint;
import static com.markupartist.sthlmtraveling.provider.ApiConf.get;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;

import com.markupartist.sthlmtraveling.utils.HttpManager;
import com.markupartist.sthlmtraveling.utils.StreamUtils;

public class SitesStore {
    private static SitesStore sInstance;

    private SitesStore() {
    }

    public static SitesStore getInstance() {
        if (sInstance == null) {
            sInstance = new SitesStore();
        }
        return sInstance;
    }

    public ArrayList<Site> getSite(String name) throws IOException {
        /*
        ArrayList<Site> sites = new ArrayList<Site>();

        final HttpGet get = new HttpGet(apiEndpoint() + "/sites/"
                + "?q=" + URLEncoder.encode(name)
                + "&key=" + get(KEY));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting sites.");
        }

        entity = response.getEntity();
        SiteParser.parseResponse(entity.getContent(), sites);
        */

        ArrayList<Site> sites = getSiteV2(name);

        return sites;
    }

    public ArrayList<Site> getSiteV2(String name) throws IOException {
        final HttpGet get = new HttpGet(apiEndpoint() + "/site/"
                + "?q=" + URLEncoder.encode(name)
                + "&key=" + get(KEY));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting sites.");
        }

        entity = response.getEntity();
        String rawContent = StreamUtils.toString(entity.getContent());
        ArrayList<Site> sites = new ArrayList<Site>();
        try {
            JSONArray jsonSites = new JSONArray(rawContent);
            for (int i = 0; i < jsonSites.length(); i++) {
                try {
                    sites.add(Site.fromJson(jsonSites.getJSONObject(i)));
                } catch (JSONException e) {
                    // Ignore errors here.
                }
            }
        } catch (JSONException e) {
            throw new IOException("Invalid input.");
        }

        return sites;
    }

    public ArrayList<StopPoint> nearby(Location location) throws IOException {
        final HttpGet get = new HttpGet(apiEndpoint() + "/stoppoint/"
                + "?lat=" + location.getLatitude()
                + "&lon=" + location.getLongitude()
                + "&maxDistance=1000"
                + "&maxResults=20"
                + "&key=" + get(KEY));

        HttpEntity entity = null;
        HttpResponse response;
        try {
            response = HttpManager.execute(get);
        } catch (Exception e) {
            response = HttpManager.execute(get);
        }
         

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting sites.");
        }

        entity = response.getEntity();
        String rawContent = StreamUtils.toString(entity.getContent());
        ArrayList<StopPoint> stopPoints = new ArrayList<StopPoint>();
        try {
            JSONArray jsonStops = new JSONArray(rawContent);
            for (int i = 0; i < jsonStops.length(); i++) {
                try {
                    JSONObject jsonStop = jsonStops.getJSONObject(i);
                    StopPoint stopPoint = new StopPoint();
                    stopPoint.site = Site.fromJson(jsonStop.getJSONObject("site"));
                    stopPoint.name = jsonStop.getString("name");
                    stopPoint.distance = jsonStop.getInt("distance");

                    stopPoints.add(stopPoint);
                } catch (JSONException e) {
                    // Ignore errors here.
                }
            }
        } catch (JSONException e) {
            throw new IOException("Invalid input.");
        }

        return stopPoints;
    }
    
    /*
    private static class SiteParser {
        public static void parseResponse(InputStream in, ArrayList<Site> sites) throws IOException {
            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new InputStreamReader(in));

                int eventType = parser.getEventType();
                boolean inNumber = false;
                int number = 0;
                String name = "";
                boolean inName = false;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if ("Name".equals(parser.getName())) {
                            inName = true;
                        } else if ("Number".equals(parser.getName())) {
                            inNumber = true;
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if ("Name".equals(parser.getName())) {
                            inName = false;
                        } else if ("Number".equals(parser.getName())) {
                            inNumber = false;
                        } else if ("Site".equals(parser.getName())) {
                            Site site = new Site();
                            site.setId(number);
                            site.setName(name);
                            sites.add(site);
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (inNumber) {
                            number = Integer.parseInt(parser.getText());
                        } else if (inName) {
                            name = parser.getText();
                        }
                    }
                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    }
    */
}
