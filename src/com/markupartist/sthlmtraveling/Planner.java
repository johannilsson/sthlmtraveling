package com.markupartist.sthlmtraveling;

import static com.markupartist.sthlmtraveling.ApiSettings.STHLM_TRAVELING_API_ENDPOINT;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.xml.sax.InputSource;

import android.util.Log;

public class Planner {
    private static final String TAG = "Planner";
    private static Planner instance = null;
    private StopFinder mStopFinder;
    private RouteFinder mRouteFinder;
    private RouteDetailFinder mRouteDetailFinder;
    private ArrayList<Route> mRoutes = null;
    private ArrayList<String> mRouteDetails = null;

    private Planner() {
        mStopFinder = new StopFinder();
        mRouteFinder = new RouteFinder();
        mRouteDetailFinder = new RouteDetailFinder();
    }

    public ArrayList<String> findStop(String name) {
        ArrayList<String> stops = new ArrayList<String>();
        try {
            URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                    + "?method=findStop&name=" + URLEncoder.encode(name));
            InputSource input = new InputSource(endpoint.openStream());
            stops = mStopFinder.parseStops(input);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return stops;
    }

    public ArrayList<Route> findRoutes(String from, String to) {
        Log.d(TAG, "Searching for from=" + from + ",to=" + to);

        String fromEncoded = URLEncoder.encode(from);
        String toEncoded = URLEncoder.encode(to);

        mRoutes = new ArrayList<Route>();
        try {
            URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                    + "?method=findRoutes&from=" 
                    + fromEncoded + "&to=" + toEncoded);
            InputSource input = new InputSource(endpoint.openStream());
            mRoutes = mRouteFinder.parseRoutes(input);
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
            URL endpoint = new URL(STHLM_TRAVELING_API_ENDPOINT
                    + "?method=routeDetail&ident=" + route.ident 
                    + "&routeId=" + route.routeId);
            mRouteDetails = mRouteDetailFinder.parseDetail(
                    new InputSource(endpoint.openStream()));
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
}
