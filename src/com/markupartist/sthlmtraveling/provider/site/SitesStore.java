package com.markupartist.sthlmtraveling.provider.site;

import static com.markupartist.sthlmtraveling.provider.ApiConf.KEY;
import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint2;
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
import android.util.Log;

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
        return getSiteV2(name);
    }

    public ArrayList<Site> getSiteV2(String name) throws IOException {
        final HttpGet get = new HttpGet(apiEndpoint2() + "v1/site/"
                + "?q=" + URLEncoder.encode(name));
        get.addHeader("X-STHLMTraveling-API-Key", get(KEY));
        HttpEntity entity = null;

        final HttpResponse response = HttpManager.execute(get);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting sites.");
        }

        entity = response.getEntity();
        String rawContent = StreamUtils.toString(entity.getContent());
        ArrayList<Site> sites = new ArrayList<Site>();
        try {
            JSONObject jsonResponse = new JSONObject(rawContent);
            if (!jsonResponse.has("sites")) {
                throw new IOException("Invalid input.");
            }
            JSONArray jsonSites = jsonResponse.getJSONArray("sites");
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

    /**
     * Find nearby {@link Site}s.
     * 
     * @param location The location.
     * @return A list of {@link Site}s.
     * @throws IOException If failed to communicate with headend or if we can
     * not parse the response.
     */
    public ArrayList<Site> nearby(Location location) throws IOException {
        final HttpGet get = new HttpGet(apiEndpoint2() + "semistatic/site/near/"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&max_distance=0.8"
                + "&max_results=20");
        get.addHeader("X-STHLMTraveling-API-Key", get(KEY));
        HttpEntity entity = null;
        HttpResponse response;
        try {
            response = HttpManager.execute(get);
        } catch (Exception e) {
            response = HttpManager.execute(get);
        }         

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            Log.w("SiteStore", "Expected 200, got " + response.getStatusLine().getStatusCode());
            throw new IOException("A remote server error occurred when getting sites.");
        }

        entity = response.getEntity();
        String rawContent = StreamUtils.toString(entity.getContent());
        ArrayList<Site> stopPoints = new ArrayList<Site>();
        try {
            JSONObject jsonSites = new JSONObject(rawContent);
            if (jsonSites.has("sites")) {
                JSONArray jsonSitesArray = jsonSites.getJSONArray("sites");
                for (int i = 0; i < jsonSitesArray.length(); i++) {
                    try {
                        JSONObject jsonStop = jsonSitesArray.getJSONObject(i);

                        Site site = new Site();
                        site.setName(jsonStop.getString("name"));
                        site.setId(jsonStop.getInt("site_id"));

                        stopPoints.add(site);
                    } catch (JSONException e) {
                        // Ignore errors here.
                    }
                }
            } else {
                throw new IOException("Sites is not present in response.");
            }
        } catch (JSONException e) {
            throw new IOException("Invalid response.");
        }

        return stopPoints;
    }

}
