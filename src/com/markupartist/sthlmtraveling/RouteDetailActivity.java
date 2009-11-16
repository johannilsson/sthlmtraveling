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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.planner.Route;
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
    private TextView mFromView;
    private TextView mToView;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private ArrayList<String> mDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Bundle extras = getIntent().getExtras();
        Route route = extras.getParcelable(EXTRA_ROUTE);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        mFromView = (TextView) findViewById(R.id.route_from);
        mFromView.setText(extras.getString(EXTRA_START_POINT));
        mToView = (TextView) findViewById(R.id.route_to);
        mToView.setText(extras.getString(EXTRA_END_POINT));

        TextView dateTimeView = (TextView) findViewById(R.id.route_date_time);
        dateTimeView.setText(route.toString());

        FavoriteButtonHelper favoriteButtonHelper = new FavoriteButtonHelper(
                this, mFavoritesDbAdapter, 
                mFromView.getText().toString(), mToView.getText().toString());
        favoriteButtonHelper.loadImage();

        initRouteDetails(route);
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
