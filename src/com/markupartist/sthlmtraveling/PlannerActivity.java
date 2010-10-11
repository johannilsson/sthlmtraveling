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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.markupartist.sthlmtraveling.AutoCompleteStopAdapter.FilterListener;
import com.markupartist.sthlmtraveling.provider.HistoryDbAdapter;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.planner.Stop;
import com.markupartist.sthlmtraveling.utils.LocationUtils;

public class PlannerActivity extends BaseActivity implements OnCheckedChangeListener {
    private static final String TAG = "PlannerActivity";
    private static final int DIALOG_START_POINT = 0;
    private static final int DIALOG_END_POINT = 1;
    private static final int DIALOG_ABOUT = 2;
    private static final int DIALOG_NO_LOCATION = 5;
    private static final int DIALOG_DIALOG_DATE = 6;
    private static final int DIALOG_TIME = 7;
    private static final int DIALOG_CREATE_SHORTCUT_NAME = 8;
    private static final int DIALOG_REINSTALL_APP = 9;
    protected static final int REQUEST_CODE_POINT_ON_MAP_START = 0;
    protected static final int REQUEST_CODE_POINT_ON_MAP_END = 1;

    private AutoCompleteTextView mStartPointAutoComplete;
    private AutoCompleteTextView mEndPointAutoComplete;
    private Stop mStartPoint = new Stop();
    private Stop mEndPoint = new Stop();
    private HistoryDbAdapter mHistoryDbAdapter;
    private boolean mCreateShortcut;
    private Time mTime;
    private Button mDateButton;
    private Button mTimeButton;
    private LinearLayout mChangeTimeLayout;
    private Spinner mWhenSpinner;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        restoreState(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon a search.
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
        }

        mStartPointAutoComplete = createAutoCompleteTextView(R.id.from, R.id.from_progress, mStartPoint);
        mEndPointAutoComplete = createAutoCompleteTextView(R.id.to, R.id.to_progress, mEndPoint);
        
        // Set "my location" as default start point
        mStartPoint.setName(Stop.TYPE_MY_LOCATION);
        mStartPointAutoComplete.setText(getText(R.string.my_location));


        try {
            mHistoryDbAdapter = new HistoryDbAdapter(this).open();
        } catch (Exception e) {
            showDialog(DIALOG_REINSTALL_APP);
            return;
        }

        // Setup search button.
        final Button search = (Button) findViewById(R.id.search_route);
        search.setOnClickListener(mGetSearchListener);

