package com.markupartist.sthlmtraveling;

/**
 * Blenda Fröjdh & Filip Appelgren
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import com.markupartist.sthlmtraveling.data.models.Place;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.data.models.TravelMode;

public class AlarmPreferencesActivity extends AppCompatActivity implements View.OnClickListener {

    private Spinner mTimeSpinnerDeparture;
    private Spinner mTimeSpinnerDestination;
    private boolean mTimeSelectedDeparture = true;
    private boolean mTimeSelectedDestination = true;
    private CheckBox mAlarmEveryStopCheckBox;
    private long mStartTime;
    private long mEndTime;
    private List<Long> mEndTimeList;
    private List<Place> mStopList;
    private static int mRequestCode = 0;
    private String mMessageDeparture;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_preferences);

        mAlarmEveryStopCheckBox = (CheckBox) findViewById(R.id.select_alarm_everyStop);
        Button mAlarmClear = findViewById(R.id.button2);
        //Set up spinner departure & destination
        mTimeSpinnerDeparture = (Spinner) findViewById(R.id.time_spinner_departure);
        mTimeSpinnerDestination = (Spinner) findViewById(R.id.time_spinner_destination);
        //findViewById(R.id.textClear).setVisibility(View.INVISIBLE);


        mAlarmClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendToast(getString(R.string.alarms_cleared));
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
        mStopList = new ArrayList<>();
        for(Leg leg : legList ){
            if(!leg.getTravelMode().equals(TravelMode.FOOT)){
                mStopList.add(leg.getTo());
                mEndTimeList.add(leg.getEndTime().getTime());

            }
        }


        /** Get final destination time**/
        Date mEndDate = legList.get(legList.size()-1).getEndTime();
        mEndTime = mEndDate.getTime();

        /** Get departure time**/
        Date mStartDate = legList.get(0).getStartTime();
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

        long mTimeDeparture;
        long mTimeDestination;

        long mCurrentTime = System.currentTimeMillis();
        int mSelectedTimeDeparture = (int) mTimeSpinnerDeparture.getSelectedItemId();
        int mSelectedTimeDestination = (int) mTimeSpinnerDestination.getSelectedItemId();
        mTimeDestination = getAlarmTimeDest(mSelectedTimeDestination, mEndTime);
        mTimeDeparture = getAlarmTimeDep(mSelectedTimeDeparture, mStartTime);

        switch (view.getId()) {
            case R.id.actionbar_done:

                if(mCurrentTime < mTimeDeparture) {
                    if (mTimeSelectedDeparture) {
                        setAlarm(mTimeDeparture, getString(R.string.time_to_go), mMessageDeparture);
                        sendToast(getString(R.string.alarm_set));
                    }
                }
                else {
                       sendToast(getString(R.string.invalid_time));
                       break;
                }

                 if(mCurrentTime < mTimeDestination) {

                    if (mTimeSelectedDestination) {
                        setAlarm(mTimeDestination, getString(R.string.you_are_arriving), mStopList.get(mStopList.size() - 1).getName());
                        sendToast(getString(R.string.alarm_set));
                        }

                         /** Set alarm for every stop **/
                         if (mAlarmEveryStopCheckBox.isChecked()) {
                             for (int i = 0; i < mEndTimeList.size(); i++) {
                                 setAlarm(getAlarmTimeDest(mSelectedTimeDestination, mEndTimeList.get(i)), getString(R.string.you_are_arriving), mStopList.get(i).getName());
                             }
                         }
                     }
                 else
                     {
                        sendToast(getString(R.string.invalid_time));
                        break;
                 }

                 finish();

                break;

            case R.id.actionbar_discard:
                finish();
                break;
        }
    }

    private void setAlarm(long time, String notifTitle, String notifMsg) {
        Log.v(notifTitle, notifMsg);
        Bundle bundle = new Bundle();
        bundle.putString("NOTIFICATION_TITLE", notifTitle);
        bundle.putString("NOTIFICATION_TEXT", notifMsg);

        Intent intent = new Intent(AlarmPreferencesActivity.this, Alarm.class);
        intent.putExtras(bundle);

        // intent.putExtra("NOTIFICATION_TITLE", notifTitle);
       // intent.putExtra("NOTIFICATION_MESSAGE", notifMsg);
        mRequestCode++;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), mRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
       long alarmDep = Long.MAX_VALUE;
        switch (selectedTimeDeparture) {
            case 0:
                mTimeSelectedDeparture = false;
                break;
            case 1:
                alarmDep = startTime - 120000;
                mMessageDeparture = getString(R.string.alarm_departure_2min);
                break;
            case 2:
                alarmDep = startTime - 300000;
                mMessageDeparture = getString(R.string.alarm_departure_5min);
                break;
            case 3:
                alarmDep = startTime - 600000;
                mMessageDeparture = getString(R.string.alarm_departure_10min);
                break;
            case 4:
                alarmDep = startTime - 900000;
                mMessageDeparture = getString(R.string.alarm_departure_15min);
                break;
            case 5:
                alarmDep = startTime - 1800000;
                mMessageDeparture = getString(R.string.alarm_departure_30min);
                break;
        }
        return alarmDep;
    }

    /** Get alarm time destination **/
    public long getAlarmTimeDest(int selectedTimeDestination, long destTime){
        long mAlarmDest = Long.MAX_VALUE;
        switch (selectedTimeDestination){
            case 0:
                mTimeSelectedDestination = false;
                break;
            case 1:
                mAlarmDest = destTime - 120000;
                break;
            case 2:
                mAlarmDest = destTime- 300000;
                break;
            case 3:
                mAlarmDest = destTime - 600000;
                break;
        }
        return mAlarmDest;
    }

    public void sendToast(String text){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}

