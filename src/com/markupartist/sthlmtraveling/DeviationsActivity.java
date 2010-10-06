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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;

import com.markupartist.sthlmtraveling.provider.departure.Departure;
import com.markupartist.sthlmtraveling.provider.deviation.Deviation;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;

public class DeviationsActivity extends BaseListActivity {
    private static final String STATE_GET_DEVIATIONS_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getdeviations.inprogress";
    public static final String DEVIATION_FILTER_ACTION =
        "com.markupartist.sthlmtraveling.action.DEVIATION_FILTER";
    static String TAG = "DeviationsActivity";

    private static final int DIALOG_GET_DEVIATIONS_NETWORK_PROBLEM = 1;

    //private ProgressDialog mProgress;
    private GetDeviationsTask mGetDeviationsTask;
    private ArrayList<Deviation> mDeviationsResult;
    private LinearLayout mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviations_list);

        registerEvent("Deviations");

        mProgress = (LinearLayout) findViewById(R.id.search_progress);
        mProgress.setVisibility(View.GONE);

        loadDeviations();
        registerForContextMenu(getListView());
    }

    private void loadDeviations() {
        @SuppressWarnings("unchecked")
        final ArrayList<Deviation> result =
            (ArrayList<Deviation>) getLastNonConfigurationInstance();
        if (result != null) {
            fillData(result);
        } else {
            mGetDeviationsTask = new GetDeviationsTask();
            mGetDeviationsTask.execute();
        }
    }

    private void fillData(ArrayList<Deviation> result) {
        TextView emptyResultView = (TextView) findViewById(R.id.deviations_empty_result);

        // TODO: Needs to be moved later on...
        if (DEVIATION_FILTER_ACTION.equals(getIntent().getAction())) {
            SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
            String filterString = sharedPreferences.getString("notification_deviations_lines_csv", "");
            ArrayList<Integer> triggerFor = DeviationStore.extractLineNumbers(filterString, null);
            result = DeviationStore.filterByLineNumbers(result, triggerFor);
        }

        if (result.isEmpty()) {
            Log.d(TAG, "is empty");
            emptyResultView.setVisibility(View.VISIBLE);
            return;
        }
        emptyResultView.setVisibility(View.GONE);

        SimpleAdapter deviationAdapter = createAdapter(result);

        mDeviationsResult = result;

        setListAdapter(deviationAdapter);
    }

    private SimpleAdapter createAdapter(ArrayList<Deviation> deviations) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();

        Time now = new Time();
        now.setToNow();
        for (Deviation deviation : deviations) {
            Time created = deviation.getCreated();

            Map<String, String> map = new HashMap<String, String>();
            map.put("header", deviation.getHeader());
            map.put("details", deviation.getDetails());
            map.put("created", created.format("%F %R"));
            map.put("scope", deviation.getScope());
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, 
                R.layout.deviations_row,
                new String[] { "header", "details", "created", "scope"},
                new int[] { 
                    R.id.deviation_header,
                    R.id.deviation_details,
                    R.id.deviation_created,
                    R.id.deviation_scope
                }
        );

        adapter.setViewBinder(new ViewBinder() {
            @Override
            public boolean setViewValue(View view, Object data,
                    String textRepresentation) {
                switch (view.getId()) {
                case R.id.deviation_header:
                case R.id.deviation_details:
                case R.id.deviation_created:
                case R.id.deviation_scope:
                    ((TextView)view).setText(textRepresentation);
                    return true;
                }
                return false;
            }
        });

        return adapter;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(R.string.share_label);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Deviation deviation = mDeviationsResult.get(menuInfo.position);
        share(deviation.getHeader(), deviation.getDetails());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu_deviations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                new GetDeviationsTask().execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mDeviationsResult;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveGetDeviationsTask(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
    }

    /**
     * Restores any local state, if any.
     * @param savedInstanceState the bundle containing the saved state
     */
    private void restoreLocalState(Bundle savedInstanceState) {
        restoreGetDeviationsTask(savedInstanceState);
    }

    /**
     * Cancels the {@link GetDeparturesTask} if it is running.
     */
    private void onCancelGetDeviationsTask() {
        if (mGetDeviationsTask != null &&
                mGetDeviationsTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels GetDeparturesTask.");
            mGetDeviationsTask.cancel(true);
            mGetDeviationsTask= null;
        }
    }

    /**
     * Restores the {@link GetDeviationsTask}.
     * @param savedInstanceState the saved state
     */
    private void restoreGetDeviationsTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_GET_DEVIATIONS_IN_PROGRESS)) {
            Log.d(TAG, "restoring getDeviationsTask");
            mGetDeviationsTask = new GetDeviationsTask();
            mGetDeviationsTask.execute();
        }
    }

    /**
     * Saves the state of {@link GetDeviationsTask}.
     * @param outState
     */
    private void saveGetDeviationsTask(Bundle outState) {
        final GetDeviationsTask task = mGetDeviationsTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            Log.d(TAG, "saving GetDeviationsState");
            task.cancel(true);
            mGetDeviationsTask = null;
            outState.putBoolean(STATE_GET_DEVIATIONS_IN_PROGRESS, true);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case DIALOG_GET_DEVIATIONS_NETWORK_PROBLEM:
            return DialogHelper.createNetworkProblemDialog(this, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mGetDeviationsTask = new GetDeviationsTask();
                    mGetDeviationsTask.execute();
                }
            });
        }
        return null;
    }

    /**
     * Show progress dialog.
     */
    private void showProgress() {
        mProgress.setVisibility(View.VISIBLE);
        /*
        if (mProgress == null) {
            mProgress = new ProgressDialog(this);
            mProgress.setMessage(getText(R.string.loading));
            mProgress.show();   
        }
        */
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        mProgress.setVisibility(View.GONE);
        /*
        if (mProgress != null) {
            mProgress.dismiss();
            mProgress = null;
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        onCancelGetDeviationsTask();

        dismissProgress();
    }

    @Override
    public boolean onSearchRequested() {
        Intent i = new Intent(this, StartActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        return true;
    }

    /**
     * Background job for getting {@link Departure}s.
     */
    private class GetDeviationsTask extends AsyncTask<Void, Void, ArrayList<Deviation>> {
        private boolean mWasSuccess = true;

        @Override
        public void onPreExecute() {
            showProgress();
        }

        @Override
        protected ArrayList<Deviation> doInBackground(Void... params) {
            try {
                DeviationStore deviationStore = new DeviationStore();
                return deviationStore.getDeviations();
            } catch (IOException e) {
                mWasSuccess = false;
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Deviation> result) {
            dismissProgress();

            if (mWasSuccess) {
                fillData(result);
            } else {
                showDialog(DIALOG_GET_DEVIATIONS_NETWORK_PROBLEM);
            }
        }
    }

    /**
     * Share a {@link Deviation} with others.
     * @param subject the subject
     * @param text the text
     */
    public void share(String subject,String text) {
        final Intent intent = new Intent(Intent.ACTION_SEND);

        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);

        startActivity(Intent.createChooser(intent, getText(R.string.share_label)));
    }
}
