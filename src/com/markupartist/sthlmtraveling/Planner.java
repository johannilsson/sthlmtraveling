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

package com.markupartist.sthlmtraveling;

import static com.markupartist.sthlmtraveling.ApiSettings.STHLM_TRAVELING_API_ENDPOINT;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.xml.sax.InputSource;

import android.text.format.Time;
import android.util.Log;

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
    private ArrayList<Route> mRoutes = null;
    private ArrayList<String> mRouteDetails = null;

    private Planner() {
        mStopFinder = new StopParser();
        mRouteFinder = new RouteParser();
        mRouteDetailFinder = new RouteDetailParser();
    }

    public ArrayList<String> findStop(String name) {
        ArrayList<String> stops = new ArrayList<String>();
        try {
            InputSource input;
            if (mUseMockData) {
                StringReader sr = new StringReader(mStopsXml);
                input = new InputSource(sr);
            } else {
                URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                        + "?method=findStop&name=" + URLEncoder.encode(name));
                input = new InputSource(endpoint.openStream());
            }

            stops = mStopFinder.parseStops(input);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return stops;
    }

    public ArrayList<Route> findRoutes(String startPoint, String endPoint, 
            Time time) {
        Log.d(TAG, "Searching for startPoint=" + startPoint + ",endPoint=" + endPoint);

        String startPointEncoded = URLEncoder.encode(startPoint);
        String endPointEncoded = URLEncoder.encode(endPoint);
        String timeEncoded = URLEncoder.encode(time.format("%Y-%m-%d %H:%M"));

        mRoutes = new ArrayList<Route>();
        try {
            InputSource input;
            if (mUseMockData) {
                StringReader sr = new StringReader(mRoutesXml);
                input = new InputSource(sr);
            } else {
                URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                        + "?method=findRoutes" 
                        + "&from=" + startPointEncoded 
                        + "&to=" + endPointEncoded
                        + "&time=" + timeEncoded);
                input = new InputSource(endpoint.openStream());
            }
            mRoutes = mRouteFinder.parseRoutes(input);

            mRequestCount = mRouteFinder.getRequestCount();
            mIdent = mRouteFinder.getIdent();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return mRoutes;
    }

    public ArrayList<Route> findEarlierRoutes() {
        if (mIdent == null) {
            Log.e(TAG, "findEarlierRoutes was accessed before findRoutes.");
            throw new IllegalStateException("findRoutes must be run firsts.");
        }

        String ident = URLEncoder.encode(mIdent);

        mRoutes = new ArrayList<Route>();
        try {
            InputSource input;
            if (mUseMockData) {
                StringReader sr = new StringReader(mRoutesXml);
                input = new InputSource(sr);
            } else {
                URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                        + "?method=findEarlierRoutes"
                        + "&requestCount=" + mRequestCount
                        + "&ident=" + ident);
                input = new InputSource(endpoint.openStream());
            }
            mRoutes = mRouteFinder.parseRoutes(input);

            // Update the request count, needed for all request that needs an ident.
            mRequestCount = mRouteFinder.getRequestCount();
            mIdent = mRouteFinder.getIdent();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return mRoutes;
    }

    public ArrayList<Route> findLaterRoutes() {
        if (mIdent == null) {
            Log.e(TAG, "findEarlierRoutes was accessed before findRoutes.");
            throw new IllegalStateException("findRoutes must be run firsts.");
        }

        String ident = URLEncoder.encode(mIdent);

        mRoutes = new ArrayList<Route>();
        try {
            InputSource input;
            if (mUseMockData) {
                StringReader sr = new StringReader(mRoutesXml);
                input = new InputSource(sr);
            } else {
                URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                        + "?method=findLaterRoutes"
                        + "&requestCount=" + mRequestCount
                        + "&ident=" + ident);
                input = new InputSource(endpoint.openStream());
            }

            mRoutes = mRouteFinder.parseRoutes(input);

            mRequestCount = mRouteFinder.getRequestCount();
            mIdent = mRouteFinder.getIdent();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return mRoutes;
    }

    public ArrayList<Route> lastFoundRoutes() {
        return mRoutes;
    }

    public ArrayList<String> findRouteDetails(Route route) {
        mRouteDetails = new ArrayList<String>();
        try {
            InputSource input;
            if (mUseMockData) {
                StringReader sr = new StringReader(mRouteDetailXml);
                input = new InputSource(sr);
            } else {
                URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                        + "?method=routeDetail&ident=" + route.ident 
                        + "&routeId=" + route.routeId
                        + "&requestCount=" + mRequestCount);
                input = new InputSource(endpoint.openStream());
            }
            mRouteDetails = mRouteDetailFinder.parseDetail(input);
            // Update the request count, needed for all request that needs an ident.
            mRequestCount = mRouteDetailFinder.getRequestCount();
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return mRouteDetails;
    }

    public ArrayList<String> lastFoundRouteDetail() {
        return mRouteDetails;
    }

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
}
