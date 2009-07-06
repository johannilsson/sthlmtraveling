package com.markupartist.sthlmtraveling;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RouteDetail extends ListActivity {
    private ArrayAdapter<String> mDetailAdapter;
    final Handler mHandler = new Handler();
    final RouteDetailFinder mDetailFinder = new RouteDetailFinder();
    final Runnable mUpdateRouteDetails = new Runnable() {
        public void run() {
            updateRouteDetailsInUi();
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Bundle extras = getIntent().getExtras();
        String from = extras.getString("from");
        String to = extras.getString("to");
        String ident = extras.getString("ident");
        String routeId = extras.getString("routeId");

        TextView fromView = (TextView) findViewById(R.id.route_from);
        fromView.setText(from);
        TextView toView = (TextView) findViewById(R.id.route_to);
        toView.setText(to);

        findRouteDetails(ident, routeId);
    }

    /**
     * Fires off a thread to do the query. Will call updateRouteDetailsInUi 
     * when done.
     */
    private void findRouteDetails(final String ident, final String routeId) {
        final ProgressDialog progressDialog = 
            ProgressDialog.show(this, "", getText(R.string.loading), true);
        Thread t = new Thread() {
            public void run() {
                try {
                    mDetailAdapter = new ArrayAdapter<String>(
                            RouteDetail.this, R.layout.route_details_row, 
                                mDetailFinder.findDetail(ident, routeId));
                    mHandler.post(mUpdateRouteDetails);
                    progressDialog.dismiss();
                } catch (Exception e) {
                    progressDialog.dismiss();
                }
            }
        };
        t.start();
    }
    
    private void updateRouteDetailsInUi() {
        setListAdapter(mDetailAdapter);
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
