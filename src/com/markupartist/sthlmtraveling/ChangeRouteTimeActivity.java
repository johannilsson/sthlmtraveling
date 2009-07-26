package com.markupartist.sthlmtraveling;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;

public class ChangeRouteTimeActivity extends Activity {
    static final String TAG = "ChangeRouteTimeActivity"; 
    static final int DIALOG_DATE = 0;
    static final int DIALOG_TIME = 1;
    static final int DIALOG_PROGRESS = 2;
    static final int DIALOG_NO_ROUTES_FOUND = 3;
    private Time mTime;
    private Button mDateButton;
    private Button mTimeButton;
    private final Handler mHandler = new Handler();

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
                searchRoutes(startPoint, endPoint, mTime);
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
            case DIALOG_PROGRESS:
                ProgressDialog progress = new ProgressDialog(this);
                progress.setMessage(getText(R.string.loading));
                return progress;
            case DIALOG_NO_ROUTES_FOUND:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                return builder.setTitle("Unfortunately no routes was found")
                    .setMessage("If searhing for an address try adding a house number.")
                    .setCancelable(true)
                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                       }
                    }).create();
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
     * Fires off a thread to do the query. Will call onSearchResult when done.
     * @param startPoint the start point.
     * @param endPoint the end point.
     * @param time the time to base the search on.
     */
    private void searchRoutes(final String startPoint, final String endPoint, 
            final Time time) {
        showDialog(DIALOG_PROGRESS);
        new Thread() {
            public void run() {
                try {
                    Planner.getInstance().findRoutes(startPoint, endPoint, time);
                    mHandler.post(new Runnable() {
                        @Override public void run() {
                            onSearchRoutesResult();
                        }
                    });
                    dismissDialog(DIALOG_PROGRESS);
                } catch (Exception e) {
                    dismissDialog(DIALOG_PROGRESS);
                }
            }
        }.start();
    }

    /**
     * Called when we have a search result for routes.
     */
    private void onSearchRoutesResult() {
        if (Planner.getInstance().lastFoundRoutes() != null 
                && !Planner.getInstance().lastFoundRoutes().isEmpty()) {
            setResult(RESULT_OK, (new Intent())
                    .putExtra("com.markupartist.sthlmtraveling.routeTime", 
                            mTime.format2445()));
            finish();
        } else {
            // TODO: This works for now, but we need to see if there are any
            // alternative stops available in later on.
            showDialog(DIALOG_NO_ROUTES_FOUND);
        }
    }
}
