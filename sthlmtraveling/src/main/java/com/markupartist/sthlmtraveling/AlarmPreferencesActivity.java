package com.markupartist.sthlmtraveling;

/**
 * Blenda och Filip
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.markupartist.sthlmtraveling.data.models.Leg;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.TravelMode;

public class AlarmPreferencesActivity extends AppCompatActivity implements View.OnClickListener {

    private Spinner mTimeSpinnerDeparture;
    private Spinner mTimeSpinnerDestination;
    private long mTimeDeparture;
    private long mTimeDestination;
    private boolean mTimeSelectedDeparture = true;
    private boolean mTimeSelectedDestination = true;
    private CheckBox mAlarmEveryStopCheckBox;
    private Date mStartDate;
    private long mStartTime;
    private Date mEndDate;
    private long mEndTime;
    private List<Long> mEndTimeList;
    private static int mRequestCode = 0;
    private Button alarmClear;
    private String mTimeBeforeDestination;
    private String mTimeBeforeDeparture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_preferences);

        mAlarmEveryStopCheckBox = (CheckBox) findViewById(R.id.select_alarm_everyStop);
        alarmClear = findViewById(R.id.button2);
        //Set up spinner departure & destination
        mTimeSpinnerDeparture = (Spinner) findViewById(R.id.time_spinner_departure);
        mTimeSpinnerDestination = (Spinner) findViewById(R.id.time_spinner_destination);
        alarmClear.setVisibility(View.INVISIBLE);
        //findViewById(R.id.textClear).setVisibility(View.INVISIBLE);

        if (mRequestCode != 0){
            alarmClear.setVisibility(View.VISIBLE);
            //findViewById(R.id.textClear).setVisibility(View.VISIBLE);
        }

        alarmClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alarmClear.setVisibility(View.INVISIBLE);
                //findViewById(R.id.textClear).setVisibility(View.INVISIBLE);
                mTimeSpinnerDeparture.setEnabled(true);
                mTimeSpinnerDestination.setEnabled(true);
                mAlarmEveryStopCheckBox.setEnabled(true);
               for(int i = 0; i < mRequestCode; i++){
                   cancelAlarm(i);
                }
                mRequestCode = 0;
            }
        });

        /** Get legs **/
        Intent intent = getIntent();
        Route route = intent.getParcelableExtra("ParceableTest");
        List<Leg> legList = route.getLegs();

        /** Get every destination time **/
        mEndTimeList = new ArrayList<>();
        for(Leg leg : legList ){
            if(!leg.getTravelMode().equals(TravelMode.FOOT)){
                mEndTimeList.add(leg.getEndTime().getTime());
            }
        }

        /** Get final destination time**/
        mEndDate = legList.get(legList.size()-1).getEndTime();
        mEndTime = mEndDate.getTime();

        /** Get departure time**/
        mStartDate = legList.get(0).getStartTime();
        mStartTime = mStartDate.getTime();

        /** From ChangeRouteTimeActivity **/
        int selectedPosDeparture = 0;
        ArrayAdapter<CharSequence> whenChoiceAdapter = ArrayAdapter.createFromResource(
                this, R.array.time_interval_departure, android.R.layout.simple_spinner_item);
        whenChoiceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeSpinnerDeparture.setAdapter(whenChoiceAdapter);
        mTimeSpinnerDeparture.setSelection(selectedPosDeparture);

        int selectedPosDestination = 0;
        ArrayAdapter<CharSequence> whenChoiceAdapterDestination = ArrayAdapter.createFromResource(
                this, R.array.time_interval_destination, android.R.layout.simple_spinner_item);
        whenChoiceAdapterDestination.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.actionbar_done:
                int mSelectedTimeDeparture = (int) mTimeSpinnerDeparture.getSelectedItemId();
                mTimeDeparture = getAlarmTimeDep(mSelectedTimeDeparture, mStartTime);

                int mSelectedTimeDestination = (int) mTimeSpinnerDestination.getSelectedItemId();
                mTimeDestination = getAlarmTimeDest(mSelectedTimeDestination, mEndTime);

                if (mTimeSelectedDeparture) {
                    setAlarm(mTimeDeparture, getString(R.string.time_to_go), mTimeBeforeDeparture);
                    sendToast();
                }
                if (mTimeSelectedDestination) {
                    setAlarm(mTimeDestination, getString(R.string.time_to_get_off), mTimeBeforeDestination);
                    sendToast();
                }

                /** Set alarm for every stop **/
                if(mAlarmEveryStopCheckBox.isChecked()){
                    for(long time : mEndTimeList) {
                      setAlarm(getAlarmTimeDest(mSelectedTimeDestination, time), getString(R.string.time_to_get_off), mTimeBeforeDestination);
                    }
                }

                finish();
                break;

            case R.id.actionbar_discard:
                finish();
                break;
        }
    }

    private void setAlarm(long time, String notifTitle, String notifMsg){
        Intent intent = new Intent(AlarmPreferencesActivity.this, Alarm.class);
        intent.putExtra("NOTIFICATION_TITLE", notifTitle);
        intent.putExtra("NOTIFICATION_MESSAGE", notifMsg );
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), mRequestCode, intent, 0);
        mRequestCode++;
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
    }

    private void cancelAlarm(int i){
        Context ctx = getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, Alarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(ctx, i, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    /** Get alarm time departure **/
    public long getAlarmTimeDep(int selectedTimeDeparture, long startTime) {
       long alarmDep = 0;
        switch (selectedTimeDeparture) {
            case 0:
                mTimeSelectedDeparture = false;
            case 1:
                alarmDep = startTime - 120000;
                mTimeBeforeDeparture = getString(R.string.alarm_departure_2min);
                break;
            case 2:
                alarmDep = startTime - 300000;
                mTimeBeforeDeparture = getString(R.string.alarm_departure_5min);
                break;
            case 3:
                alarmDep = startTime - 600000;
                mTimeBeforeDeparture = getString(R.string.alarm_departure_10min);
                break;
            case 4:
                alarmDep = startTime - 900000;
                mTimeBeforeDeparture = getString(R.string.alarm_departure_15min);
                break;
            case 5:
                alarmDep = startTime - 1800000;
                mTimeBeforeDeparture = getString(R.string.alarm_departure_30min);
                break;
        }
        return alarmDep;
    }

    /** Get alarm time destination **/
    public long getAlarmTimeDest(int selectedTimeDestination, long destTime){
        long mAlarmDest = 0;
        switch (selectedTimeDestination){
            case 0:
                mTimeSelectedDestination = false;
                break;
            case 1:
                mAlarmDest = destTime - 60000;
                mTimeBeforeDestination = getString(R.string.alarm_destination_1min);
                break;
            case 2:
                mAlarmDest = destTime- 180000;
                mTimeBeforeDestination = getString(R.string.alarm_destination_3min);
                break;
            case 3:
                mAlarmDest = destTime - 300000;
                mTimeBeforeDestination = getString(R.string.alarm_destination_5min);
                break;
        }
        return mAlarmDest;
    }

    public void sendToast(){
        Context context = getApplicationContext();
        String text = getString(R.string.alarm_set);
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}

