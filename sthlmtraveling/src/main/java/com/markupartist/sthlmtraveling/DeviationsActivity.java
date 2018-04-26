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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.format.Time;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

import com.markupartist.sthlmtraveling.provider.deviation.Deviation;
import com.markupartist.sthlmtraveling.provider.deviation.DeviationStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DeviationsActivity extends BaseListActivity {
    private static final String STATE_GET_DEVIATIONS_IN_PROGRESS =
        "com.markupartist.sthlmtraveling.getdeviations.inprogress";
    public static final String DEVIATION_FILTER_ACTION =
        "com.markupartist.sthlmtraveling.action.DEVIATION_FILTER";
    static String TAG = "DeviationsActivity";

    private static final int DIALOG_GET_DEVIATIONS_NETWORK_PROBLEM = 1;

    private GetDeviationsTask mGetDeviationsTask;
    private ArrayList<Deviation> mDeviationsResult;
    private ArrayList<Deviation> mAllDeviations;
    private LinearLayout mProgress;
    private TextView mSearchEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.deviations_list);

        registerScreen("Deviations");

        mProgress = (LinearLayout) findViewById(R.id.search_progress);
        mProgress.setVisibility(View.GONE);

        initActionBar();
        if(savedInstanceState != null){
            fillData(savedInstanceState.<Deviation>getParcelableArrayList("deviationsResult"));
        }
        else{
            loadDeviations();
        }

        registerForContextMenu(getListView());

        mSearchEditText = (EditText) this.findViewById(R.id.deviations_search_field);
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //unused
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d(TAG, "onTextChanged");
                //unused
            }

            @Override
            public void afterTextChanged(Editable editable) {
                filterResult(editable.toString());
            }
        });
        mSearchEditText.clearFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_deviations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.actionbar_item_refresh:
            mGetDeviationsTask = new GetDeviationsTask();
            mGetDeviationsTask.execute();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadDeviations() {
        @SuppressWarnings("unchecked")
        final ArrayList<Deviation> result =
            (ArrayList<Deviation>) getLastNonConfigurationInstance();
        if (result != null) {
            fillData(result);
            mAllDeviations = result;
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
                case R.id.deviation_details:
                case R.id.deviation_header:
                case R.id.deviation_created:
                case R.id.deviation_scope:
                    String[] word = mSearchEditText.getText().toString().split(" ");
                    ((TextView)view).setText(highlight(textRepresentation, word));
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
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Deviation deviation = mDeviationsResult.get(menuInfo.position);
        share(deviation.getHeader(), deviation.getDetails().toString());
        return true;
    }

//    @Override
//    public Object onRetainNonConfigurationInstance() {
//        return mDeviationsResult;
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("deviationsResult", mDeviationsResult);
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
     * Cancels the {@link GetDeviationsTask} if it is running.
     */
    private void onCancelGetDeviationsTask() {
        if (mGetDeviationsTask != null &&
                mGetDeviationsTask.getStatus() == AsyncTask.Status.RUNNING) {
            Log.i(TAG, "Cancels GetDeviationsTask.");
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
        //setSupportProgressBarIndeterminateVisibility(true);
    }

    /**
     * Dismiss the progress dialog.
     */
    private void dismissProgress() {
        //setSupportProgressBarIndeterminateVisibility(false);
    }

    @Override
    protected void onDestroy() {
        mSearchEditText.setText("");
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
     * Background job for getting {@link Deviation}s.
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
                return deviationStore.getDeviations(DeviationsActivity.this);
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
                mAllDeviations = result;
            } else {
                try {
                    showDialog(DIALOG_GET_DEVIATIONS_NETWORK_PROBLEM);
                } catch (BadTokenException e) {
                    Log.w(TAG, "Caught BadTokenException when trying to show network error dialog.");
                }
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

    // Pretty ugly solution fix it NOW
    private void filterResult(String search){
        if (mAllDeviations == null) return;
        String[] words = search.toLowerCase().split(" ");
        ArrayList<Deviation> filteredResult = new ArrayList<Deviation>();
        boolean match = false;
        for(Deviation deviation:mAllDeviations){
            for(String word:words){
                boolean checkA = deviation.getHeader().toLowerCase().contains(word);
                boolean checkB = deviation.getDetails().toString().toLowerCase().contains(word);
                boolean checkC = deviation.getScope().toLowerCase().contains(word);
                if(!(checkA||checkB||checkC)){
                    match = false;
                    break;
                }
                else {
                    match = true;
                }
            }
            if (match) {
                filteredResult.add(deviation);
            }
        }
        fillData(filteredResult);
    }

    /***
     * Writed by Team 8, highlight all words contained within a string
     */
    private Spannable highlight(String str, String[] words){
        Spannable spn = new SpannableString(str);
        for(String word:words)
        if(word.length() > 1) {
            str = str.toLowerCase();
            word = word.toLowerCase();
            int index_start = str.indexOf(word);
            while (index_start > -1) {

                BackgroundColorSpan hl = new BackgroundColorSpan(Color.YELLOW);//new TextAppearanceSpan(null, Typeface.BOLD, -1, highlightColor, null);

                spn.setSpan(hl, index_start, index_start + word.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index_start = str.indexOf(word, index_start + word.length());
            }
        }
        return spn;
    }


}
