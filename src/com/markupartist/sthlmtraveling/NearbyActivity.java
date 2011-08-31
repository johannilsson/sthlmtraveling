package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.actionbar.R;
import com.markupartist.sthlmtraveling.MyLocationManager.MyLocationFoundListener;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;

public class NearbyActivity extends BaseListActivity implements LocationListener {
    private static String TAG = "NearbyActivity";
    private MyLocationManager mMyLocationManager;
    private TextView mCurrentLocationText;
    private ActionBar mActionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            onCreateShortCut();
        }

        setContentView(R.layout.nearby);
        
        mActionBar = initActionBar(R.menu.actionbar_nearby);

        requestUpdate();

        mCurrentLocationText = (TextView) findViewById(R.id.current_location);
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
        mActionBar.setProgressBarVisibility(View.VISIBLE);
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mMyLocationManager = new MyLocationManager(locationManager);
        mMyLocationManager.requestLocationUpdates(new MyLocationFoundListener() {
            
            @Override
            public void onMyLocationFound(Location location) {
                // TODO Auto-generated method stub
                
            }
        });
        mMyLocationManager.setLocationListener(this);        
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
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Nearby");
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
        finish();
    }
    
    private void fill(ArrayList<Site> stopPoints) {
        mActionBar.setProgressBarVisibility(View.GONE);

        ArrayAdapter<Site> adapter =
            new ArrayAdapter<Site>(this, android.R.layout.simple_list_item_1, stopPoints);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Site stopPoint = (Site) getListAdapter().getItem(position);
        Intent i = new Intent(this, DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE_NAME, stopPoint.getName());
        startActivity(i);
    }

    private class FindNearbyStopAsyncTask extends AsyncTask<Location, Void, ArrayList<Site>> {

        @Override
        protected ArrayList<Site> doInBackground(Location... params) {
            try {
                return SitesStore.getInstance().nearby(params[0]);
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Site> result) {
            if (result != null) {
                fill(result);
            } else {
                Toast.makeText(NearbyActivity.this, "Your're in the void", Toast.LENGTH_LONG).show();
            }
        }
        
        
    }

    @Override
    public void onLocationChanged(Location location) {
        mActionBar.setTitle("Nearby ("+ location.getAccuracy() +"m)");
        new FindNearbyStopAsyncTask().execute(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        
    }
}
