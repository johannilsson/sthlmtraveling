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

package com.markupartist.sthlmtraveling.tasks;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.planner.Route;

/**
 * Background task for searching for routes.
 */
public abstract class AbstractSearchRoutesTask extends AsyncTask<Object, Void, ArrayList<Route>>{
    private Activity mActivity;
    private ProgressDialog mProgress;
    private OnSearchRoutesResultListener mOnSearchRoutesResultListener;
    private boolean mWasSuccess = true;

    /**
     * Constructs a new AbstractSearchRoutesTask
     * @param activity the activity
     */
    public AbstractSearchRoutesTask(Activity activity) {
        mActivity = activity;

        mProgress = new ProgressDialog(mActivity);
        mProgress.setMessage(mActivity.getText(R.string.loading));
    }

    /**
     * Performs the search in a background thread.
     * @param params The search parameters
     * @return list of routes
     * @throws IOException thrown on network problems
     */
    abstract ArrayList<Route> doSearchInBackground(Object... params) 
            throws IOException;

    /**
     * Set a listener for search results. 
     * @param listener the listener
     */
    public void setOnSearchRoutesResultListener(
            OnSearchRoutesResultListener listener) {
        mOnSearchRoutesResultListener = listener;
    }

    @Override
    protected ArrayList<Route> doInBackground(Object... params) {
        publishProgress();

        try {
            return doSearchInBackground(params);
        } catch (IOException e) {
            mWasSuccess = false;
            return null;
        }
    }
    
    @Override
    public void onProgressUpdate(Void... values) {
        mProgress.show();
    }

    @Override
    protected void onPostExecute(ArrayList<Route> result) {
        mProgress.dismiss();

        if (result != null && !result.isEmpty()) {
            mOnSearchRoutesResultListener.onSearchRoutesResult(result);
        } else if (!mWasSuccess) {
            onNetworkProblem();
        } else {
            onNoRoutesFound();
        }
    }

    /**
     * Called on network problems. Will display a Dialog with an informative message.
     * </p>
     * Implementations are free to override this.
     */
    protected void onNetworkProblem() {
        new AlertDialog.Builder(mActivity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(mActivity.getText(R.string.network_problem_label))
            .setMessage(mActivity.getText(R.string.network_problem_message))
            .setNeutralButton(mActivity.getText(android.R.string.ok), null)
            .create()
            .show();
    }

    /**
     * Called if no routes was found. Will display a Dialog with an informative message.
     * </p>
     * Implementations are free to override this.
     */
    protected void onNoRoutesFound() {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getText(R.string.no_routes_found_label))
            .setMessage(mActivity.getText(R.string.no_routes_found_message))
            .setNeutralButton(mActivity.getText(android.R.string.ok), null)
            .create()
            .show();        
    }
}
