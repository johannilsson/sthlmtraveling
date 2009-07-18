package com.markupartist.sthlmtraveling;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RouteDetailActivity extends ListActivity {
    private ArrayAdapter<String> mDetailAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.route_details_list);

        Route route = RoutesActivity.route;

        TextView fromView = (TextView) findViewById(R.id.route_from);
        fromView.setText(route.from + " (" + route.departure + ")");
        TextView toView = (TextView) findViewById(R.id.route_to);
        toView.setText(route.to + " (" + route.arrival + ")");

        mDetailAdapter = new ArrayAdapter<String>(
                RouteDetailActivity.this, R.layout.route_details_row, 
                    Planner.getInstance().lastFoundRouteDetail());
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
                final Intent intent = new Intent(this, SearchActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
