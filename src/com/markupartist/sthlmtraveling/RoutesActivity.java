package com.markupartist.sthlmtraveling;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.SectionedAdapter.Section;

public class RoutesActivity extends ListActivity {
    private final String TAG = "RoutesActivity";
    private static final int DIALOG_NO_ROUTE_DETAILS_FOUND = 0;

    private final int SECTION_EARLIER_ROUTES = 1;
    private final int SECTION_ROUTES = 2;
    private final int SECTION_LATER_ROUTES = 3;

    private final Handler mHandler = new Handler();
    private ArrayAdapter<Route> mRouteAdapter;
    private TextView mFromView;
    private TextView mToView;
    /**
     * Holds the current selected route, this is referenced by 
     * RouteDetailActivity.
     */
    public static Route route;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        createSections();

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);

        Bundle extras = getIntent().getExtras();
        mFromView.setText(extras.getString("com.markupartist.sthlmtraveling.startPoint"));
        mToView.setText(extras.getString("com.markupartist.sthlmtraveling.endPoint"));
    }

    private void createSections() {
        // Earlier routes
        ArrayAdapter<String> earlierAdapter = 
            new ArrayAdapter<String>(this, R.layout.simple_list_row);
        earlierAdapter.add("Show earlier routes");
        mSectionedAdapter.addSection(SECTION_EARLIER_ROUTES, "Earlier routes", earlierAdapter);

        // Routes
        ArrayList<Route> routes = Planner.getInstance().lastFoundRoutes();        
        mRouteAdapter = new ArrayAdapter<Route>(this, R.layout.routes_row, routes);
        mRouteAdapter.setNotifyOnChange(true);
        mSectionedAdapter.addSection(SECTION_ROUTES, "Routes", mRouteAdapter);

        // Later routes
        ArrayAdapter<String> laterAdapter = 
            new ArrayAdapter<String>(this, R.layout.simple_list_row);
        laterAdapter.add("Show later routes");
        mSectionedAdapter.addSection(SECTION_LATER_ROUTES, "Later routes", laterAdapter);

        setListAdapter(mSectionedAdapter);
    }
    
    SectionedAdapter mSectionedAdapter = new SectionedAdapter() {
        protected View getHeaderView(Section section, int index, 
                View convertView, ViewGroup parent) {
            TextView result = (TextView) convertView;

            if (convertView == null) {
                result = (TextView) getLayoutInflater().inflate(R.layout.header, null);
            }

            result.setText(section.caption);

            return (result);
        }
    };

    /**
     * Updates routes in the UI after a search.
     */
    final Runnable mUpdateRoutes = new Runnable() {
        @Override public void run() {
            final ArrayList<Route> routes = Planner.getInstance().lastFoundRoutes();
            //TODO: Should find a better way than resetting the adapter like this.
            mRouteAdapter = new ArrayAdapter<Route>(RoutesActivity.this, R.layout.routes_row, routes);
            mSectionedAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Object item = mSectionedAdapter.getItem(position);
        if (item instanceof Route) {
            route = (Route) item;
            findRouteDetails(route);            
        } else {
            Section section = mSectionedAdapter.getSection(position);
            Log.d(TAG, "section.id=" + section.id);
            if (section.id == SECTION_EARLIER_ROUTES) {
                final ProgressDialog earlierProgress = ProgressDialog.show(RoutesActivity.this, "", getText(R.string.loading), true);
                earlierProgress.setCancelable(true);
                new Thread() {
                    public void run() {
                        try {
                            Planner.getInstance().findEarlierRoutes();
                            mHandler.post(mUpdateRoutes);
                            earlierProgress.dismiss();
                        } catch (Exception e) {
                            earlierProgress.dismiss();
                        }
                    }
                }.start();
            } else if (section.id == SECTION_LATER_ROUTES) {
                final ProgressDialog laterProgress = ProgressDialog.show(RoutesActivity.this, "", getText(R.string.loading), true);
                new Thread() {
                    public void run() {
                        try {
                            Planner.getInstance().findLaterRoutes();
                            mHandler.post(mUpdateRoutes);
                            laterProgress.dismiss();
                        } catch (Exception e) {
                            laterProgress.dismiss();
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Find route details. Calls onSearchRouteDetailsResult when done.
     * @param route the route to find details for
     */
    private void findRouteDetails(final Route route) {
        final ProgressDialog progressDialog = 
            ProgressDialog.show(this, "", getText(R.string.loading), true);
        new Thread() {
            public void run() {
                try {
                    Planner.getInstance().findRouteDetails(route);
                    mHandler.post(new Runnable() {
                        @Override public void run() {
                            onSearchRouteDetailsResult();
                        }
                    });
                    progressDialog.dismiss();
                } catch (Exception e) {
                    progressDialog.dismiss();
                }
            }
        }.start();
    }

    /**
     * Called when we got a route details search result.
     */
    private void onSearchRouteDetailsResult() {
        if (Planner.getInstance().lastFoundRouteDetail() != null 
                && !Planner.getInstance().lastFoundRoutes().isEmpty()) {
            Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
            startActivity(i);
        } else {
            showDialog(DIALOG_NO_ROUTE_DETAILS_FOUND);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_search :
                final Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
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
}
