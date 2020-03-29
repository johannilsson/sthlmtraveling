package com.markupartist.sthlmtraveling;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.markupartist.sthlmtraveling.data.api.ApiService;
import com.markupartist.sthlmtraveling.data.models.NearbyStop;
import com.markupartist.sthlmtraveling.data.models.NearbyStopsResponse;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.provider.site.SitesStore;
import com.markupartist.sthlmtraveling.ui.adapter.NearbyAdapter;
import com.markupartist.sthlmtraveling.utils.LocationManager;
import com.markupartist.sthlmtraveling.utils.LocationUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class NearbyActivity extends BaseFragmentActivity implements
        LocationManager.LocationFoundListener, NearbyAdapter.NearbyStopClickListener {
    private static final String STATE_NEARBY_STOPS = "STATE_NEARBY_STOPS";
    private static String TAG = "NearbyActivity";
    private LocationManager mMyLocationManager;
    private ApiService mApiService;
    private RecyclerView mRecyclerView;
    private NearbyAdapter mNearbyStopsAdapter;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            onCreateShortCut();
        }

        registerScreen("Nearby Stops");

//        initGoogleApiClient();
        mMyLocationManager = new LocationManager(this, getGoogleApiClient());
        mMyLocationManager.setLocationListener(this);
        registerPlayService(mMyLocationManager);

        mApiService = MyApplication.get(this).getApiService();

        setContentView(R.layout.nearby);

        mRecyclerView = findViewById(R.id.nearby_recycler_view);
        mProgressBar = findViewById(R.id.search_progress_bar);

        setupRecyclerView();
        initActionBar();
        verifyLocationPermission();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_NEARBY_STOPS)) {
                List<NearbyStop> nearbyStops = savedInstanceState.getParcelableArrayList(STATE_NEARBY_STOPS);
                if (nearbyStops != null) {
                    mNearbyStopsAdapter.fill(nearbyStops);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(STATE_NEARBY_STOPS, new ArrayList<>(mNearbyStopsAdapter.getNearbyStops()));
    }

    public void setupRecyclerView() {
        mNearbyStopsAdapter = new NearbyAdapter(this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mNearbyStopsAdapter);
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
        showProgressBar();
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

        setResult(RESULT_OK, intent);
        finish();
    }

    public void showProgressBar() {
        if (mNearbyStopsAdapter.getItemCount() == 0) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    public void hideProgressBar() {
        mProgressBar.setVisibility(View.GONE);
    }

    public void showNearbyStops(List<NearbyStop> stopPoints) {
        mNearbyStopsAdapter.fill(stopPoints);
    }

    @Override
    public void onMyLocationFound(Location location) {
        if (location == null) {
            hideProgressBar();
            Snackbar.make(mRecyclerView, R.string.no_location_message, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        setTitle(getString(R.string.nearby) + " ("+ location.getAccuracy() +"m)");
        showProgressBar();

        mApiService.getNearbyStops(location.getLatitude(), location.getLongitude(),
                new Callback<NearbyStopsResponse>() {
            @Override
            public void success(NearbyStopsResponse nearbyStopsResponse, Response response) {
                hideProgressBar();
                showNearbyStops(nearbyStopsResponse.getSites());
            }

            @Override
            public void failure(RetrofitError error) {
                hideProgressBar();
                Snackbar.make(mRecyclerView, R.string.network_problem_message, Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    @Override
    public void onNearbyStopClick(NearbyStop nearbyStop) {
        Pair<String, String> nameAndLocality = SitesStore.nameAsNameAndLocality(nearbyStop.getName());

        Site stopPoint = new Site();
        stopPoint.setSource(Site.SOURCE_STHLM_TRAVELING);
        stopPoint.setType(Site.TYPE_TRANSIT_STOP);
        stopPoint.setId(nearbyStop.getSiteId());
        stopPoint.setLocation(LocationUtils.parseLocation(nearbyStop.getLocation()));
        stopPoint.setName(nameAndLocality.first);
        stopPoint.setLocality(nameAndLocality.second);

        Intent i = new Intent(this, DeparturesActivity.class);
        i.putExtra(DeparturesActivity.EXTRA_SITE, stopPoint);
        startActivity(i);
    }

    @Override
    public void onLocationPermissionGranted() {
        if (mNearbyStopsAdapter.getItemCount() == 0) {
            requestUpdate();
        }
    }

    @Override
    public void onLocationPermissionRationale() {
        hideProgressBar();
        Snackbar.make(mRecyclerView, R.string.permission_location_needed_search, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.allow, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        requestLocationPermission();
                    }
                })
                .show();
    }

    @Override
    public void onLocationPermissionDontShowAgain() {
        hideProgressBar();
    }

}
