package com.markupartist.sthlmtraveling;

/**
 * Blenda och Filiip
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.Route;

public class AlarmPreferencesActivity extends AppCompatActivity implements View.OnClickListener {


    private Spinner mTimeSpinnerDeparture;
    private Spinner mTimeSpinnerDestination;
    long mTimeDeparture;
    int mTimeDestination;
    boolean mTimeSelectedDeparture = true;
    boolean ismTimeSelectedDestination = true;
    Date mStartDate;
    long mStartTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_preferences);

        /**Get leg time**/
        Intent intent = getIntent();
        Route route = intent.getParcelableExtra("ParceableTest");
        List<Leg> legList = new ArrayList<>();
        legList = route.getLegs();
        List<Date> endTime = new ArrayList<>();

        for(Leg leg : legList ){
            endTime.add(leg.getEndTime());
            Log.v("aznee", leg.getStartTime().toString());
            Log.v("aznee2", leg.getEndTime().toString());

        }

        mStartDate = legList.get(0).getStartTime();
        mStartTime = mStartDate.getTime();
        Log.v("aznee3", String.valueOf(mStartTime));

        /** From ChangeRouteTimeActivity **/
        //Set up spinner departure & destination
        mTimeSpinnerDeparture = (Spinner) findViewById(R.id.time_spinner_departure);
        mTimeSpinnerDestination = (Spinner) findViewById(R.id.time_spinner_destination);
        mTimeSpinnerDeparture.setEnabled(false);
        mTimeSpinnerDestination.setEnabled(false);

        int selectedPosDeparture = 0;
        ArrayAdapter<CharSequence> whenChoiceAdapter = ArrayAdapter.createFromResource(
                this, R.array.time_interval_departure, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeSpinnerDeparture.setAdapter(whenChoiceAdapter);
        mTimeSpinnerDeparture.setSelection(selectedPosDeparture);



        int selectedPosDestination = 0;
        ArrayAdapter<CharSequence> whenChoiceAdapterDestination = ArrayAdapter.createFromResource(
                this, R.array.time_interval_destination, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeSpinnerDestination.setAdapter(whenChoiceAdapterDestination);
        mTimeSpinnerDestination.setSelection(selectedPosDestination);
        /***/


        /** From ChangeRouteTimeActivity **/
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
        /***/

        //Set up checkbox
        final CheckBox mAlarmDepartureCheckBox = (CheckBox) findViewById(R.id.select_alarm_departure);
        final CheckBox mAlarmDestinationCheckBox = (CheckBox) findViewById(R.id.select_alarm_destination);
        final CheckBox mAlarmEveryStopCheckBox = (CheckBox) findViewById(R.id.select_alarm_everyStop);
        mAlarmEveryStopCheckBox.setEnabled(false);

        mAlarmDepartureCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAlarmDepartureCheckBox.isChecked()){
                    mTimeSpinnerDeparture.setEnabled(true);
                } else {
                    mTimeSpinnerDeparture.setEnabled(false);
                }
            }
        });


        mAlarmDestinationCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mAlarmDestinationCheckBox.isChecked()){
                    mTimeSpinnerDestination.setEnabled(true);
                    mAlarmEveryStopCheckBox.setEnabled(true);
                } else {
                    mTimeSpinnerDestination.setEnabled(false);
                    mAlarmEveryStopCheckBox.setEnabled(false);
                }
            }
        });
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.actionbar_done:
                int mSelectedTimeDeparture = (int) mTimeSpinnerDeparture.getSelectedItemId();
                switch (mSelectedTimeDeparture) {
                    case 0:
                        mTimeSelectedDeparture = false;
                    case 1:
                        mTimeDeparture = mStartTime - 120000;
                        break;
                    case 2:
                        mTimeDeparture = mStartTime - 300000;
                        break;
                    case 3:
                        mTimeDeparture = mStartTime - 600000;
                        break;
                    case 4:
                        mTimeDeparture = mStartTime - 900000;
                        break;
                    case 5:
                        mTimeDeparture = mStartTime - 1800000;
                        break;
                }

                int mSelectedTimeDestination = (int) mTimeSpinnerDestination.getSelectedItemId();
                switch (mSelectedTimeDestination){
                    case 0:
                        ismTimeSelectedDestination = false;
                        break;
                    case 1:
                        mTimeDestination = 120;
                        break;
                    case 2:
                        mTimeDestination = 180;
                        break;
                    case 3:
                        mTimeDestination = 300;
                        break;
                }

            if(mTimeSelectedDeparture) {
                Log.d("timececked", "value:" + mTimeDeparture);
                Intent intent = new Intent(AlarmPreferencesActivity.this, Alarm.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mTimeDeparture, pendingIntent);
            }
            if(ismTimeSelectedDestination){
                Log.d("timecehckd", "value:" + mTimeDestination);
            }

            finish();
            break;

            case R.id.actionbar_discard:
                finish();
                break;
        }
    }
}
