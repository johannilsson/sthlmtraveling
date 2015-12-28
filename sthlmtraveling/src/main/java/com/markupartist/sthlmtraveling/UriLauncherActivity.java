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

package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.utils.Analytics;

import java.util.ArrayList;
import java.util.Date;


public class UriLauncherActivity extends Activity {
    private static int INDEX_ORIGIN_NAME = 6;
    private static int INDEX_DESTINATION_NAME = 7;
    private static int INDEX_ORIGIN_ID = 8;
    private static int INDEX_ORIGIN_LAT = 8;
    private static int INDEX_ORIGIN_LON = 9;
    private static int INDEX_DESTINATION_ID = 9;
    private static int INDEX_DATE_TIME = 10;
    private static int INDEX_TIME_MODE = 11;
    private static int INDEX_VIA_ID = 14;
    private static int INDEX_TRAFFIC_MODE = 15;
    private static int INDEX_VIA_NAME = 25;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchFromIntent(getIntent());
    }

    protected void launchFromIntent(Intent intent) {
        Uri uri = intent.getData();

        String fullUrl = "";
        if (uri != null) {
            fullUrl = uri.toString();
        }

        try {
            if (fullUrl.contains("sharelink")) {
                handleShareLink(uri);
                return;
            } else if (fullUrl.contains("Travel")) {
                handlePlanner(fullUrl);
                return;
            } else if (fullUrl.contains("Realtime")) {
                handleDepartures(fullUrl);
                return;
            }
        } catch (Exception e) {
            Crashlytics.log("Uri: " + fullUrl);
            Crashlytics.logException(e);
        }
        Intent i = new Intent(this, StartActivity.class);
        startActivity(i);
        finish();
    }

    protected void handleDepartures(String url) {
        String[] parts = TextUtils.split(url, "/");
        Site site = parseSite(parts[5], parts[6], null);
        Intent i = new Intent(this, DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE, site);

        Analytics.getInstance(this).event("Launch", "SL", "Departure");

        startActivity(i);
        finish();
    }

    /**
     * Construct a query from destination position.
     *
     * For this quert we expect origin to contain a site id and the destination to have a location.
     *
     * @param parts
     * @return
     */
    protected JourneyQuery createFromSearchTravelByDestinationPosition(String[] parts) {
        Site origin = parseSite(parts[3], parts[7], null);
        Site destination = parseSite(parts[4], parts[5], parts[6]);

        return new JourneyQuery.Builder()
                .origin(origin)
                .destination(destination)
                .time(parseTime(parts[8])) // 2015-12-28 14_55
//                .via(parseSite(parts[12], parts[12], null))
                .transportModes(parseTransports(parts[13]))
                .isTimeDeparture("depart".equals(parts[9]))
                .create();
    }

    protected JourneyQuery createFromSearchTravelById(String[] parts) {
        Site origin = parseSite(parts[3], parts[5], null);
        Site destination = parseSite(parts[4], parts[6], null);

        return new JourneyQuery.Builder()
                .origin(origin)
                .destination(destination)
                .time(parseTime(parts[7])) // 2015-12-28 14_55
//                .via(parseSite(parts[12], parts[12], null))
                .transportModes(parseTransports(parts[12]))
                .isTimeDeparture("depart".equals(parts[8]))
                .create();
    }

    protected void handleShareLink(Uri uri) {
        String q = uri.getQueryParameter("q");

        String[] parts = TextUtils.split(Uri.decode(q), "/");

        JourneyQuery journeyQuery = null;
        if (q.contains("SearchTravelByDestinationPosition")) {
            journeyQuery = createFromSearchTravelByDestinationPosition(parts);
        } else if (q.contains("SearchTravelById")) {
            journeyQuery = createFromSearchTravelById(parts);
        }


        Intent intent;
        if (journeyQuery != null) {
            intent = new Intent(this, RoutesActivity.class);
            intent.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, journeyQuery);
            Analytics.getInstance(this).event("Launch", "SL", "Planner");
        } else {
            intent = new Intent(this, StartActivity.class);
            Analytics.getInstance(this).event("Launch", "SL", "Home");
        }

        startActivity(intent);
        finish();
    }

    protected void handlePlanner(String url) {
        String[] parts = TextUtils.split(url, "/");
        boolean fromMyLocation = !"SearchTravelById".equals(parts[5]);
        int indexOffset = fromMyLocation ? 1 : 0;

        Site origin;
        Site destination;
        if (fromMyLocation) {
            origin = parseSite(parts[INDEX_ORIGIN_NAME], parts[INDEX_ORIGIN_LAT], parts[INDEX_ORIGIN_LON]);
            destination = parseSite(parts[INDEX_DESTINATION_NAME], parts[INDEX_DESTINATION_ID + indexOffset], null);
        } else {
            origin = parseSite(parts[INDEX_ORIGIN_NAME], parts[INDEX_ORIGIN_ID], null);
            destination = parseSite(parts[INDEX_DESTINATION_NAME], parts[INDEX_DESTINATION_ID], null);
        }

        JourneyQuery q = new JourneyQuery.Builder()
                .origin(origin)
                .destination(destination)
                .time(parseTime(parts[INDEX_DATE_TIME + indexOffset]))
                .via(parseSite(parts[INDEX_VIA_NAME + indexOffset], parts[INDEX_VIA_ID + indexOffset], null))
                .transportModes(parseTransports(parts[INDEX_TRAFFIC_MODE + indexOffset]))
                .isTimeDeparture("depart".equals(parts[INDEX_TIME_MODE + indexOffset]))
                .create();

        Intent i = new Intent(this, RoutesActivity.class);
        i.putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, q);

        Analytics.getInstance(this).event("Launch", "SL", "Planner");

        startActivity(i);
        finish();
    }

    private ArrayList<String> parseTransports(String part) {
        // TODO: Transport mode is not supported atm.
        // 2,8,1,4,96
        return null;
    }

    private Date parseTime(String part) {
        // TODO: Date time is not supported atm.
        if (TextUtils.isEmpty(part) || "null".equals(part)) {
            return null;
        }
        return null;
    }

    protected Site parseSite(String name, String latOrId, String lon) {
        if (TextUtils.isEmpty(name) || "null".equals(name)) {
            return null;
        }

        Site s = new Site();
        s.setName(Uri.decode(name));
        if (TextUtils.isEmpty(lon)) {
            s.setId(Integer.parseInt(latOrId));
        } else {
            s.setLocation(
                    (int) (Double.parseDouble(latOrId) * 1E6),
                    (int) (Double.parseDouble(lon) * 1E6)
            );
        }

        return s;
    }


}
