package com.markupartist.sthlmtraveling;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Routes extends ListActivity {
    private final String TAG = "Routes";
    private static final int DIALOG_NO_ROUTE_DETAILS_FOUND = 0;
    private ArrayAdapter<Route> mRouteAdapter;
    private TextView mFromView;
    private TextView mToView;
    private int mCurrentRoutePosition;
    final Handler mHandler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        ArrayList<Route> routes = Planner.getInstance().lastFoundRoutes();
        if (routes != null && !routes.isEmpty()) {
            mRouteAdapter = new ArrayAdapter<Route>(this, 
                    R.layout.routes_row, routes);
            setListAdapter(mRouteAdapter);

            mFromView = (TextView) findViewById(R.id.route_from);
            mToView = (TextView) findViewById(R.id.route_to);

            Route route = mRouteAdapter.getItem(0);
            mFromView.setText(route.from);
            mToView.setText(route.to);            
        }
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
            Intent i = new Intent(Routes.this, RouteDetail.class);
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
                final Intent intent = new Intent(this, Search.class);
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
