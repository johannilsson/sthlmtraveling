package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;
import com.markupartist.sthlmtraveling.utils.LocationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class NearbyActivity extends BaseListActivity implements LocationManager.LocationFoundListener {
    private static String TAG = "NearbyActivity";
    private LocationManager mMyLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            onCreateShortCut();
        }

        initGoogleApiClient();
        mMyLocationManager = new LocationManager(this, getGoogleApiClient());
        mMyLocationManager.setLocationListener(this);
        registerPlayService(mMyLocationManager);

        setContentView(R.layout.nearby);
        initActionBar();
        requestUpdate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_nearby, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_refresh:
            requestUpdate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestUpdate() {
        setSupportProgressBarIndeterminateVisibility(true);
        mMyLocationManager.requestLocation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMyLocationManager.removeUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMyLocationManager.removeUpdates();
    }

    protected void onCreateShortCut() {
        Intent shortcutIntent = new Intent(this, NearbyActivity.class);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.nearby_stops));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private void fill(ArrayList<Site> stopPoints, final Location location) {
        setSupportProgressBarIndeterminateVisibility(false);

        //Sort stop in order of distance from users location
        Collections.sort(stopPoints, new Comparator<Site>() {
            @Override
            public int compare(Site site1, Site site2) {
                float distanceToSite1 = location.distanceTo(site1.getLocation());
                float distanceToSite2 = location.distanceTo(site2.getLocation());
                if (distanceToSite1 > distanceToSite2) {
                    return 1;
                } else if (distanceToSite1 < distanceToSite2) {
                    return -1;
                }
                return 0;
            }
        });

        ArrayAdapter<Site> adapter =
            new ArrayAdapter<Site>(this, android.R.layout.simple_list_item_1, stopPoints);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Site stopPoint = (Site) getListAdapter().getItem(position);
        Intent i = new Intent(this, DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE, stopPoint);
        startActivity(i);
    }

    @Override
    public void onMyLocationFound(Location location) {
        // TODO: We need to handle this.
        if (location == null) {
            Log.e(TAG, "Location was null");
            return;
        }
        setTitle(getString(R.string.nearby) + " ("+ location.getAccuracy() +"m)");
        new FindNearbyStopAsyncTask(location).execute();
    }

    private class FindNearbyStopAsyncTask extends AsyncTask<Void, Void, ArrayList<Site>> {

        private Location mLocation;

        public FindNearbyStopAsyncTask(Location location){
            mLocation = location;
        }

        @Override
        protected ArrayList<Site> doInBackground(Void... params) {
            try {
                return SitesStore.getInstance().nearby(NearbyActivity.this, mLocation);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Site> result) {
            if (result != null) {
                fill(result, mLocation);
            } else {
                Toast.makeText(NearbyActivity.this, "Your're in the void", Toast.LENGTH_LONG).show();
            }
        }
        
        
    }
}
