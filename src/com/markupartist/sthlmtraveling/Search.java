package com.markupartist.sthlmtraveling;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class Search extends Activity {
    private static final String TAG = "Search";
    private static final int DIALOG_START_POINT = 0;
    private static final int DIALOG_END_POINT = 1;

    private AutoCompleteTextView mFromAutoComplete;
    private AutoCompleteTextView mToAutoComplete;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        StopFinder planner = new StopFinder();
        mFromAutoComplete = (AutoCompleteTextView) findViewById(R.id.from);
        StopSimpleAdapter stopAdapter = new StopSimpleAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mFromAutoComplete.setAdapter(stopAdapter);

        mToAutoComplete = (AutoCompleteTextView) findViewById(R.id.to);
        StopSimpleAdapter toAdapter = new StopSimpleAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mToAutoComplete.setAdapter(toAdapter);

        final Button search = (Button) findViewById(R.id.search_route);

        search.setOnClickListener(onSearchHandler);

        final ImageButton fromDialog = (ImageButton) findViewById(R.id.from_menu);
        fromDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_START_POINT);
            }
        });

        final ImageButton toDialog = (ImageButton) findViewById(R.id.to_menu);
        toDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_END_POINT);
            }
        });
    }

    View.OnClickListener onSearchHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mFromAutoComplete.getText().length() <= 0) {
                mFromAutoComplete.setError(getText(R.string.empty_value));
            } else if (mToAutoComplete.getText().length() <= 0) {
                mToAutoComplete.setError(getText(R.string.empty_value));
            } else {
                /* TODO: Query for routes here and just post the request to the
                 * next intent. Think that will be a better ux. Then we can also 
                 * suggest alternatives if no route was found.  
                 */
            	
            	// TODO: Send it as JSON
                Intent i = new Intent(Search.this, Routes.class);
                i.putExtra("from", mFromAutoComplete.getText().toString());
                i.putExtra("to", mToAutoComplete.getText().toString());
                startActivity(i);
            }
        }
    };
    
    @Override
    protected Dialog onCreateDialog(int id) {
        final CharSequence[] items = {"My Location"};
        Dialog dialog = null;
        switch(id) {
        case DIALOG_START_POINT:
            AlertDialog.Builder startPointDialogBuilder = new AlertDialog.Builder(this);
            startPointDialogBuilder.setTitle("Choose start point");
            startPointDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    mFromAutoComplete.setText(getAddressFromCurrentPosition());
                }
            });
            dialog = startPointDialogBuilder.create();
            break;
        case DIALOG_END_POINT:
            AlertDialog.Builder endPointDialogBuilder = new AlertDialog.Builder(this);
            endPointDialogBuilder.setTitle("Choose end point");
            endPointDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    mToAutoComplete.setText(getAddressFromCurrentPosition());
                }
            });
            dialog = endPointDialogBuilder.create();
            break;
        }
        return dialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                Dialog dialog = new Dialog(this);
                PackageManager pm = getPackageManager();
                String version = "";
                try {
                    PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);
                    version = pi.versionName;
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Could not get the package info.");
                }
                dialog.setContentView(R.layout.about_dialog);
                dialog.setTitle(getText(R.string.about) + " " + version);
                dialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getAddressFromCurrentPosition() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc == null && gpsLoc != null) {
            loc = gpsLoc;
        } else if (gpsLoc != null && gpsLoc.getTime() > loc.getTime()) {
            // If we got the gps loc is more recent than the network loc use it.
            loc = gpsLoc;
        }

        Double lat = loc.getLatitude();
        Double lng = loc.getLongitude();
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String addressString = null;
        try {
            //Log.d(TAG, "Getting address from position " + lat + "," + lng);
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (!addresses.isEmpty()) {
                // Address address = addresses.get(0);
                for (Address address : addresses) {
                    //Log.d("Search", address.getLocality()); // City
                    //Log.d("Search", address.getAddressLine(0)); // Full Street
                    //Log.d("Search", address.getThoroughfare()); // Street name
                    //Log.d("Search", address.getFeatureName()); // House number or interval*/
                    //Log.d(TAG, address.toString());
                    addressString = address.getLocality() + ", " + address.getThoroughfare();
                    if (address.getFeatureName().contains("-")) {
                        // getFeatureName returns all house numbers on the 
                        // position like 14-16 get the first number and append 
                        // that to the address.
                        addressString += " " + address.getFeatureName().split("-")[0];
                    } else if (address.getFeatureName().length() < 4) {
                        // Sometime the feature name also is the same as the 
                        // postal code, this is a bit ugly but we just assume that
                        // we do not have any house numbers that is bigger longer 
                        // than four, if so append it to the address.
                        addressString += " " + address.getFeatureName();
                    }
                }
            }
        } catch (IOException e) {
            // Change to some dialog?
            Toast.makeText(this, "Could not determine your position", 10);
            Log.e(TAG, e.getMessage());
        }
        return addressString;
    }
}