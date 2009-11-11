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
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.SectionedAdapter.Section;
import com.markupartist.sthlmtraveling.planner.Route;
import com.markupartist.sthlmtraveling.provider.FavoritesDbAdapter;
import com.markupartist.sthlmtraveling.tasks.OnSearchRoutesResultListener;
import com.markupartist.sthlmtraveling.tasks.SearchEarlierRoutesTask;
import com.markupartist.sthlmtraveling.tasks.SearchLaterRoutesTask;
import com.markupartist.sthlmtraveling.tasks.SearchRoutesTask;

public class RoutesActivity extends ListActivity implements OnSearchRoutesResultListener {
    private final String TAG = "RoutesActivity";
    private static final int DIALOG_NO_ROUTE_DETAILS_FOUND = 0;

    private static final int ADAPTER_EARLIER = 0;
    private static final int ADAPTER_ROUTES = 1;
    private static final int ADAPTER_LATER = 2;

    private final int SECTION_CHANGE_TIME = 1;
    private final int SECTION_ROUTES = 2;

    private final int CHANGE_TIME = 0;

    private RoutesAdapter mRouteAdapter;
    private MultipleListAdapter mMultipleListAdapter;
    private TextView mFromView;
    private TextView mToView;
    private ArrayList<HashMap<String, String>> mDateAdapterData;
    private Time mTime;
    private FavoritesDbAdapter mFavoritesDbAdapter;
    private FavoriteButtonHelper mFavoriteButtonHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        mFavoritesDbAdapter = new FavoritesDbAdapter(this).open();

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);

        Bundle extras = getIntent().getExtras();

        mTime = new Time();
        mTime.parse(extras.getString("com.markupartist.sthlmtraveling.routeTime"));

        mFromView.setText(extras.getString("com.markupartist.sthlmtraveling.startPoint"));
        mToView.setText(extras.getString("com.markupartist.sthlmtraveling.endPoint"));

        mFavoriteButtonHelper = new FavoriteButtonHelper(this, mFavoritesDbAdapter, 
                mFromView.getText().toString(), mToView.getText().toString());
        mFavoriteButtonHelper.loadImage();

        searchRoutes(mFromView.getText().toString(), mToView.getText().toString(), mTime);
    }

    /**
     * Search for routes. Will first check if we already have data stored.
     * @param startPoint the start point
     * @param endPoint the end point
     * @param time the time
     */
    private void searchRoutes(String startPoint, String endPoint, Time time) {
        @SuppressWarnings("unchecked")
        final ArrayList<Route> routes = (ArrayList<Route>) getLastNonConfigurationInstance();
        if (routes != null) {
            onSearchRoutesResult(routes);
        } else {
            SearchRoutesTask searchRoutesTask = new SearchRoutesTask(this);
            searchRoutesTask.setOnSearchRoutesResultListener(this);
            searchRoutesTask.execute(startPoint, endPoint, time);
        }
    }

    /**
     * Called before this activity is destroyed, returns the previous details. This data is used 
     * if the screen is rotated. Then we don't need to ask for the data again.
     * @return route details
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        return mRouteAdapter.getRoutes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFavoriteButtonHelper.loadImage();
    }

    private void createSections() {
        // Date and time adapter.
        String timeString = mTime.format("%R %x"); // %r
        mDateAdapterData = new ArrayList<HashMap<String,String>>(1); 
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("title", timeString);
        mDateAdapterData.add(item);
        SimpleAdapter dateTimeAdapter = new SimpleAdapter(
                this,
                mDateAdapterData,
                R.layout.date_and_time,
                new String[] { "title" },
                new int[] { R.id.date_time } );

        // Earlier routes
        SimpleAdapter earlierAdapter = createEarlierLaterAdapter(android.R.drawable.arrow_up_float);

        // Later routes
        SimpleAdapter laterAdapter = createEarlierLaterAdapter(android.R.drawable.arrow_down_float);

        mMultipleListAdapter = new MultipleListAdapter();
        mMultipleListAdapter.addAdapter(ADAPTER_EARLIER, earlierAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_ROUTES, mRouteAdapter);
        mMultipleListAdapter.addAdapter(ADAPTER_LATER, laterAdapter);

        mSectionedAdapter.addSection(SECTION_CHANGE_TIME, "Date & Time", dateTimeAdapter);
        mSectionedAdapter.addSection(SECTION_ROUTES, "Routes", mMultipleListAdapter);

        setListAdapter(mSectionedAdapter);
    }

    SectionedAdapter mSectionedAdapter = new SectionedAdapter() {
        protected View getHeaderView(Section section, int index, 
                View convertView, ViewGroup parent) {
            TextView result = (TextView) convertView;

            if (convertView == null)
                result = (TextView) getLayoutInflater().inflate(R.layout.header, null);

            result.setText(section.caption);
            return (result);
        }
    };

    /**
     * Helper to create earlier or later adapter.
     * @param resource the image resource to show in the list
     * @return a prepared adapter
     */
    private SimpleAdapter createEarlierLaterAdapter(int resource) {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("image", resource);
        list.add(map);

        SimpleAdapter adapter = new SimpleAdapter(this, list, 
                R.layout.earlier_later_routes_row,
                new String[] { "image"},
                new int[] { 
                    R.id.earlier_later,
                }
        );

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                switch (view.getId()) {
                case R.id.earlier_later:
                    ImageView imageView = (ImageView) view;
                    imageView.setImageResource((Integer) data);
                    return true;
                }
                return false;
            }
        });
        return adapter;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Section section = mSectionedAdapter.getSection(position);
        int sectionId = section.id;
        int innerPosition = mSectionedAdapter.getSectionIndex(position);
        Adapter adapter = section.adapter;

        switch (sectionId) {
        case SECTION_ROUTES:
            MultipleListAdapter multipleListAdapter = (MultipleListAdapter) adapter;
            int adapterId = multipleListAdapter.getAdapterId(innerPosition);
            switch(adapterId) {
            case ADAPTER_EARLIER:
                SearchEarlierRoutesTask serTask = new SearchEarlierRoutesTask(this);
                serTask.setOnSearchRoutesResultListener(this);
                serTask.execute();
                break;
            case ADAPTER_LATER:
                SearchLaterRoutesTask slrTask = new SearchLaterRoutesTask(this);
                slrTask.setOnSearchRoutesResultListener(this);
                slrTask.execute();
                break;
            case ADAPTER_ROUTES:
                Route route = (Route) mSectionedAdapter.getItem(position);
                findRouteDetails(route);
                break;
            }
            break;
        case SECTION_CHANGE_TIME:
            Intent i = new Intent(this, ChangeRouteTimeActivity.class);
            i.putExtra("com.markupartist.sthlmtraveling.routeTime", mTime.format2445());
            i.putExtra("com.markupartist.sthlmtraveling.startPoint", mFromView.getText());
            i.putExtra("com.markupartist.sthlmtraveling.endPoint", mToView.getText());
            startActivityForResult(i, CHANGE_TIME);
            break;
        }
    }

    @Override
    public void onSearchRoutesResult(ArrayList<Route> routes) { 
        if (mRouteAdapter == null) {
            mRouteAdapter = new RoutesAdapter(this, routes);
            createSections();
        } else {
            mRouteAdapter.refill(routes);
            mSectionedAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Find route details. Will start {@link RouteDetailActivity}. 
     * @param route the route to find details for 
     */
    private void findRouteDetails(final Route route) {
        Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
        i.putExtra("com.markupartist.sthlmtraveling.startPoint", mFromView.getText().toString());
        i.putExtra("com.markupartist.sthlmtraveling.endPoint", mToView.getText().toString());
        i.putExtra("com.markupartist.sthlmtraveling.route", route);
        startActivity(i);
    }

    /**
     * This method is called when the sending activity has finished, with the
     * result it supplied.
     * 
     * @param requestCode The original request code as given to startActivity().
     * @param resultCode From sending activity as per setResult().
     * @param data From sending activity as per setResult().
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                Intent data) {
        if (requestCode == CHANGE_TIME) {
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "Change time activity cancelled.");
            } else {
                String startPoint = data.getStringExtra("com.markupartist.sthlmtraveling.startPoint");
                String endPoint = data.getStringExtra("com.markupartist.sthlmtraveling.endPoint");
                String newTime = data.getStringExtra("com.markupartist.sthlmtraveling.routeTime");

                mTime.parse(newTime);
                HashMap<String, String> item = mDateAdapterData.get(0);
                item.put("title", mTime.format("%R %x"));

                SearchRoutesTask searchRoutesTask = new SearchRoutesTask(this);
                searchRoutesTask.setOnSearchRoutesResultListener(this);
                searchRoutesTask.execute(startPoint, endPoint, mTime);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
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
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_NO_ROUTE_DETAILS_FOUND:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            dialog = builder.setTitle("Unfortunately no route details was found")
                .setMessage("Most likely your session has timed out.")
                .setCancelable(true)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
                }).create();
            break;
        }
        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFavoritesDbAdapter.close();
    }

    private class RoutesAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<Route> mRoutes;

        public RoutesAdapter(Context context, ArrayList<Route> routes) {
            mContext = context;
            mRoutes = routes;
        }

        public void refill(ArrayList<Route> routes) {
            mRoutes = routes;
        }

        public ArrayList<Route> getRoutes() {
            return mRoutes;
        }

        @Override
        public int getCount() {
            return mRoutes.size();
        }

        @Override
        public Object getItem(int position) {
            return mRoutes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Route route = mRoutes.get(position);
            return new RouteAdapterView(mContext, route);
        }
    }

    private class RouteAdapterView extends LinearLayout {

        public RouteAdapterView(Context context, Route route) {
            super(context);
            this.setOrientation(VERTICAL);

            this.setPadding(10, 10, 10, 10);

            TextView routeDetail = new TextView(context);
            routeDetail.setText(route.toString());
            routeDetail.setTextColor(Color.WHITE);
            routeDetail.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

            LinearLayout routeChanges = new LinearLayout(context);
            routeChanges.setPadding(0, 3, 0, 0);

            /*
            TextView changesView = new TextView(context);
            changesView.setText(route.changes + " changes:");
            changesView.setPadding(0, 0, 5, 0);
            routeChanges.addView(changesView);
            */

            int currentTransportCount = 1;
            int transportCount = route.transports.size();
            for (Route.Transport transport : route.transports) {
                ImageView change = new ImageView(context);
                change.setImageResource(transport.imageResource());
                change.setPadding(0, 0, 5, 0);
                routeChanges.addView(change);

                if (transportCount > currentTransportCount) {
                    ImageView separator = new ImageView(context);
                    separator.setImageResource(R.drawable.transport_separator);
                    separator.setPadding(0, 5, 5, 0);
                    routeChanges.addView(separator);
                }

                currentTransportCount++;
            }

            this.addView(routeDetail);
            this.addView(routeChanges);
        }

    }
}
