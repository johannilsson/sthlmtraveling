/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
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
import static com.markupartist.sthlmtraveling.provider.ApiConf.plannerEndpoint;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
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
    private boolean mUseMockData = false;
    private int mRequestCount;
    private String mIdent;
    private StopParser mStopFinder;
    private RouteParser mRouteFinder;
    private RouteDetailParser mRouteDetailFinder;
    // TODO: We should remove routes and details from here...
    private ArrayList<Route> mRoutes = null;
    private ArrayList<RouteDetail> mRouteDetails = null;

    /**
     * Constructs a new Planner
     */
    private Planner() {
        mStopFinder = new StopParser();
        mRouteFinder = new RouteParser();
        mRouteDetailFinder = new RouteDetailParser();
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

    public Response query(JourneyQuery query) throws IOException {
        Response r = null;

        
        JSONObject jsonQuery = new JSONObject();
        try {
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
            jsonQuery.put("time", query.time.format("%F %R"));
            jsonQuery.put("isTimeDeparture", query.isTimeDeparture);
            
            Log.d(TAG, "JSON: " + jsonQuery.toString());
            
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final HttpPost post = new HttpPost(apiEndpoint()
                + "/journeyplanner/?key=" + get(KEY));
        post.setEntity(new StringEntity(jsonQuery.toString()));

        HttpEntity entity = null;
        final HttpResponse response = HttpManager.execute(post);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("A remote server error occurred when getting deviations.");
        }

        entity = response.getEntity();

        String rawContent = StreamUtils.toString(entity.getContent());
        
        //String rawContent = "{\"previousQuery\":\"\",\"nextQuery\":\"\",\"numberOfTrips\":5,\"trips\":[{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"18:22\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:51\",\"changes\":3,\"duration\":\"1:29\",\"priceInfo\":\"-1\",\"co2\":\"3,6\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"My location\",\"latitude\":59404818,\"longitude\":17632332},\"destination\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"departureDate\":\"04.06.10\",\"departureTime\":\"18:20\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"18:21\",\"transport\":{\"type\":\"Walk\"}},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"departureDate\":\"04.06.10\",\"departureTime\":\"18:22\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"18:49\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 318\",\"towards\":\"Brommaplan (bussbyte)\"}},{\"origin\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"destination\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338621,\"longitude\":17938360},\"departureDate\":\"04.06.10\",\"departureTime\":\"18:50\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:27\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 176\",\"towards\":\"Mörby station\"}},{\"origin\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338289,\"longitude\":17939637},\"destination\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59333048,\"longitude\":18031597},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:31\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:41\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans gröna linje 19\",\"towards\":\"Hagsätra\"}},{\"origin\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59332158,\"longitude\":18027740},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:50\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:51\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 1\",\"towards\":\"Stora Essingen\"}}]},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:27\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:48\",\"changes\":3,\"duration\":\"1:21\",\"priceInfo\":\"-1\",\"co2\":\"3,2\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"My location\",\"latitude\":59404818,\"longitude\":17632332},\"destination\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:25\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:26\",\"transport\":{\"type\":\"Walk\"}},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"destination\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:27\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"19:49\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 317\",\"towards\":\"Brommaplan (bussbyte)\"}},{\"origin\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"destination\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338621,\"longitude\":17938360},\"departureDate\":\"04.06.10\",\"departureTime\":\"19:50\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:27\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 176\",\"towards\":\"Mörby station\"}},{\"origin\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338289,\"longitude\":17939637},\"destination\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59333048,\"longitude\":18031597},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:31\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:41\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans gröna linje 19\",\"towards\":\"Hagsätra\"}},{\"origin\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59332176,\"longitude\":18029610},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:47\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:48\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 4\",\"towards\":\"Gullmarsplan\"}}]},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:55\",\"changes\":3,\"duration\":\"1:31\",\"priceInfo\":\"-1\",\"co2\":\"3,6\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"My location\",\"latitude\":59404818,\"longitude\":17632332},\"destination\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:22\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:23\",\"transport\":{\"type\":\"Walk\"}},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"20:51\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 318\",\"towards\":\"Brommaplan (bussbyte)\"}},{\"origin\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"destination\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338621,\"longitude\":17938360},\"departureDate\":\"04.06.10\",\"departureTime\":\"20:51\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:27\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 176\",\"towards\":\"Mörby station\"}},{\"origin\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338289,\"longitude\":17939637},\"destination\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59333048,\"longitude\":18031597},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:34\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:44\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans gröna linje 19\",\"towards\":\"Hagsätra\"}},{\"origin\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59332176,\"longitude\":18029610},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:54\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:55\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 40\",\"towards\":\"Reimersholme\"}}]},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:29\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"22:54\",\"changes\":3,\"duration\":\"1:25\",\"priceInfo\":\"-1\",\"co2\":\"3,2\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"My location\",\"latitude\":59404818,\"longitude\":17632332},\"destination\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:27\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:28\",\"transport\":{\"type\":\"Walk\"}},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404701,\"longitude\":17632296},\"destination\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:29\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"21:51\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 317\",\"towards\":\"Brommaplan (bussbyte)\"}},{\"origin\":{\"id\":0,\"name\":\"Solbacka\",\"latitude\":59340698,\"longitude\":17695409},\"destination\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338621,\"longitude\":17938360},\"departureDate\":\"04.06.10\",\"departureTime\":\"21:51\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"22:27\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 176\",\"towards\":\"Mörby station\"}},{\"origin\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338289,\"longitude\":17939637},\"destination\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59333048,\"longitude\":18031597},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:34\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"22:44\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans gröna linje 19\",\"towards\":\"Hagsätra\"}},{\"origin\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59332176,\"longitude\":18029610},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:53\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"22:54\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 40\",\"towards\":\"Reimersholme\"}}]},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:54\",\"changes\":2,\"duration\":\"1:30\",\"priceInfo\":\"-1\",\"co2\":\"3,6\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"My location\",\"latitude\":59404818,\"longitude\":17632332},\"destination\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:22\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"22:23\",\"transport\":{\"type\":\"Walk\"}},{\"origin\":{\"id\":0,\"name\":\"Kungsberga konsum\",\"latitude\":59404827,\"longitude\":17632341},\"destination\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338208,\"longitude\":17938109},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:27\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 318\",\"towards\":\"Brommaplan\"}},{\"origin\":{\"id\":0,\"name\":\"Brommaplan\",\"latitude\":59338289,\"longitude\":17939637},\"destination\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59333048,\"longitude\":18031597},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:34\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:44\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans gröna linje 19\",\"towards\":\"Hagsätra\"}},{\"origin\":{\"id\":0,\"name\":\"Fridhemsplan\",\"latitude\":59332176,\"longitude\":18029610},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331106,\"longitude\":18023560},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:53\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:54\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 40\",\"towards\":\"Reimersholme\"}}]}]}";
        // Telefonplan - Mariebergsgatan
        //String rawContent = "{\"previousQuery\":\"\",\"nextQuery\":\"\",\"numberOfTrips\":5,\"trips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:54\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:19\",\"changes\":1,\"duration\":\"0:25\",\"priceInfo\":\"1\",\"co2\":\"0,02\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59315959,\"longitude\":18035543},\"departureDate\":\"04.06.10\",\"departureTime\":\"22:54\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:00\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans röda linje 14\",\"towards\":\"Mörby centrum\"}},{\"origin\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59316031,\"longitude\":18033808},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:14\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:19\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 4\",\"towards\":\"Radiohuset\"}}]},{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:09\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:29\",\"changes\":1,\"duration\":\"0:20\",\"priceInfo\":\"1\",\"co2\":\"0,02\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59315959,\"longitude\":18035543},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:09\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:15\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans röda linje 14\",\"towards\":\"Mörby centrum\"}},{\"origin\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59316031,\"longitude\":18033808},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:29\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 4\",\"towards\":\"Radiohuset\"}}]},{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:49\",\"changes\":1,\"duration\":\"0:25\",\"priceInfo\":\"1\",\"co2\":\"0,02\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59315959,\"longitude\":18035543},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:24\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:30\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans röda linje 14\",\"towards\":\"Mörby centrum\"}},{\"origin\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59316031,\"longitude\":18033808},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:44\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:49\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 4\",\"towards\":\"Radiohuset\"}}]},{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:39\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:59\",\"changes\":1,\"duration\":\"0:20\",\"priceInfo\":\"1\",\"co2\":\"0,02\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59315959,\"longitude\":18035543},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:39\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:45\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans röda linje 14\",\"towards\":\"Mörby centrum\"}},{\"origin\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59316031,\"longitude\":18033808},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:54\",\"arrivalDate\":\"04.06.10\",\"arrivalTime\":\"23:59\",\"transport\":{\"type\":\"BUS\",\"name\":\"blåbuss 4\",\"towards\":\"Radiohuset\"}}]},{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:54\",\"arrivalDate\":\"05.06.10\",\"arrivalTime\":\"00:15\",\"changes\":1,\"duration\":\"0:21\",\"priceInfo\":\"1\",\"co2\":\"0,02\",\"mt6MessageExist\":false,\"rtuMessageExist\":false,\"remarksMessageExist\":false,\"subTrips\":[{\"origin\":{\"id\":0,\"name\":\"Telefonplan\",\"latitude\":59298251,\"longitude\":17997321},\"destination\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59315959,\"longitude\":18035543},\"departureDate\":\"04.06.10\",\"departureTime\":\"23:54\",\"arrivalDate\":\"05.06.10\",\"arrivalTime\":\"00:00\",\"transport\":{\"type\":\"MET\",\"name\":\"tunnelbanans röda linje 14\",\"towards\":\"Mörby centrum\"}},{\"origin\":{\"id\":0,\"name\":\"Hornstull\",\"latitude\":59316031,\"longitude\":18033808},\"destination\":{\"id\":0,\"name\":\"Mariebergsgatan\",\"latitude\":59331735,\"longitude\":18024918},\"departureDate\":\"05.06.10\",\"departureTime\":\"00:10\",\"arrivalDate\":\"05.06.10\",\"arrivalTime\":\"00:15\",\"transport\":{\"type\":\"BUS\",\"name\":\"buss 40\",\"towards\":\"Fridhemsplan\"}}]}]}";
        
        try {
            r = Response.fromJson(new JSONObject(rawContent));            
            Log.d(TAG, "The response: " + r.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return r;
    }

    /**
     * Find routes.
     * @param trip the {@link Trip}
     * @return a {@link Trip}
     * @throws IOException on network problems
     */
    public Trip findRoutes(Trip trip, String languageCode)
            throws IOException {
        Log.d(TAG, "Searching for startPoint=" + trip.getStartPoint() + ",endPoint=" + trip.getEndPoint());

        Stop startPoint = trip.getStartPoint();
        Stop endPoint = trip.getEndPoint();

        String startPointPositionPart = "";
        if (startPoint.getLocation() != null) {
            startPointPositionPart = String.format("&fromLat=%s&fromLng=%s",
                    startPoint.getLocation().getLatitude(),
                    startPoint.getLocation().getLongitude());
        }
        String endPointPositionPart = "";
        if (endPoint.getLocation() != null) {
            endPointPositionPart = String.format("&toLat=%s&toLng=%s",
                    endPoint.getLocation().getLatitude(),
                    endPoint.getLocation().getLongitude());
        }

        String startPointEncoded;
        if (startPoint.getSiteId() > 0) {
            startPointEncoded = Integer.toString(startPoint.getSiteId());
        } else {
            startPointEncoded = URLEncoder.encode(startPoint.getName());
        }
        String endPointEncoded;
        if (endPoint.getSiteId() > 0) {
            endPointEncoded = Integer.toString(endPoint.getSiteId());
        } else {
            endPointEncoded = URLEncoder.encode(endPoint.getName());
        }

        String timeEncoded = URLEncoder.encode(trip.getTime().format("%Y-%m-%d %H:%M"));
        int isDeparture = trip.isTimeDeparture() ? 1 : 0;

        InputSource input;
        if (mUseMockData) {
            StringReader sr = new StringReader(mRoutesXml);
            input = new InputSource(sr);
        } else {
            final HttpGet get = new HttpGet(plannerEndpoint()
                    + "?method=findRoutes" 
                    + "&from=" + startPointEncoded 
                    + "&to=" + endPointEncoded
                    + "&time=" + timeEncoded
                    + "&isTimeDeparture=" + isDeparture
                    + "&langCode=" + languageCode
                    + startPointPositionPart
                    + endPointPositionPart);
            HttpEntity entity = null;
            final HttpResponse response = HttpManager.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("A remote server error occurred when getting routes.");
            }
            entity = response.getEntity();
            input = new InputSource(entity.getContent());
        }

        mRoutes = mRouteFinder.parseRoutes(input);
        mRequestCount = mRouteFinder.getRequestCount();
        mIdent = mRouteFinder.getIdent();

        /*
         * If the result is empty, see if the user just misspelled the start or
         * end point to see if we can find some suggestions.
         */
        if (mRoutes.isEmpty()) {
            if (!startPoint.isMyLocation()
                    && (startPoint.getLocation() == null 
                    && startPoint.getSiteId() == 0)) {
                trip.setStartPointAlternatives(
                        SitesStore.getInstance().getSite(startPoint.getName()));
            } else {
                trip.setStartPointAlternatives(null);
            }
            if (!endPoint.isMyLocation()
                    && (endPoint.getLocation() == null
                    && endPoint.getSiteId() == 0)) {
                trip.setEndPointAlternatives(
                        SitesStore.getInstance().getSite(endPoint.getName()));
            } else {
                trip.setEndPointAlternatives(null);
            }

            /*
             * Since sl.se can't distinguish between some names, check if the
             * provided names match the name in the alternative lists. If they
             * match we query again but with the site id. 
             */
            if (trip.hasAlternatives()) {
                boolean shouldResend = false;
                for (Site site : trip.getStartPointAlternatives()) {
                    if (site.getName().equals(startPoint.getName())) {
                        startPoint.setSiteId(site.getId());
                        shouldResend = true;
                        break;
                    }
                }
                for (Site site : trip.getEndPointAlternatives()) {
                    if (site.getName().equals(endPoint.getName())) {
                        endPoint.setSiteId(site.getId());
                        shouldResend = true;
                    }
                }
                if (shouldResend)
                    return findRoutes(trip, languageCode);
            }
        }

        trip.setRoutes(mRoutes);

        return trip;
    }

    /**
     * Find earlier routes, requires a call to findRoutes before this can be called.
     * @return a list of RouteS
     * @throws IOException on network problems
     * @throws IllegalStateException if called before findRoutes
     */
    public ArrayList<Route> findEarlierRoutes(String languageCode) throws IOException {
        if (mIdent == null) {
            Log.e(TAG, "findEarlierRoutes was accessed before findRoutes.");
            throw new IllegalStateException("findRoutes must be run first.");
        }

        String ident = URLEncoder.encode(mIdent);

        InputSource input;
        if (mUseMockData) {
            StringReader sr = new StringReader(mRoutesXml);
            input = new InputSource(sr);
        } else {
            final HttpGet get = new HttpGet(plannerEndpoint()
                    + "?method=findEarlierRoutes"
                    + "&requestCount=" + mRequestCount
                    + "&ident=" + ident
                    + "&langCode=" + languageCode);
            HttpEntity entity = null;
            final HttpResponse response = HttpManager.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("A remote server error occurred when getting earlier routes.");
            }
            entity = response.getEntity();
            input = new InputSource(entity.getContent());
        }

        mRoutes = mRouteFinder.parseRoutes(input);
        mRequestCount = mRouteFinder.getRequestCount();
        mIdent = mRouteFinder.getIdent();

        return mRoutes;
    }

    /**
     * Find later routes, requires a call to findRoutes before this can be called.
     * @return a list of RouteS
     * @throws IOException on network problems
     * @throws IllegalStateException if called before findRoutes
     */
    public ArrayList<Route> findLaterRoutes(String languageCode) throws IOException {
        if (mIdent == null) {
            Log.e(TAG, "findEarlierRoutes was accessed before findRoutes.");
            throw new IllegalStateException("findRoutes must be run first.");
        }

        String ident = URLEncoder.encode(mIdent);

        InputSource input;
        if (mUseMockData) {
            StringReader sr = new StringReader(mRoutesXml);
            input = new InputSource(sr);
        } else {
            final HttpGet get = new HttpGet(plannerEndpoint()
                    + "?method=findLaterRoutes"
                    + "&requestCount=" + mRequestCount
                    + "&ident=" + ident
                    + "&langCode=" + languageCode);
            HttpEntity entity = null;
            final HttpResponse response = HttpManager.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("A remote server error occurred when getting later routes.");
            }
            entity = response.getEntity();
            input = new InputSource(entity.getContent());
        }

        mRoutes = mRouteFinder.parseRoutes(input);
        mRequestCount = mRouteFinder.getRequestCount();
        mIdent = mRouteFinder.getIdent();

        return mRoutes;
    }

    /**
     * Find route details
     * @param route the Route to find details for
     * @return a list of details, each string in the result represent a detail for each change
     * @throws IOException on network problems
     */
    public ArrayList<RouteDetail> findRouteDetails(Route route, String languageCode)
            throws IOException {
        mRouteDetails = new ArrayList<RouteDetail>();

        InputSource input;
        if (mUseMockData) {
            StringReader sr = new StringReader(mRouteDetailXml);
            input = new InputSource(sr);
        } else {
            final HttpGet get = new HttpGet(plannerEndpoint()
                    + "?method=routeDetail&ident=" + route.ident 
                    + "&routeId=" + route.routeId
                    + "&requestCount=" + mRequestCount
                    + "&langCode=" + languageCode);
            HttpEntity entity = null;
            final HttpResponse response = HttpManager.execute(get);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("A remote server error occurred when getting route details.");
            }
            entity = response.getEntity();
            input = new InputSource(entity.getContent());
        }

        mRouteDetails = mRouteDetailFinder.parseDetail(input);
        mRequestCount = mRouteDetailFinder.getRequestCount();

        return mRouteDetails;
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

    /**
     * Mocked routes xml
     */
    private static final String mRoutesXml = "<findRoutes generator='zend' version='1.0'><requestCount>1</requestCount><ident>54.010259213.1248185539</ident><routes><key_0><routeId>C0-0</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:36</departure><arrival>20:03</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_0><key_1><routeId>C0-1</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:46</departure><arrival>20:13</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_1><key_2><routeId>C0-2</routeId><from>Centralen (Klarabergsviad.)</from><to>Tensta</to><departure>approx. 19:56</departure><arrival>20:23</arrival><duration>0:27</duration><changes>1</changes><by><key_0>Bus -</key_0><key_1>Metro blue line 10</key_1></by></key_2></routes><status>success</status></findRoutes>";

    /**
     * Mocked stops xml
     */
    private static final String mStopsXml = "<findStop generator='zend' version='1.0'><key_0>Telefonplan (Stockholm)</key_0><key_1>Telegramvägen (Nacka)</key_1><key_2>Telemarksgränd (Stockholm)</key_2><key_3>Tellusborgsvägen (Stockholm)</key_3><key_4>TEL</key_4><key_5>Telia (Stockholm)</key_5><key_6>Tellusvägen (Järfälla)</key_6><key_7>Tellusgatan (Sigtuna)</key_7><key_8>Telgebo (Södertälje)</key_8><status>success</status></findStop>";

    /**
     * Mocked route detail xml
     */
    private static final String mRouteDetailXml = "<routeDetail generator='zend' version='1.0'><requestCount>3</requestCount><details><key_0>Take Bus - from Centralen (Klarabergsviad.) towards Rådhuset.Your departure from Centralen (Klarabergsviad.) is at approx. 19:36, your arrival in Rådhuset is at 19:40.</key_0><key_1>At Rådhuset change to Metro blue line 10 towards Hjulsta.Your departure from Rådhuset is at 19:45.You arrive in Tensta at 20:03.</key_1><key_2>The duration of your journey is 27 minutes.</key_2><key_3>Have a nice journey!</key_3></details><status>success</status></routeDetail>";
    
    
    public static class Response implements Parcelable {
        // TODO: Parse out the ident.
        public String previousQuery;
        public String nextQuery;
        public int numberOfTrips;
        public ArrayList<Trip2> trips = new ArrayList<Trip2>();

        public Response() {}

        public Response(Parcel parcel) {
            trips = new ArrayList<Trip2>();
            parcel.readTypedList(trips, Trip2.CREATOR);
        }

        @Override
        public String toString() {
            return "Response{" +
                    "previousQuery='" + previousQuery + '\'' +
                    ", nextQuery='" + nextQuery + '\'' +
                    ", numberOfTrips=" + numberOfTrips +
                    ", trips=" + trips +
                    '}';
        }

        public static Response fromJson(JSONObject json) throws JSONException {
            Response r = new Response();
            
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

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(trips);
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
        public String priceInfo;
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
            priceInfo = parcel.readString();
            co2 = parcel.readString();
            mt6MessageExist = (parcel.readInt() == 1) ? true : false;
            rtuMessageExist = (parcel.readInt() == 1) ? true : false;
            remarksMessageExist = (parcel.readInt() == 1) ? true : false;
            subTrips = new ArrayList<SubTrip>();
            parcel.readTypedList(subTrips, SubTrip.CREATOR);
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
            dest.writeString(priceInfo);
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
            trip.priceInfo = json.getString("priceInfo");
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
                    ", priceInfo='" + priceInfo + '\'' +
                    ", co2='" + co2 + '\'' +
                    ", mt6MessageExist=" + mt6MessageExist +
                    ", rtuMessageExist=" + rtuMessageExist +
                    ", remarksMessageExist=" + remarksMessageExist +
                    ", subTrips=" + subTrips +
                    '}';
        }

        public String toText() {
            return departureTime + " - " + arrivalTime + " (" + duration + ")";
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

        public SubTrip() {}

        public SubTrip(Parcel parcel) {
            origin = parcel.readParcelable(Location.class.getClassLoader());
            destination = parcel.readParcelable(Location.class.getClassLoader());
            departureDate = parcel.readString();
            departureTime = parcel.readString();
            arrivalDate = parcel.readString();
            arrivalTime = parcel.readString();
            transport = parcel.readParcelable(TransportType.class.getClassLoader());
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

            return st;
        }
        
        @Override
        public String toString() {
            return "SubTrip{" +
                    "origin=" + origin +
                    ", destination=" + destination +
                    ", departureDate='" + departureDate + '\'' +
                    ", departureTime='" + departureTime + '\'' +
                    ", arrivalDate='" + arrivalDate + '\'' +
                    ", arrivalTime='" + arrivalTime + '\'' +
                    ", transport=" + transport +
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
            if ("BUS".equals(type)) {
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
            } else if ("SHP".equals(type)) {
                return R.drawable.transport_boat;
            }

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
