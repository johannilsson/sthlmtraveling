package com.markupartist.sthlmtraveling;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.markupartist.sthlmtraveling.utils.Analytics;
import com.markupartist.sthlmtraveling.utils.PlayService;
import com.markupartist.sthlmtraveling.utils.PlayServicesUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseFragmentActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int PERMISSIONS_REQUEST_LOCATION = 100;
    private Toolbar mActionBarToolbar;

    private List<PlayService> mPlayServices;
    private GoogleApiClient mGoogleApiClient;

    public void registerPlayService(PlayService playService) {
        if (mPlayServices == null) {
            mPlayServices = new ArrayList<>();
        }
        mPlayServices.add(playService);
    }

    public synchronized void initGoogleApiClient(boolean checkIfAvailable) {
        boolean looksGood = true;
        if (checkIfAvailable) {
            looksGood = PlayServicesUtils.checkGooglePlaySevices(this);
        }
        if (looksGood) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    public synchronized void initGoogleApiClient() {
        initGoogleApiClient(true);
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        if (mPlayServices != null) {
            for (PlayService ps : mPlayServices) {
                ps.onStop();
            }
        }

        super.onStop();
    }

    protected ActionBar initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        return actionBar;
    }

    @Override
    public void setSupportActionBar(final Toolbar toolbar) {
        mActionBarToolbar = toolbar;
        super.setSupportActionBar(toolbar);
    }

    public Toolbar getActionBarToolbar() {
        return mActionBarToolbar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void registerScreen(String event) {
        Analytics.getInstance(this).registerScreen(event);
    }

    protected void registerEvent(String event, Map<String, String> parameters) {
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Need to know if we are on the top level, then we should not apply this.
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    protected boolean shouldShowAds(boolean isPromotionsActivated) {
        if (getResources().getBoolean(R.bool.is_landscape)) {
            return false;
        }

        if (AppConfig.shouldServeAds() && isPromotionsActivated) {
            final SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            boolean isDisabled = sharedPreferences.getBoolean("is_ads_disabled", false);
            if (isDisabled) {
                Log.d("BaseFragmentActivity", "Ads disabled by the user");
                Analytics.getInstance(this).event("Ads", "Ad view disabled");
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mPlayServices != null) {
            for (PlayService ps : mPlayServices) {
                ps.onConnected();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        PlayServicesUtils.checkGooglePlaySevices(this);
    }

    public void verifyLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                onLocationPermissionRationale();
            } else {
                requestLocationPermission();
            }
        } else {
            onLocationPermissionGranted();
        }
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_LOCATION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted.
                    onLocationPermissionGranted();
                } else {
                    // Permission was denied.
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_CONTACTS)) {
                        // If the user has selected don't show again
                        onLocationPermissionDontShowAgain();
                    }
                }
            }
            break;
        }

    }

    public void onLocationPermissionGranted() {
    }

    public void onLocationPermissionRationale() {
    }

    public void onLocationPermissionDontShowAgain() {
    }
}