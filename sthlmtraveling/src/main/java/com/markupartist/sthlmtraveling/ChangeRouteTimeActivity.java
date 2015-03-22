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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.markupartist.sthlmtraveling.provider.TransportMode;
import com.markupartist.sthlmtraveling.provider.planner.JourneyQuery;
import com.markupartist.sthlmtraveling.provider.planner.Planner;
import com.markupartist.sthlmtraveling.provider.site.Site;
import com.markupartist.sthlmtraveling.ui.view.DelayAutoCompleteTextView;
import com.markupartist.sthlmtraveling.utils.ViewHelper;

import java.util.ArrayList;

public class ChangeRouteTimeActivity extends BaseActivity implements OnClickListener {
    static final String TAG = "ChangeRouteTimeActivity";
    static final int DIALOG_DATE = 0;
    static final int DIALOG_TIME = 1;

    private Site mViaPoint = new Site();
    private Time mTime;
    private JourneyQuery mJourneyQuery;

    private Button mDateButton;
    private Button mTimeButton;
    private DelayAutoCompleteTextView mViaPointAutoComplete;
    private Spinner mWhenSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_route_time);

        registerScreen("Change route time");

        restoreState(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mJourneyQuery = extras.getParcelable(RoutesActivity.EXTRA_JOURNEY_QUERY);

        mTime = mJourneyQuery.time;

        mDateButton = (Button) findViewById(R.id.change_route_date);
        mDateButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_DATE);
            }
        });

        mTimeButton = (Button) findViewById(R.id.change_route_time);
        mTimeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_TIME);
            }
        });

        ImageButton refreshTimeButton = (ImageButton) findViewById(R.id.btn_set_to_now);
        ViewHelper.tintIcon(refreshTimeButton.getDrawable(), Color.GRAY);
        refreshTimeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mJourneyQuery.time != null) {
                    mJourneyQuery.time.setToNow();
                    updateTimeViews();
                }
            }
        });

        // Don't fill via if it has been restored.
        if (!mViaPoint.looksValid() && mJourneyQuery.hasVia()) {
            mViaPoint = new Site();
            mViaPoint.setId(mJourneyQuery.via.id);
            mViaPoint.setName(mJourneyQuery.via.name);
            mViaPoint.setLocation(mJourneyQuery.via.latitude, mJourneyQuery.via.longitude);
        }

        mViaPointAutoComplete = createAutoCompleteTextView(R.id.via, mViaPoint, true);
        mViaPointAutoComplete.setText(mViaPoint.getName());

        int selectedPosition = mJourneyQuery.isTimeDeparture ? 0 : 1;
        mWhenSpinner = (Spinner) findViewById(R.id.departure_arrival_choice);
        ArrayAdapter<CharSequence> whenChoiceAdapter = ArrayAdapter.createFromResource(
                this, R.array.when_choice, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mWhenSpinner.setAdapter(whenChoiceAdapter);
        mWhenSpinner.setSelection(selectedPosition);

        // Inflate a "Done/Discard" custom action bar view.
        LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_custom_view_done_discard, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(this);
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(this);

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        updateViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mViaPoint != null && !mViaPoint.hasName()) {
            mViaPointAutoComplete.setText("");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mViaPoint != null) outState.putParcelable("viaPoint", mViaPoint);

        super.onSaveInstanceState(outState);
    }

    private void restoreState(Bundle state) {
        mViaPoint = new Site();
        if (state != null) {
            Site viaPoint = state.getParcelable("viaPoint");
            if (viaPoint != null) mViaPoint.fromSite(viaPoint);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_change_route_time, menu);
        return true;
    }

    /**
     * Update time on the buttons.
     */
    private void updateViews() {
        updateTimeViews();

        // Update transport types
        CheckBox transportBus = (CheckBox) findViewById(R.id.planner_transport_bus);
        CheckBox transportMetro = (CheckBox) findViewById(R.id.planner_transport_metro);
        CheckBox transportNar = (CheckBox) findViewById(R.id.planner_transport_nar);
        CheckBox transportTrain = (CheckBox) findViewById(R.id.planner_transport_train);
        CheckBox transportTram = (CheckBox) findViewById(R.id.planner_transport_tram);
        CheckBox transportWax = (CheckBox) findViewById(R.id.planner_transport_wax);

        for (String type : mJourneyQuery.transportModes) {
            if (type.equals(TransportMode.BUS)) {
                transportBus.setChecked(true);
            } else if (type.equals(TransportMode.METRO)) {
                transportMetro.setChecked(true);
            } else if (type.equals(TransportMode.NAR)) {
                transportNar.setChecked(true);
            } else if (type.equals(TransportMode.TRAIN)) {
                transportTrain.setChecked(true);
            } else if (type.equals(TransportMode.TRAM)) {
                transportTram.setChecked(true);
            } else if (type.equals(TransportMode.WAX)) {
                transportWax.setChecked(true);
            }
        }

        CheckBox alternativeCheckBox = (CheckBox) findViewById(R.id.planner_alternative_stops);
        alternativeCheckBox.setChecked(mJourneyQuery.alternativeStops);


    }

    private void updateTimeViews() {
        String formattedDate = mTime.format("%x");
        String formattedTime = mTime.format("%R");
        mDateButton.setText(formattedDate);
        mTimeButton.setText(formattedTime);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DATE:
                return new DatePickerDialog(this, mDateSetListener,
                        mTime.year, mTime.month, mTime.monthDay);
            case DIALOG_TIME:
                // TODO: Base 24 hour on locale, same with the format.
                return new TimePickerDialog(this, mTimeSetListener,
                        mTime.hour, mTime.minute, true);
        }
        return null;
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
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, int monthOfYear,
                                      int dayOfMonth) {
                    mTime.year = year;
                    mTime.month = monthOfYear;
                    mTime.monthDay = dayOfMonth;
                    updateViews();
                }
            };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {

                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    mTime.hour = hourOfDay;
                    mTime.minute = minute;
                    updateViews();
                }
            };


    private ArrayList<String> getSelectedTransportModes() {
        CheckBox transportBus = (CheckBox) findViewById(R.id.planner_transport_bus);
        CheckBox transportMetro = (CheckBox) findViewById(R.id.planner_transport_metro);
        CheckBox transportNar = (CheckBox) findViewById(R.id.planner_transport_nar);
        CheckBox transportTrain = (CheckBox) findViewById(R.id.planner_transport_train);
        CheckBox transportTram = (CheckBox) findViewById(R.id.planner_transport_tram);
        CheckBox transportWax = (CheckBox) findViewById(R.id.planner_transport_wax);

        ArrayList<String> transportModes = new ArrayList<String>();
        if (transportBus.isChecked()) {
            transportModes.add(TransportMode.BUS);
        }
        if (transportMetro.isChecked()) {
            transportModes.add(TransportMode.METRO);
        }
        if (transportNar.isChecked()) {
            transportModes.add(TransportMode.NAR);
        }
        if (transportTrain.isChecked()) {
            transportModes.add(TransportMode.TRAIN);
        }
        if (transportTram.isChecked()) {
            transportModes.add(TransportMode.TRAM);
        }
        if (transportWax.isChecked()) {
            transportModes.add(TransportMode.WAX);
        }

        return transportModes;
    }

    public boolean getUseAlternatives() {
        CheckBox alternativeCheckBox = (CheckBox) findViewById(R.id.planner_alternative_stops);
        return alternativeCheckBox.isChecked();
    }

    private Site buildStop(Site site, AutoCompleteTextView auTextView) {
        if (site.hasName() && site.getName().equals(auTextView.getText().toString())) {
            return site;
        }
        site.fromSite(null);
        return site;
    }

    private DelayAutoCompleteTextView createAutoCompleteTextView(
            int autoCompleteResId, final Site site, boolean includeAddresses) {
        final DelayAutoCompleteTextView autoCompleteTextView = (DelayAutoCompleteTextView)
                findViewById(autoCompleteResId);
        final AutoCompleteStopAdapter stopAdapter = new AutoCompleteStopAdapter(
                this, R.layout.autocomplete_item_2line,
                Planner.getInstance(), includeAddresses);

        autoCompleteTextView.addTextChangedListener(new UpdateStopTextWatcher(site));

        String name = site.getName();
        autoCompleteTextView.setText(name);

        autoCompleteTextView.setSelectAllOnFocus(true);
        autoCompleteTextView.setAdapter(stopAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // Sometime site is not properly set here, where is the reference cleared?
                Site v = stopAdapter.getValue(position);
                site.fromSite(v);
            }
        });

        return autoCompleteTextView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.actionbar_done:
                mJourneyQuery.isTimeDeparture = mWhenSpinner.getSelectedItemId() == 0 ? true : false;
                mJourneyQuery.time = mTime;
                mJourneyQuery.transportModes = getSelectedTransportModes();
                mJourneyQuery.alternativeStops = getUseAlternatives();

                boolean looksValid = true;
                if (!TextUtils.isEmpty(mViaPointAutoComplete.getText())) {
                    mViaPoint = buildStop(mViaPoint, mViaPointAutoComplete);
                    if (!mViaPoint.looksValid()) {
                        Log.d(TAG, "Via was not valid");
                        mViaPointAutoComplete.setError(getText(R.string.empty_value));
                        looksValid = false;
                    }
                    mJourneyQuery.via = JourneyQuery.Builder.buildLocationFromStop(mViaPoint);
                } else {
                    mJourneyQuery.via = null;
                }

                if (looksValid) {
                    setResult(RESULT_OK, (new Intent())
                            .putExtra(RoutesActivity.EXTRA_JOURNEY_QUERY, mJourneyQuery));
                    finish();
                }
                break;
            case R.id.actionbar_discard:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }

    }

    private class UpdateStopTextWatcher implements TextWatcher {
        private final Site mSite;

        public UpdateStopTextWatcher(Site site) {
            mSite = site;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!getString(R.string.my_location).equals(s.toString())
                    || getString(R.string.point_on_map).equals(s.toString())) {
                if (!s.toString().equals(mSite.getName())) {
                    mSite.setName(s.toString());
                    mSite.setId(0);
                    mSite.setType(null);
                    mSite.setLocation(null);
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
            // Needed by interface, but not used.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            // Needed by interface, but not used.
        }

    }
}
