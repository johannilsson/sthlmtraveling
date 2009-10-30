package com.markupartist.sthlmtraveling;


import java.io.IOException;
import java.util.ArrayList;
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
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.markupartist.sthlmtraveling.SearchRoutesTask.OnSearchRoutesResultListener;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;

public class PlannerActivity extends Activity implements OnSearchRoutesResultListener {
    private static final String TAG = "Search";
    private static final int DIALOG_START_POINT = 0;
    private static final int DIALOG_END_POINT = 1;
    private static final int DIALOG_ABOUT = 2;
    private static final int DIALOG_START_POINT_HISTORY = 3;
    private static final int DIALOG_END_POINT_HISTORY = 4;

    private AutoCompleteTextView mFromAutoComplete;
    private AutoCompleteTextView mToAutoComplete;
    private HistoryDbAdapter mHistoryDbAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        Planner planner = Planner.getInstance();

        mFromAutoComplete = (AutoCompleteTextView) findViewById(R.id.from);
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mFromAutoComplete.setAdapter(stopAdapter);

        mToAutoComplete = (AutoCompleteTextView) findViewById(R.id.to);
        AutoCompleteStopAdapter toAdapter = new AutoCompleteStopAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mToAutoComplete.setAdapter(toAdapter);

        final Button search = (Button) findViewById(R.id.search_route);

        search.setOnClickListener(mGetSearchListener);

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

    View.OnClickListener mGetSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mFromAutoComplete.getText().length() <= 0) {
                mFromAutoComplete.setError(getText(R.string.empty_value));
            } else if (mToAutoComplete.getText().length() <= 0) {
                mToAutoComplete.setError(getText(R.string.empty_value));
            } else {
                Time time = new Time();
                time.setToNow();

                SearchRoutesTask searchRoutesTask = 
                    new SearchRoutesTask(PlannerActivity.this)
                        .setOnSearchRoutesResultListener(PlannerActivity.this);
                searchRoutesTask.execute(mFromAutoComplete.getText().toString(), 
                        mToAutoComplete.getText().toString(), time);
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_START_POINT:
            AlertDialog.Builder startPointDialogBuilder = new AlertDialog.Builder(this);
            startPointDialogBuilder.setTitle("Choose start point");
            startPointDialogBuilder.setItems(getDialogSelectPointItems(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                    case 0:
                        mFromAutoComplete.setText(getAddressFromCurrentPosition());
                        break;
                    case 1:
                        showDialog(DIALOG_START_POINT_HISTORY);
                        break;
                    }
                }
            });
            dialog = startPointDialogBuilder.create();
            break;
        case DIALOG_END_POINT:
            AlertDialog.Builder endPointDialogBuilder = new AlertDialog.Builder(this);
            endPointDialogBuilder.setTitle("Choose end point");
            endPointDialogBuilder.setItems(getDialogSelectPointItems(), 
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                    case 0:
                        mToAutoComplete.setText(getAddressFromCurrentPosition());
                        break;
                    case 1:
                        showDialog(DIALOG_END_POINT_HISTORY);
                        break;
                    }
                }
            });
            dialog = endPointDialogBuilder.create();
            break;
        case DIALOG_ABOUT:
            PackageManager pm = getPackageManager();
            String version = "";
            try {
                PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);
                version = pi.versionName;
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Could not get the package info.");
            }

            View aboutLayout = getLayoutInflater()
                .inflate(R.layout.about_dialog,
                        (ViewGroup) findViewById(R.id.about_dialog_layout_root));

            return new AlertDialog.Builder(this)
                .setTitle(getText(R.string.app_name) + " " + version)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(aboutLayout)
                .setCancelable(true)
                .setPositiveButton(getText(android.R.string.ok), null)
                .setNeutralButton(getText(R.string.donate), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://pledgie.com/campaigns/6527"));
                        startActivity(browserIntent);
                    }
                })
                .setNegativeButton(getText(R.string.feedback), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent .setType("plain/text");
                        emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL,
                                new String[]{"sthlmtraveling@markupartist.com"});
                        emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT,
                                "STHLM Traveling feedback");
                        startActivity(Intent.createChooser(emailIntent,
                                getText(R.string.send_email)));
                    }
                })
                .create();
        case DIALOG_START_POINT_HISTORY:
            final Cursor startPointCursor = mHistoryDbAdapter.fetchAllStartPoints();
            startManagingCursor(startPointCursor);
            Log.d(TAG, "startPoints: " + startPointCursor.getCount());
            return new AlertDialog.Builder(this)
                .setTitle("History")
                .setCursor(startPointCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = startPointCursor.getColumnIndex(HistoryDbAdapter.KEY_NAME);
                        mFromAutoComplete.setText(startPointCursor.getString(index));
                    }
                }, HistoryDbAdapter.KEY_NAME)
                .create();
        case DIALOG_END_POINT_HISTORY:
            final Cursor endPointCursor = mHistoryDbAdapter.fetchAllEndPoints();
            startManagingCursor(endPointCursor);
            Log.d(TAG, "endPoints: " + endPointCursor.getCount());
            return new AlertDialog.Builder(this)
                .setTitle("History")
                .setCursor(endPointCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = endPointCursor.getColumnIndex(HistoryDbAdapter.KEY_NAME);
                        mToAutoComplete.setText(endPointCursor.getString(index));
                    }
                }, HistoryDbAdapter.KEY_NAME)
                .create();
        }
        return dialog;
    }

    private CharSequence[] getDialogSelectPointItems() {
        CharSequence[] items = {"My Location", "History"};
        return items;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_search, menu);
        return true;
    }

    private String getAddressFromCurrentPosition() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc == null && gpsLoc != null) {
            loc = gpsLoc;
        } else if (gpsLoc != null && gpsLoc.getTime() > loc.getTime()) {
            // If we the gps location is more recent than the network 
            // location use it.
            loc = gpsLoc;
        }

        Double lat = loc.getLatitude();
        Double lng = loc.getLongitude();
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String addressString = null;
        try {
            Log.d(TAG, "Getting address from position " + lat + "," + lng);
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 5);
            if (!addresses.isEmpty()) {
                for (Address address : addresses) {
                    //Log.d(TAG, address.toString());
                    if (addressString == null) {
                        addressString = address.getThoroughfare();
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

                    String locality = address.getLocality();
                    if (locality != null) {
                        addressString = locality + ", " + addressString;
                        break; // Get out of the loop
                    }
                }
            }
        } catch (IOException e) {
            // TODO: Change to dialog
            Toast.makeText(this, "Could not determine your position", 10);
            Log.e(TAG, e.getMessage());
        }
        return addressString;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                showDialog(DIALOG_ABOUT);
                return true;
            case R.id.reverse_start_end:
                String startPoint = mFromAutoComplete.getText().toString();
                String endPoint = mToAutoComplete.getText().toString();
                mFromAutoComplete.setText(endPoint);
                mToAutoComplete.setText(startPoint);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHistoryDbAdapter.close();
    }

    @Override
    public void onSearchRoutesResult(ArrayList<Route> routes) {
        String startPoint = mFromAutoComplete.getText().toString();
        String endPoint = mToAutoComplete.getText().toString();

        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_START_POINT, startPoint);
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_END_POINT, endPoint);

        Intent i = new Intent(PlannerActivity.this, RoutesActivity.class);
        i.putExtra("com.markupartist.sthlmtraveling.startPoint", startPoint);
        i.putExtra("com.markupartist.sthlmtraveling.endPoint", endPoint);
        startActivity(i);
    }
}
