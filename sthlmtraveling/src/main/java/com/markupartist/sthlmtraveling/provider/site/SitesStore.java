package com.markupartist.sthlmtraveling.provider.site;

import android.content.Context;
import android.location.Location;
import androidx.core.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.markupartist.sthlmtraveling.data.misc.HttpHelper;
import com.markupartist.sthlmtraveling.utils.LocationUtils;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.markupartist.sthlmtraveling.provider.ApiConf.apiEndpoint2;

public class SitesStore {
    private static Pattern SITE_NAME_PATTERN = Pattern.compile("([\\w\\s0-9-& ]+)");

    private static SitesStore sInstance;

    private SitesStore() {
    }

    public static SitesStore getInstance() {
        if (sInstance == null) {
            sInstance = new SitesStore();
        }
        return sInstance;
    }

    public ArrayList<Site> getSite(final Context context, final String name) throws IOException {
        return getSiteV2(context, name);
    }

    public ArrayList<Site> getSiteV2(final Context context, final String name) throws IOException {
        return getSiteV2(context, name, true);
    }

    public ArrayList<Site> getSiteV2(final Context context, final String name, final boolean onlyStations) throws IOException {
        if (TextUtils.isEmpty(name)) {
            return new ArrayList<>();
        }

        HttpHelper httpHelper = HttpHelper.getInstance(context);
        String onlyStationsParam = onlyStations ? "true" : "false";
        String url = apiEndpoint2() + "v1/site/"
                + "?q=" + URLEncoder.encode(name, "UTF-8")
                + "&onlyStations=" + onlyStationsParam
                + "&addLocation=true";
        Response response = httpHelper.getClient().newCall(
                httpHelper.createRequest(url)).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Server error while fetching sites");
        }

        ArrayList<Site> sites = new ArrayList<Site>();
        try {
            JSONObject jsonResponse = new JSONObject(response.body().string());
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
    public ArrayList<Site> nearby(Context context, Location location) throws IOException {
        String endpoint = apiEndpoint2() + "semistatic/site/near/"
                + "?latitude=" + location.getLatitude()
                + "&longitude=" + location.getLongitude()
                + "&max_distance=0.8"
                + "&max_results=20";

        HttpHelper httpHelper = HttpHelper.getInstance(context);
        Response response = httpHelper.getClient().newCall(
                httpHelper.createRequest(endpoint)).execute();

        if (!response.isSuccessful()) {
            Log.w("SiteStore", "Expected 200, got " + response.code());
            throw new IOException("A remote server error occurred when getting sites.");
        }

        String rawContent = response.body().string();
        ArrayList<Site> stopPoints = new ArrayList<Site>();
        try {
            JSONObject jsonSites = new JSONObject(rawContent);
            if (jsonSites.has("sites")) {
                JSONArray jsonSitesArray = jsonSites.getJSONArray("sites");
                for (int i = 0; i < jsonSitesArray.length(); i++) {
                    try {
                        JSONObject jsonStop = jsonSitesArray.getJSONObject(i);

                        Site site = new Site();
                        Pair<String, String> nameAndLocality =
                                nameAsNameAndLocality(jsonStop.getString("name"));
                        site.setName(nameAndLocality.first);
                        site.setLocality(nameAndLocality.second);
                        site.setId(jsonStop.getInt("site_id"));
                        site.setSource(Site.SOURCE_STHLM_TRAVELING);
                        site.setType(Site.TYPE_TRANSIT_STOP);
                        String locationData = jsonStop.optString("location");
                        if(locationData != null) {
                            site.setLocation(LocationUtils.parseLocation(locationData));
                        }

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

    public static Pair<String, String> nameAsNameAndLocality(String name) {
        Matcher m = SITE_NAME_PATTERN.matcher(name);
        String title = name;
        String subtitle = null;
        if (m.find()) {
            if (m.groupCount() > 0) {
                title = m.group(1).trim();
            }
            if (m.find()) {
                if (m.groupCount() > 0) {
                    subtitle = m.group(1).trim();
                }
            }
        }
        return Pair.create(title, subtitle);
    }
}
