package com.markupartist.sthlmtraveling;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
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
    private ArrayAdapter<Route> mRouteAdapter;
    private TextView mFromView;
    private TextView mToView;
    final Handler mHandler = new Handler();
    final RouteFinder mRouteFinder = new RouteFinder();
    final Runnable updateRoutes = new Runnable() {
        public void run() {
            updateRoutesInUi();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.routes_list);

        mFromView = (TextView) findViewById(R.id.route_from);
        mToView = (TextView) findViewById(R.id.route_to);

        Bundle extras = getIntent().getExtras();
        String from = extras.getString("from");
        String to = extras.getString("to");

        findRoutes(from, to);
    }

    /**
     * Fires off a thread to do the query. Will call updateRoutesInUi when done.
     * @param from TODO
     * @param to TODO
     */
    private void findRoutes(final String from, final String to) {
        final ProgressDialog progressDialog = 
            ProgressDialog.show(this, "", getText(R.string.loading), true);
        Thread t = new Thread() {
            public void run() {
                try {
                    mRouteAdapter = new ArrayAdapter<Route>(Routes.this, 
                            R.layout.routes_row, mRouteFinder.findRoutes(from, to));
                    mHandler.post(updateRoutes);
                    progressDialog.dismiss();
                } catch (Exception e) {
                    progressDialog.dismiss();
                }
            }
        };
        t.start();
    }

    private void updateRoutesInUi() {
    	if (!mRouteAdapter.isEmpty()) {
        	Route route = mRouteAdapter.getItem(0);
        	mFromView.setText(route.from);
        	mToView.setText(route.to);
            setListAdapter(mRouteAdapter);    		
    	} else {
    		// TODO: Add dialog with error...
    	}
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Route route = mRouteAdapter.getItem(position);
        Intent i = new Intent(this, RouteDetail.class);
        i.putExtra("from", route.from);
        i.putExtra("to", route.to);
        i.putExtra("ident", route.ident);
        i.putExtra("routeId", route.routeId);
        startActivity(i);
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
}
