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

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.planner.Route;
import com.markupartist.sthlmtraveling.planner.Stop;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.tasks.FindRouteDetailsTask;
import com.markupartist.sthlmtraveling.tasks.FindRouteDetailsTask.OnNoRoutesDetailsResultListener;
import com.markupartist.sthlmtraveling.tasks.FindRouteDetailsTask.OnRouteDetailsResultListener;

public class RouteDetailActivity extends ListActivity 
        implements OnRouteDetailsResultListener, OnNoRoutesDetailsResultListener {
    public static final String EXTRA_START_POINT = "com.markupartist.sthlmtraveling.start_point";
    public static final String EXTRA_END_POINT = "com.markupartist.sthlmtraveling.end_point";
    public static final String EXTRA_ROUTE = "com.markupartist.sthlmtraveling.route";

    private ArrayAdapter<String> mDetailAdapter;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private ArrayList<String> mDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Bundle extras = getIntent().getExtras();
        Route route = extras.getParcelable(EXTRA_ROUTE);
        Stop startPoint = extras.getParcelable(EXTRA_START_POINT);
        Stop endPoint = extras.getParcelable(EXTRA_END_POINT);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        TextView startPointView = (TextView) findViewById(R.id.route_from);
        startPointView.setText(startPoint.getName());
        TextView endPointView = (TextView) findViewById(R.id.route_to);
        endPointView.setText(endPoint.getName());

        if (startPoint.isMyLocation()) {
            startPointView.setText(getMyLocationString(startPoint));
        }
        if (endPoint.isMyLocation()) {
            endPointView.setText(getMyLocationString(endPoint));
        }
        
        TextView dateTimeView = (TextView) findViewById(R.id.route_date_time);
        dateTimeView.setText(route.toString());

        FavoriteButtonHelper favoriteButtonHelper = new FavoriteButtonHelper(
                this, mFavoritesDbAdapter, startPoint.getName(), endPoint.getName());
        favoriteButtonHelper.loadImage();

        initRouteDetails(route);
    }

    /**
     * Helper that returns the my location text representation. If the {@link Location}
     * is set the accuracy will also be appended.
     * @param stop the stop
     * @return a text representation of my location
     */
    private CharSequence getMyLocationString(Stop stop) {
        CharSequence string = getText(R.string.my_location);
        if (stop.getLocation() != null) {
            string = String.format("%s (%sm)", string, stop.getLocation().getAccuracy());
        }
        return string;
    }

    /**
     * Find route details. Will first check if we already have data stored. 
     * @param route
     */
    private void initRouteDetails(Route route) {
        @SuppressWarnings("unchecked")
        final ArrayList<String> details = (ArrayList<String>) getLastNonConfigurationInstance();
        if (details != null) {
            onRouteDetailsResult(details);
        } else {
            FindRouteDetailsTask findRouteDetailsTask = new FindRouteDetailsTask(this);
            findRouteDetailsTask.setOnRouteDetailsResultListener(this);
            findRouteDetailsTask.setOnNoResultListener(this);
            findRouteDetailsTask.execute(route);
        }
    }

    /**
     * Called before this activity is destroyed, returns the previous details. This data is used 
     * if the screen is rotated. Then we don't need to ask for the data again.
     * @return route details
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDetails;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_route_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_search :
                Intent i = new Intent(this, StartActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFavoritesDbAdapter.close();
    }

    @Override
    public void onRouteDetailsResult(ArrayList<String> details) {
        mDetailAdapter = new ArrayAdapter<String>(this, R.layout.route_details_row, details);
        setListAdapter(mDetailAdapter);
        mDetails = details;
    }

    @Override
    public void onNoRoutesDetailsResult() {
        TextView noResult = (TextView) findViewById(R.id.route_details_no_result);
        noResult.setVisibility(View.VISIBLE);
    }

}
