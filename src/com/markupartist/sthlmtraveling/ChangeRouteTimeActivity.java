package com.markupartist.sthlmtraveling;

import java.util.ArrayList;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class ChangeRouteTimeActivity extends Activity {
    static final String TAG = "ChangeRouteTimeActivity"; 
    static final int DIALOG_DATE = 0;
    static final int DIALOG_TIME = 1;
    private Time mTime;
    private Button mDateButton;
    private Button mTimeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_route_time);
        setTitle(getText(R.string.change_date_and_time));

        Bundle extras = getIntent().getExtras();
        final String startPoint = extras.getString("com.markupartist.sthlmtraveling.startPoint");
        final String endPoint = extras.getString("com.markupartist.sthlmtraveling.endPoint");
        String timeString = extras.getString("com.markupartist.sthlmtraveling.routeTime");

        mTime = new Time();
        mTime.parse(timeString);

        mDateButton = (Button) findViewById(R.id.change_route_date);
        mDateButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                showDialog(DIALOG_DATE);
            }
        });

        mTimeButton = (Button) findViewById(R.id.change_route_time);
        mTimeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                showDialog(DIALOG_TIME);
            }
        });

        Button changeButton = (Button) findViewById(R.id.change_route_time_change);
        changeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                SearchRoutesTask task = new SearchRoutesTask();
                task.execute(startPoint, endPoint);

                setResult(RESULT_OK, (new Intent())
                        .putExtra("com.markupartist.sthlmtraveling.routeTime", 
                                mTime.format2445()));
                finish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.change_route_time_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        updateDisplay();
    }

    /**
     * Update time on the buttons.
     */
    private void updateDisplay() {
        String formattedDate = mTime.format("%x");
        String formattedTime = mTime.format("%R");
        mDateButton.setText(formattedDate);
        mTimeButton.setText(formattedTime);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DATE:
                return new DatePickerDialog(this,
                            mDateSetListener,
                            mTime.year, mTime.month, mTime.monthDay);
            case DIALOG_TIME:
                // TODO: Base 24 hour on locale, same with the format.
                return new TimePickerDialog(this,
                        mTimeSetListener, mTime.hour, mTime.minute, true);
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

    private DatePickerDialog.OnDateSetListener mDateSetListener =
        new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear,
                    int dayOfMonth) {
                mTime.year = year;
                mTime.month = monthOfYear;
                mTime.monthDay = dayOfMonth;
                updateDisplay();
            }
        };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener =
        new TimePickerDialog.OnTimeSetListener() {

            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mTime.hour = hourOfDay;
                mTime.minute = minute;
                updateDisplay();
            }
        };

    /**
     * Search for routes.
     */
    private class SearchRoutesTask extends AsyncTask<String, Void, ArrayList<Route>> {
        private ProgressDialog mProgressDialog;
        @Override
        protected ArrayList<Route> doInBackground(String... params) {
            publishProgress();
            ArrayList<Route> routes = Planner.getInstance().findRoutes(params[0], params[1]);
            return routes;
        }

        @Override
        protected void onProgressUpdate(Void... progress) {
            if (mProgressDialog == null) {
                mProgressDialog = 
                    ProgressDialog.show(ChangeRouteTimeActivity.this, "", getText(R.string.loading), true);
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Route> routes) {
            mProgressDialog.dismiss();
        }
    }
}