        // Setup view for choosing other data for start and end point.
        final ImageButton fromDialog = (ImageButton) findViewById(R.id.from_menu);
        fromDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	mStartPointAutoComplete.setError(null);
                showDialog(DIALOG_START_POINT);
            }
        });

        final ImageButton toDialog = (ImageButton) findViewById(R.id.to_menu);
        toDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	mEndPointAutoComplete.setError(null);
                showDialog(DIALOG_END_POINT);
            }
        });

        // Views for date and time
        mChangeTimeLayout = (LinearLayout) findViewById(R.id.planner_change_time_layout);

        mDateButton = (Button) findViewById(R.id.planner_route_date);
        mDateButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                showDialog(DIALOG_DIALOG_DATE);
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

        mWhenSpinner = (Spinner) findViewById(R.id.departure_arrival_choice);
        ArrayAdapter<CharSequence> whenChoiceAdapter = ArrayAdapter.createFromResource(
                this, R.array.when_choice, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mWhenSpinner.setAdapter(whenChoiceAdapter);

        // Handle create shortcut.
        if (mCreateShortcut) {
            registerEvent("Planner create shortcut");
            setTitle(R.string.create_shortcut_label);
            search.setText(getText(R.string.create_shortcut_label));
            RadioGroup chooseTimeGroup = (RadioGroup) findViewById(R.id.planner_choose_time_group);
            chooseTimeGroup.setVisibility(View.GONE);
        } else {
            registerEvent("Planner");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

//        if (mTime != null) {
//            mTime.setToNow();
//            onTimeChanged();
//        }
        if (!mStartPoint.hasName()) {
            mStartPoint.setName(Stop.TYPE_MY_LOCATION);
            mStartPointAutoComplete.setText(getText(R.string.my_location));
//            mStartPointAutoComplete.setText("");
        }
        
        if (!mEndPoint.hasName()) {
            mEndPointAutoComplete.setText("");
        }
    }

    /**
     * On click listener for search and create shortcut button. Validates that the start point and 
     * the end point is correctly filled out before moving on.
     */
    View.OnClickListener mGetSearchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            
            Log.d(TAG, "startpoint: " + mStartPoint.toString());
            
            //if (!mStartPoint.hasName()) {
            if (TextUtils.isEmpty(mStartPointAutoComplete.getText())) {
                mStartPointAutoComplete.setError(getText(R.string.empty_value));
            //} else if (!mEndPoint.hasName()) {
            } else if (TextUtils.isEmpty(mEndPointAutoComplete.getText())) {
                mEndPointAutoComplete.setError(getText(R.string.empty_value));
            } else {
                mStartPoint = buildStop(mStartPoint, mStartPointAutoComplete);
                mEndPoint = buildStop(mEndPoint, mEndPointAutoComplete);

                if (mCreateShortcut) {
                    showDialog(DIALOG_CREATE_SHORTCUT_NAME);
                    //onCreateShortCut(mStartPoint, mEndPoint);
                } else {
                    onSearchRoutes(mStartPoint, mEndPoint, mTime);
                }
            }
        }
    };

    private Stop buildStop(Stop stop, AutoCompleteTextView auTextView) {
        if (stop.hasName() 
                && stop.getName().equals(auTextView.getText().toString())) {
            return stop;
        } else if (stop.isMyLocation()
                && auTextView.getText().toString().equals(getString(R.string.my_location))) {
            // Check for my location.
            return stop;
        } else if (auTextView.getText().toString().equals(getString(R.string.point_on_map))) {
            // Check for point-on-map.
            return stop;
        }
        
        Log.d(TAG, "no match: " + stop.toString() + " " + auTextView.getText().toString());
        
        stop.setName(auTextView.getText().toString());
        stop.setLocation(null);
        return stop;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mStartPoint != null) {
            outState.putParcelable("startPoint", mStartPoint);
        }
        if (mEndPoint != null) {
            outState.putParcelable("endPoint", mEndPoint);
        }
    }

    private void restoreState(Bundle state) {
        mStartPoint = new Stop();
        mEndPoint = new Stop();
        if (state != null) {
            Stop startPoint = state.getParcelable("startPoint");
            Stop endPoint = state.getParcelable("endPoint");
            if (startPoint != null) {
                mStartPoint = startPoint;
            }
            if (endPoint != null) {
                mEndPoint = endPoint;
            }
        }
    }

    private AutoCompleteTextView createAutoCompleteTextView(
            int autoCompleteResId, int progressResId, final Stop stop) {
        // TODO: Wrap the auto complete view in a custom view...

        SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean searchAddresses = sharedPreferences.getBoolean(
                "search_address_enabled", true);

        final AutoCompleteTextView autoCompleteTextView =
            (AutoCompleteTextView) findViewById(autoCompleteResId);
        final AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(this,
                R.layout.autocomplete_item_2line, Planner.getInstance(),
                searchAddresses);
        final ProgressBar progress = (ProgressBar) findViewById(progressResId);

        stopAdapter.setFilterListener(new FilterListener() {
            @Override
            public void onPublishFiltering() {
                progress.setVisibility(View.GONE);
            }
            @Override
            public void onPerformFiltering() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        autoCompleteTextView.setAdapter(stopAdapter);
        autoCompleteTextView.setSelectAllOnFocus(true);

        autoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?>  parent, View  view, int position, long id) {
                Log.d(TAG, "Item clicked: " + position);
                Object value = stopAdapter.getValue(position);
                if (value instanceof String) {
                    stop.setLocation(null);
                    stop.setName((String) value);
                } else if (value instanceof Address) {
                    Address address = (Address) value;

                    Log.d(TAG, "address: " + address);

                    stop.setLocation(
                            (int)(address.getLatitude() * 1E6),
                            (int) (address.getLongitude() * 1E6));
                    String addressLine = LocationUtils.getAddressLine(address);
                    //autoCompleteTextView.focusSearch(View.FOCUS_UP);
                    
                    //autoCompleteTextView.setText(addressLine);
                    stop.setName(addressLine);
                    //autoCompleteTextView.clearFocus();

                    Log.d(TAG, stop.toString() + " location " + stop.getLocation());
                    
                }
            }
        });

       // autoCompleteTextView.setOnClickListener(new OnClickListener() {
       //	@Override
       //public void onClick(View v) {
				/*if (autoCompleteTextView.hasSelection()) {
					autoCompleteTextView.setTextKeepState(autoCompleteTextView.getText());
				}*/
			    //autoCompleteTextView.selectAll();
		//	    int stop = autoCompleteTextView.getText().length();
		//	    autoCompleteTextView.setSelection(0, stop);
		//	}
		//});
		
        autoCompleteTextView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int stop = autoCompleteTextView.getText().length();
                autoCompleteTextView.setSelection(0, stop);
                return false;
            }
        });

        autoCompleteTextView.addTextChangedListener(
                new ReservedNameTextWatcher(getText(R.string.my_location), autoCompleteTextView));
        autoCompleteTextView.addTextChangedListener(
                new ReservedNameTextWatcher(getText(R.string.point_on_map), autoCompleteTextView));
        autoCompleteTextView.addTextChangedListener(
                new UpdateStopTextWatcher(stop));

        return autoCompleteTextView;
    }

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
    private void onSearchRoutes(Stop startPoint, Stop endPoint, Time time) {
        // TODO: We should not handle point-on-map this way. But for now we just
        // want it to work.
        if (!mStartPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map)))
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_START_POINT, startPoint);
        if (!mEndPointAutoComplete.getText().toString().equals(getString(R.string.point_on_map)))
            mHistoryDbAdapter.create(HistoryDbAdapter.TYPE_END_POINT, endPoint);

        boolean isTimeDeparture = mWhenSpinner.getSelectedItemId() == 0 ? true : false;

        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint, time, isTimeDeparture);
        Intent i = new Intent(Intent.ACTION_VIEW, routesUri, this, RoutesActivity.class);
        startActivity(i);
    }

    /**
     * Setup a search short cut.
     * @param startPoint the start point
     * @param endPoint the end point
     */
    protected void onCreateShortCut(Stop startPoint, Stop endPoint, String name) {
        Uri routesUri = RoutesActivity.createRoutesUri(startPoint, endPoint, null, true);
        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, routesUri,
                this, RoutesActivity.class);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Then, set up the container intent (the response to the caller)
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
                this, R.drawable.shortcut);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        // Now, return the result to the launcher
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
            case DIALOG_DIALOG_DATE:
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

            final Cursor historyOriginCursor = mHistoryDbAdapter.fetchAllStartPoints();
            startManagingCursor(historyOriginCursor);
            final SelectPointAdapter startPointAdapter = new SelectPointAdapter(this, historyOriginCursor);
            stopManagingCursor(historyOriginCursor);
            startPointDialogBuilder.setAdapter(startPointAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        mStartPoint.setName(Stop.TYPE_MY_LOCATION);
                        mStartPointAutoComplete.setText(getText(R.string.my_location));
                        mStartPointAutoComplete.clearFocus();
                        break;
                    case 1:
                        Intent i = new Intent(PlannerActivity.this, PointOnMapActivity.class);
                        i.putExtra(PointOnMapActivity.EXTRA_STOP, mStartPoint);
                        i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                                getString(R.string.tap_your_start_point_on_map));
                        startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_START);
                        break;
                    default:
                        Stop startPoint = (Stop) startPointAdapter.getItem(which);
                        mStartPoint = new Stop(startPoint);
                        mStartPointAutoComplete.setText(mStartPoint.getName());
                        mStartPointAutoComplete.clearFocus();
                    }
                }
            });
            dialog = startPointDialogBuilder.create();
            break;
        case DIALOG_END_POINT:
            AlertDialog.Builder endPointDialogBuilder = new AlertDialog.Builder(this);
            endPointDialogBuilder.setTitle(getText(R.string.choose_end_point_label));
            final Cursor historyDestinationCursor = mHistoryDbAdapter.fetchAllEndPoints();
            startManagingCursor(historyDestinationCursor);
            final SelectPointAdapter endPointAdapter =
                new SelectPointAdapter(this, historyDestinationCursor);
            stopManagingCursor(historyDestinationCursor);
            endPointDialogBuilder.setAdapter(endPointAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        mEndPoint.setName(Stop.TYPE_MY_LOCATION);
                        mEndPointAutoComplete.setText(getText(R.string.my_location));
                        mEndPointAutoComplete.clearFocus();
                        break;
                    case 1:
                        Intent i = new Intent(PlannerActivity.this, PointOnMapActivity.class);
                        i.putExtra(PointOnMapActivity.EXTRA_STOP, mEndPoint);
                        i.putExtra(PointOnMapActivity.EXTRA_HELP_TEXT,
                                getString(R.string.tap_your_end_point_on_map));
                        startActivityForResult(i, REQUEST_CODE_POINT_ON_MAP_END);
                        break;
                    default:
                        Stop endPoint = (Stop) endPointAdapter.getItem(which);
                        mEndPoint = new Stop(endPoint);
                        mEndPointAutoComplete.setText(mEndPoint.getName());
                        mEndPointAutoComplete.clearFocus();
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
                .setInverseBackgroundForced(true)
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
                        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
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
        case DIALOG_NO_LOCATION:
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.no_location_title))
                .setMessage(getText(R.string.no_location_message))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        case DIALOG_DIALOG_DATE:
            return new DatePickerDialog(this, mDateSetListener,
                    mTime.year, mTime.month, mTime.monthDay);
        case DIALOG_TIME:
            // TODO: Base 24 hour on locale, same with the format.
            return new TimePickerDialog(this, mTimeSetListener,
                    mTime.hour, mTime.minute, true);
        case DIALOG_CREATE_SHORTCUT_NAME:
            final View chooseShortcutName = getLayoutInflater().inflate(R.layout.create_shortcut_name, null);
            final EditText shortCutName = (EditText) chooseShortcutName.findViewById(R.id.shortcut_name);
            return new AlertDialog.Builder(this)
                .setTitle(R.string.create_shortcut_label)
                .setView(chooseShortcutName)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onCreateShortCut(mStartPoint, mEndPoint, shortCutName.getText().toString());
                    }
                })
                .create();
        case DIALOG_REINSTALL_APP:
            return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getText(R.string.attention_label))
                .setMessage(getText(R.string.reinstall_app_message))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        }
        return dialog;
    }

    private CharSequence[] getDialogSelectPointItems() {
        CharSequence[] items = {
                getText(R.string.my_location), 
                getText(R.string.history_label),
                getText(R.string.point_on_map)
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                showDialog(DIALOG_ABOUT);
                return true;
            case R.id.reverse_start_end:
                Stop tmpStartPoint = new Stop(mEndPoint);
                Stop tmpEndPoint = new Stop(mStartPoint);

                mStartPoint = tmpStartPoint;
                mEndPoint = tmpEndPoint;

                String startPoint = mStartPointAutoComplete.getText().toString();
                String endPoint = mEndPointAutoComplete.getText().toString();
                mStartPointAutoComplete.setText(endPoint);
                mEndPointAutoComplete.setText(startPoint);
                return true;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent()
                        .setClass(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                Intent data) {
        switch (requestCode) {
        case REQUEST_CODE_POINT_ON_MAP_START:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mStartPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                mStartPointAutoComplete.setText(getText(R.string.point_on_map));
                //mStartPointAutoComplete.setText(mStartPoint.getName());
                Log.d(TAG, "Got Stop " + mStartPoint);
            }
            break;
        case REQUEST_CODE_POINT_ON_MAP_END:
            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "action canceled");
            } else {
                mEndPoint = data.getParcelableExtra(PointOnMapActivity.EXTRA_STOP);
                mEndPointAutoComplete.setText(getText(R.string.point_on_map));
                //mEndPointAutoComplete.setText(mEndPoint.getName());
                Log.d(TAG, "Got Stop " + mEndPoint);
            }
            break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHistoryDbAdapter != null) {
            mHistoryDbAdapter.close();
        }
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

    private class UpdateStopTextWatcher implements TextWatcher {
        private final Stop mStop;

        public UpdateStopTextWatcher(Stop stop) {
            mStop = stop;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!getString(R.string.my_location).equals(s.toString())
                    || getString(R.string.point_on_map).equals(s.toString())) {
                mStop.setName(s.toString());
                mStop.setLocation(null);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Needed by interface, but not used.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Needed by interface, but not used.
        }
        
    }

    private class ReservedNameTextWatcher implements TextWatcher {
        private final CharSequence mReservedName;
    	private final AutoCompleteTextView mViewToWatch;
    	private boolean mRemoveText = false;
    	private String mNewText;

    	public ReservedNameTextWatcher(CharSequence reservedName, AutoCompleteTextView viewToWatch) {
    	    mReservedName = reservedName;
    		mViewToWatch = viewToWatch;
    	}

		@Override
		public void afterTextChanged(Editable s) {
            if (mRemoveText) {
                mRemoveText = false;
                if (!mViewToWatch.getText().toString().equals(mNewText)) {
                    mViewToWatch.setText(mNewText);
                }
            }
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		    if (mRemoveText == false && s.toString().equals(mReservedName.toString())) {
		        mRemoveText = true;
		        mViewToWatch.setTextColor(Color.BLACK);
		    }
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		    mNewText = s.toString().substring(start, start + count);

		    if (s.toString().equals(mReservedName.toString())) {
                mViewToWatch.setTextColor(0xFF4F94CD);
            }
		}
    }

    private class SelectPointAdapter extends MultipleListAdapter {
        private SectionedAdapter mHistoryWrapperAdapter = new SectionedAdapter() {
            @Override
            protected View getHeaderView(Section section, int index, View convertView,
                    ViewGroup parent) {
                TextView result = (TextView) convertView;
                if (convertView == null)
                    result = (TextView) getLayoutInflater().inflate(R.layout.header, null);
                result.setText(section.caption);
                return (result);
            }
        };

        public SelectPointAdapter(Context context, Cursor historyCursor) {
            ArrayList<HashMap<String,String>> items = new ArrayList<HashMap<String,String>>(); 

            HashMap<String, String> myLocationItem = new HashMap<String, String>();
            myLocationItem.put("item", getString(R.string.my_location));
            items.add(myLocationItem);

            HashMap<String, String> pointOnMapItem = new HashMap<String, String>();
            pointOnMapItem.put("item", getString(R.string.point_on_map));
            items.add(pointOnMapItem);

            SimpleAdapter itemsAdapter = new SimpleAdapter(
                    context,
                    items,
                    android.R.layout.simple_list_item_1,
                    new String[] { "item" },
                    new int[] { android.R.id.text1 } );

            ArrayList<Stop> historyList = new ArrayList<Stop>();
            historyCursor.moveToFirst();
            for (int i=0; i< historyCursor.getCount(); i++) {
                Stop stop = new Stop();

                stop.setName(historyCursor.getString(HistoryDbAdapter.INDEX_NAME));
                stop.setLocation(
                        historyCursor.getInt(HistoryDbAdapter.INDEX_LATITUDE),
                        historyCursor.getInt(HistoryDbAdapter.INDEX_LONGITUDE));
                stop.setSiteId(historyCursor.getInt(HistoryDbAdapter.INDEX_SITE_ID));

                historyList.add(stop);
                historyCursor.moveToNext();
            }
            historyCursor.close();
            ArrayAdapter<Stop> historyAdapter =
                new ArrayAdapter<Stop>(context,
                        android.R.layout.simple_list_item_1, historyList);

            mHistoryWrapperAdapter.addSection(0,
                    getString(R.string.history_label), historyAdapter);

            addAdapter(0, itemsAdapter);
            addAdapter(1, mHistoryWrapperAdapter);
        }
    }
}
