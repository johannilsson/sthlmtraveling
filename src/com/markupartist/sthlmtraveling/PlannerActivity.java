/*
 * Copyright (C) 2009 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markupartist.sthlmtraveling;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
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
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TimePicker;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.markupartist.sthlmtraveling.planner.Planner;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;

public class PlannerActivity extends Activity implements OnCheckedChangeListener {
    private static final String TAG = "Search";
    private static final int DIALOG_START_POINT = 0;
    private static final int DIALOG_END_POINT = 1;
    private static final int DIALOG_ABOUT = 2;
    private static final int DIALOG_START_POINT_HISTORY = 3;
    private static final int DIALOG_END_POINT_HISTORY = 4;
    private static final int NO_LOCATION = 5;
    private static final int DIALOG_DATE = 6;
    private static final int DIALOG_TIME = 7;

    private AutoCompleteTextView mFromAutoComplete;
    private AutoCompleteTextView mToAutoComplete;
    private HistoryDbAdapter mHistoryDbAdapter;
    private boolean mCreateShortcut;
    private Time mTime;
    private Button mDateButton;
    private Button mTimeButton;
    private LinearLayout mChangeTimeLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
        }

        mHistoryDbAdapter = new HistoryDbAdapter(this).open();

        Planner planner = Planner.getInstance();

        // Setup autocomplete views.
        mFromAutoComplete = (AutoCompleteTextView) findViewById(R.id.from);
        AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mFromAutoComplete.setAdapter(stopAdapter);

        mToAutoComplete = (AutoCompleteTextView) findViewById(R.id.to);
        AutoCompleteStopAdapter toAdapter = new AutoCompleteStopAdapter(this, 
                android.R.layout.simple_dropdown_item_1line, planner);
        mToAutoComplete.setAdapter(toAdapter);

        // Setup search button.
        final Button search = (Button) findViewById(R.id.search_route);
        search.setOnClickListener(mGetSearchListener);
        if (mCreateShortcut) {
            search.setText(getText(R.string.create_shortcut_label));
        }

        // Setup view for choosing other data for start and end point.
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

        // Views for date and time
        mChangeTimeLayout = (LinearLayout) findViewById(R.id.planner_change_time_layout);

        mDateButton = (Button) findViewById(R.id.planner_route_date);
        mDateButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                showDialog(DIALOG_DATE);
            }
        });

        mTimeButton = (Button) findViewById(R.id.planner_route_time);
        mTimeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                showDialog(DIALOG_TIME);
            }
        });

        // Set time to now, and notify buttons about the new time.
        mTime = new Time();
        mTime.setToNow();
        onTimeChanged();

        // Views for radio buttons
        RadioButton nowRadioButton = (RadioButton) findViewById(R.id.planner_check_now);
        nowRadioButton.setOnCheckedChangeListener(this);
        RadioButton laterRadioButton = (RadioButton) findViewById(R.id.planner_check_later);
        laterRadioButton.setOnCheckedChangeListener(this);
    }

    /**
     * On click listener for search and create shortcut button. Validates that the start point and 
     * the end point is correctly filled out before moving on.
     */
    View.OnClickListener mGetSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mFromAutoComplete.getText().length() <= 0) {
                mFromAutoComplete.setError(getText(R.string.empty_value));
            } else if (mToAutoComplete.getText().length() <= 0) {
                mToAutoComplete.setError(getText(R.string.empty_value));
            } else {
                String startPoint = mFromAutoComplete.getText().toString();
                String endPoint = mToAutoComplete.getText().toString();
                if (mCreateShortcut) {
                    onCreateShortCut(startPoint, endPoint);
                } else {
                    onSearchRoutes(startPoint, endPoint, mTime);
                }
            }
        }
    };

    /**
     * On date set listener for the date picker. Sets the new date to the time member and updates 
     * views if the date was changed.
     */
    private DatePickerDialog.OnDateSetListener mDateSetListener =
        new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                    int dayOfMonth) {
                mTime.year = year;
                mTime.month = monthOfYear;
                mTime.monthDay = dayOfMonth;
                onTimeChanged();
            }
        };

    /**
     * On time set listener for the time picker. Sets the new time to the time member and updates 
     * views if the time was changed.
     */
    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mTime.hour = hourOfDay;
                mTime.minute = minute;
                onTimeChanged();
            }
        };

    /**
     * Start the search.
     * @param startPoint the start point
     * @param endPoint the end point
     * @param time the departure time
     */
    private void onSearchRoutes(String startPoint, String endPoint, Time time) {
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_START_POINT, startPoint);
        mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_END_POINT, endPoint);

        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint, time);
        Intent i = new Intent(Intent.ACTION_VIEW, routesUri, this, RoutesActivity.class);
        startActivity(i);
    }

    /**
     * Setup a search short cut.
     * @param startPoint the start point
     * @param endPoint the end point
     */
    protected void onCreateShortCut(String startPoint, String endPoint) {
        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint);
        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, routesUri,
                this, RoutesActivity.class);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, startPoint + " " + endPoint);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_DATE:
                ((DatePickerDialog) dialog).updateDate(mTime.year, mTime.month, mTime.monthDay);
                break;
            case DIALOG_TIME:
                ((TimePickerDialog) dialog).updateTime(mTime.hour, mTime.minute);
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_START_POINT:
            AlertDialog.Builder startPointDialogBuilder = new AlertDialog.Builder(this);
            startPointDialogBuilder.setTitle(getText(R.string.choose_start_point_label));
            startPointDialogBuilder.setItems(getDialogSelectPointItems(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                    case 0:
                        String startPointAddress = getAddressFromCurrentPosition();
                        if (startPointAddress== null) showDialog(NO_LOCATION);
                        mFromAutoComplete.setText(startPointAddress);
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
            endPointDialogBuilder.setTitle(getText(R.string.choose_end_point_label));
            endPointDialogBuilder.setItems(getDialogSelectPointItems(), 
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    switch(item) {
                    case 0:
                        String endPointAddress = getAddressFromCurrentPosition();
                        if (endPointAddress == null) showDialog(NO_LOCATION);
                        mToAutoComplete.setText(endPointAddress);
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
                                Uri.parse(getString(R.string.donate_url)));
                        startActivity(browserIntent);
                    }
                })
                .setNegativeButton(getText(R.string.feedback), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("plain/text");
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                                new String[]{getString(R.string.send_feedback_email_emailaddress)});
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                getText(R.string.send_feedback_email_title));
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
                .setTitle(getText(R.string.history_label))
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
                .setTitle(getText(R.string.history_label))
                .setCursor(endPointCursor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = endPointCursor.getColumnIndex(HistoryDbAdapter.KEY_NAME);
                        mToAutoComplete.setText(endPointCursor.getString(index));
                    }
                }, HistoryDbAdapter.KEY_NAME)
                .create();
        case NO_LOCATION:
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.no_location_title))
                .setMessage(getText(R.string.no_location_message))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        case DIALOG_DATE:
            return new DatePickerDialog(this, mDateSetListener,
                    mTime.year, mTime.month, mTime.monthDay);
        case DIALOG_TIME:
            // TODO: Base 24 hour on locale, same with the format.
            return new TimePickerDialog(this, mTimeSetListener,
                    mTime.hour, mTime.minute, true);
        }
        return dialog;
    }

    private CharSequence[] getDialogSelectPointItems() {
        CharSequence[] items = {
                getText(R.string.my_location), 
                getText(R.string.history_label)
            };
        return items;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_search, menu);
        return true;
    }

    /**
     * Update time on the buttons.
     */
    private void onTimeChanged() {
        String formattedDate = mTime.format("%x");
        String formattedTime = mTime.format("%R");
        mDateButton.setText(formattedDate);
        mTimeButton.setText(formattedTime);
    }

    /**
     * Get address from the current position.
     * TODO: Extract to a class
     * @return the address in the format "location name, street" or null if failed
     * to determine address 
     */
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

        if (loc == null) {
            return null;
        }

        Double lat = loc.getLatitude();
        Double lng = loc.getLongitude();

        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        String addressString = null;
        try {
            Log.d(TAG, "Getting address from position " + lat + "," + lng);
            // TODO: Move the call for getFromLocation to a separate background thread.
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // We only check the state for later radio button.
        if (isChecked && buttonView.getId() == R.id.planner_check_later) {
            mChangeTimeLayout.setVisibility(View.VISIBLE);
        } else if (!isChecked && buttonView.getId() == R.id.planner_check_later) {
            mChangeTimeLayout.setVisibility(View.GONE);
            mTime.setToNow();
            onTimeChanged();
        }
    }
}
