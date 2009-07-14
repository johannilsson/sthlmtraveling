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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class RoutesActivity extends ListActivity {
    private final String TAG = "RoutesActivity";
    private final int LATER_ROUTES = 1;
    private final int EARLIER_ROUTES = 0;
    private static final int DIALOG_NO_ROUTE_DETAILS_FOUND = 0;
    private final Handler mHandler = new Handler();
    private ArrayAdapter<Route> mRouteAdapter;
    private TextView mFromView;
    private TextView mToView;
    private int mCurrentRoutePosition;
    private ListView mEarlierLaterRoutes;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        updateRoutes();

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);

        Bundle extras = getIntent().getExtras();
        mFromView.setText(extras.getString("com.markupartist.sthlmtraveling.startPoint"));
        mToView.setText(extras.getString("com.markupartist.sthlmtraveling.endPoint"));

        // Check the simple view in android.layout to see if we can use them
        // instead of simple_list_row.
        ArrayAdapter<String> earlierLaterAdapter = 
            new ArrayAdapter<String>(this, R.layout.simple_list_row);

        earlierLaterAdapter.add("Show earlier routes");
        earlierLaterAdapter.add("Show later routes");

        mEarlierLaterRoutes = (ListView) findViewById(R.id.earlier_later_routes);
        mEarlierLaterRoutes.setAdapter(earlierLaterAdapter);
        mEarlierLaterRoutes.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, 
                    int position, long id) {
                switch (position) {
                    case EARLIER_ROUTES:
                        final ProgressDialog earlierProgress = ProgressDialog.show(RoutesActivity.this, "", getText(R.string.loading), true);
                        new Thread() {
                            public void run() {
                                try {
                                    Planner.getInstance().findEarlierRoutes();
                                    mHandler.post(mSearchRoutes);
                                    earlierProgress.dismiss();
                                } catch (Exception e) {
                                    earlierProgress.dismiss();
                                }
                            }
                        }.start();
                        break;
                    case LATER_ROUTES:
                        final ProgressDialog laterProgress = ProgressDialog.show(RoutesActivity.this, "", getText(R.string.loading), true);
                        new Thread() {
                            public void run() {
                                try {
                                    Planner.getInstance().findEarlierRoutes();
                                    mHandler.post(mSearchRoutes);
                                    laterProgress.dismiss();
                                } catch (Exception e) {
                                    laterProgress.dismiss();
                                }
                            }
                        }.start();
                        break;
                }
            }
        });
    }

    final Runnable mSearchRoutes = new Runnable() {
        public void run() {
            updateRoutes();
        }
    };

    private void updateRoutes() {
        ArrayList<Route> routes = Planner.getInstance().lastFoundRoutes();
        Log.d(TAG, "Updating routes " + routes);

        mRouteAdapter = new ArrayAdapter<Route>(this, R.layout.routes_row, routes);
        setListAdapter(mRouteAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Route route = mRouteAdapter.getItem(position);
        mCurrentRoutePosition = position;
        findRouteDetails(route);
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
                    mHandler.post(mSearchRouteDetails);
                    progressDialog.dismiss();
                } catch (Exception e) {
                    progressDialog.dismiss();
                }
            }
        }.start();
    }

    final Runnable mSearchRouteDetails = new Runnable() {
        public void run() {
            onSearchRouteDetailsResult();
        }
    };

    /**
     * Called when we got a route details search result.
     */
    private void onSearchRouteDetailsResult() {
        if (Planner.getInstance().lastFoundRouteDetail() != null 
                && !Planner.getInstance().lastFoundRoutes().isEmpty()) {
            Intent i = new Intent(RoutesActivity.this, RouteDetailActivity.class);
            i.putExtra("com.markupartist.sthlmtraveling.route_position", mCurrentRoutePosition);
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
