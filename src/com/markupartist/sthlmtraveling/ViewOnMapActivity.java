/*
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.markupartist.sthlmtraveling.graphics.FixedMyLocationOverlay;
import com.markupartist.sthlmtraveling.graphics.SimpleItemizedOverlay;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Planner.IntermediateStop;
import com.markupartist.sthlmtraveling.provider.planner.Planner.SubTrip;
import com.markupartist.sthlmtraveling.provider.planner.Planner.Trip2;

public class ViewOnMapActivity extends BaseMapActivity {
    private static final String TAG = "ViewOnMapActivity";

    public static String EXTRA_LOCATION = "com.markupartist.sthlmtraveling.extra.Location";
    public static String EXTRA_JOURNEY_QUERY = "com.markupartist.sthlmtraveling.extra.JourneyQuery";
    public static String EXTRA_TRIP = "com.markupartist.sthlmtraveling.extra.Trip";
    

    private MapView mMapView;
    private MapController mapController;
    private MyLocationOverlay mMyLocationOverlay;
    private ShapeDrawable mDefaultMarker;
    private SimpleItemizedOverlay mItemizedOverlay;
    private ActionBar mActionBar;

    private GeoPoint mFocusedGeoPoint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.point_on_map);

        registerEvent("View on map");

        mActionBar = getSupportActionBar();
        mActionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        mActionBar.setTitle(R.string.stop_label);

        Bundle extras = getIntent().getExtras();

        final Trip2 trip =
                (Trip2) extras.getParcelable(EXTRA_TRIP);
        final JourneyQuery journeyQuery =
                (JourneyQuery) extras.getParcelable(EXTRA_JOURNEY_QUERY);
        final Planner.Location focusedLocation =
                (Planner.Location) extras.getParcelable(EXTRA_LOCATION);

        mFocusedGeoPoint = new GeoPoint(
                focusedLocation.latitude, focusedLocation.longitude);

        mMapView = (MapView) findViewById(R.id.mapview);
        //mMapView.setBuiltInZoomControls(true);
        mapController = mMapView.getController();

        // We draw our marker our self later on, fake for the first overlay.
        mDefaultMarker = new ShapeDrawable(new OvalShape());
        mDefaultMarker.getPaint().setColor(Color.TRANSPARENT);

        myLocationOverlay();

        mapController.setZoom(16);
        mapController.animateTo(mFocusedGeoPoint); 

        mItemizedOverlay = new SimpleItemizedOverlay(mDefaultMarker, mMapView);
        //mItemizedOverlay.addOverlay(new OverlayItem(mFocusedGeoPoint, "First", "Desc"));

        fetchRoute(trip, journeyQuery);
    }

    public void fetchRoute(final Trip2 trip, final JourneyQuery journeyQuery) {
        //mActionBar.setProgressBarVisibility(View.VISIBLE);
        setSupportProgressBarIndeterminateVisibility(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Planner.getInstance().addIntermediateStops(
                            trip, journeyQuery);
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // In the UI
                        //mActionBar.setProgressBarVisibility(View.GONE);
                        setSupportProgressBarIndeterminateVisibility(false);
                        addRoute(trip);
                    }
                });
            }
        }).start();
    }

    public void addRoute(Trip2 trip) {
        List<Overlay> mapOverlays = mMapView.getOverlays();

        RouteOverlay routeOverlay = new RouteOverlay(trip);
        mapOverlays.add(routeOverlay);
        mMapView.invalidate();

        for (SubTrip subTrip : trip.subTrips) {
            GeoPoint origin = new GeoPoint(subTrip.origin.latitude,
                    subTrip.origin.longitude);
            routeOverlay.addGeoPoint(origin);
            
            
            OverlayItem originOverlayItem = new OverlayItem(
                    origin, getLocationName(subTrip.origin), 
                    getRouteDescription(subTrip));
            mItemizedOverlay.addOverlay(originOverlayItem);
            if (origin.equals(mFocusedGeoPoint)) {
                mItemizedOverlay.setFocus(originOverlayItem);
            }

            for (IntermediateStop stop : subTrip.intermediateStop) {
                GeoPoint intermediateStop = new GeoPoint(stop.location.latitude,
                        stop.location.longitude);
                routeOverlay.addGeoPoint(intermediateStop);

                mItemizedOverlay.addOverlay(new OverlayItem(
                        intermediateStop, getLocationName(stop.location), 
                        String.format("%s", stop.arrivalTime)));
            }

            GeoPoint destination = new GeoPoint(subTrip.destination.latitude,
                    subTrip.destination.longitude);
            routeOverlay.addGeoPoint(destination);
            
            OverlayItem destinationOverlayItem = new OverlayItem(
                    destination, getLocationName(subTrip.destination), 
                    String.format("%s", subTrip.arrivalTime));
            mItemizedOverlay.addOverlay(destinationOverlayItem);
            if (destination.equals(mFocusedGeoPoint)) {
                mItemizedOverlay.setFocus(destinationOverlayItem);
            }
        }

        mapOverlays.add(mItemizedOverlay);
    }

    public String getRouteDescription(SubTrip subTrip) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        String description;
        if ("Walk".equals(subTrip.transport.type)) {
            description = getString(R.string.trip_description_walk,
                    subTrip.departureTime,
                    subTrip.arrivalTime,
                    getLocationName(subTrip.origin),
                    getLocationName(subTrip.destination));
        } else {
            description = getString(R.string.trip_description_normal,
                    subTrip.departureTime, subTrip.arrivalTime,
                    subTrip.transport.name,
                    getLocationName(subTrip.origin),
                    subTrip.transport.towards,
                    getLocationName(subTrip.destination));
        }
        return description;
    }

    private String getLocationName(Planner.Location location) {
        // TODO: Copied from RouteDetailActivity, centralize please!
        if (location == null) {
            return "Unknown";
        }
        if (location.isMyLocation()) {
            return getString(R.string.my_location);
        }
        return location.name;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.actionbar_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_my_location:
                if (mMyLocationOverlay.isMyLocationEnabled()) {
                    GeoPoint myLocation = mMyLocationOverlay.getMyLocation();
                    if (myLocation != null) {
                        mapController.animateTo(myLocation);
                    }
                } else {
                    toastMissingMyLocationSource();
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toastMissingMyLocationSource() {
        Toast.makeText(this, getText(R.string.my_location_source_disabled),
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
 
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.enableMyLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableMyLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableMyLocation();
    }

    private void disableMyLocation() {
        if (mMyLocationOverlay != null) {
            mMyLocationOverlay.disableCompass();
            mMyLocationOverlay.disableMyLocation();
        }
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private void myLocationOverlay() {
        mMyLocationOverlay = new FixedMyLocationOverlay(this, mMapView);
        if (mMyLocationOverlay.isMyLocationEnabled()) {
            mMyLocationOverlay.enableMyLocation();
        }
        if (mMyLocationOverlay.isCompassEnabled()) {
            mMyLocationOverlay.enableCompass();
        }
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    /**
     * Converts the passed trip to a visual route. 
     */
    class RouteOverlay extends Overlay {

        private Paint mMarkerPaint;
        private static final int markerRadius = 8;
        private ArrayList<GeoPoint> mPoints;
        private Trip2 mTrip;

        public RouteOverlay(Trip2 trip) {
            mTrip = trip;
            mMarkerPaint = new Paint();
            mMarkerPaint.setColor(Color.DKGRAY);
            mMarkerPaint.setAntiAlias(true);
        }

        public void addGeoPoint(GeoPoint geoPoint) {
            if (mPoints == null) {
                mPoints = new ArrayList<GeoPoint>();
            }
            mPoints.add(geoPoint);
        }

        protected Paint createPaint(SubTrip subTrip) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(subTrip.transport.getColor());
            paint.setStrokeWidth(6);

            if (subTrip.transport.type.equalsIgnoreCase("walk")) {
                DashPathEffect dashPath = new DashPathEffect(new float[]{6,2}, 1);
                paint.setPathEffect(dashPath);
            }

            return paint;
        }

        protected void drawStopMarker(SubTrip subTrip, Canvas canvas, Point point) {
            RectF oval = new RectF(point.x-markerRadius,
                    point.y-markerRadius,
                    point.x+markerRadius,
                    point.y+markerRadius);
            mMarkerPaint.setColor(subTrip.transport.getColor());
            canvas.drawOval(oval, mMarkerPaint);
            oval.inset(4, 4);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Projection projection = mapView.getProjection();
            if (shadow == false) {

                GeoPoint lastGeoPoint = null;
                for (SubTrip subTrip : mTrip.subTrips) {
                    GeoPoint originGeoPoint = new GeoPoint(subTrip.origin.latitude,
                            subTrip.origin.longitude);
                    
                    Paint paint = createPaint(subTrip); 

                    Point originPoint = new Point();
                    projection.toPixels(originGeoPoint, originPoint);
                    drawStopMarker(subTrip, canvas, originPoint);

                    if (lastGeoPoint != null) {
                        Point point2 = new Point();
                        projection.toPixels(lastGeoPoint, point2);
                        Paint connectionPaint = new Paint(paint);
                        connectionPaint.setColor(Color.DKGRAY);
                        DashPathEffect dashPath = new DashPathEffect(new float[]{6,2}, 1);
                        connectionPaint.setPathEffect(dashPath);
                        canvas.drawLine((float) originPoint.x,
                                (float) originPoint.y,
                                (float) point2.x,
                                (float) point2.y, connectionPaint);
                    }

                    GeoPoint lastIntermediateGeoPoint = null;
                    for (IntermediateStop stop : subTrip.intermediateStop) {
                        GeoPoint intermediateStopGeoPoint = new GeoPoint(stop.location.latitude,
                                stop.location.longitude);
                        
                        Point intermediatePoint = new Point();
                        projection.toPixels(intermediateStopGeoPoint, intermediatePoint);
                        drawStopMarker(subTrip, canvas, intermediatePoint);
                        if (lastIntermediateGeoPoint != null) {
                            Point point2 = new Point();
                            projection.toPixels(lastIntermediateGeoPoint, point2);
                            canvas.drawLine((float) intermediatePoint.x,
                                    (float) intermediatePoint.y,
                                    (float) point2.x,
                                    (float) point2.y, paint);
                        } else {
                            canvas.drawLine((float) originPoint.x, (float) originPoint.y, (float) intermediatePoint.x,(float) intermediatePoint.y, paint);
                        }

                        lastIntermediateGeoPoint = intermediateStopGeoPoint;
                    }

                    GeoPoint destinationGeoPoint = new GeoPoint(subTrip.destination.latitude,
                            subTrip.destination.longitude);
                    Point destinationPoint = new Point();
                    projection.toPixels(destinationGeoPoint, destinationPoint);
                    drawStopMarker(subTrip, canvas, destinationPoint);
                    if (lastIntermediateGeoPoint != null) {
                        Point lastPoint = new Point();
                        projection.toPixels(lastIntermediateGeoPoint, lastPoint);
                        canvas.drawLine((float) lastPoint.x,
                                (float) lastPoint.y,
                                (float) destinationPoint.x,
                                (float) destinationPoint.y, paint);
                    } else {
                        // Assume origin
                        canvas.drawLine((float) originPoint.x,
                                (float) originPoint.y,
                                (float) destinationPoint.x,
                                (float) destinationPoint.y, paint);
                    }

                    lastGeoPoint = destinationGeoPoint;
                }
            }

            super.draw(canvas, mapView, shadow);
        }
    }

}
