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
import com.markupartist.sthlmtraveling.planner.Planner;
import com.markupartist.sthlmtraveling.planner.Route;

/**
 * Background task for finding route details
 */
public class FindRouteDetailsTask extends AsyncTask<Route, Void, ArrayList<String>>{
    private Activity mActivity;
    private ProgressDialog mProgress;
    private OnRouteDetailsResultListener mOnRouteDetailsResultListener;
    private OnNoRoutesDetailsResultListener mOnNoResultListener;
    private boolean mWasSuccess = true;

    /**
     * Constructs a new AbstractSearchRoutesTask
     * @param activity the activity
     */
    public FindRouteDetailsTask(Activity activity) {
        mActivity = activity;

        mProgress = new ProgressDialog(mActivity);
        mProgress.setMessage(mActivity.getText(R.string.loading));
    }

    /**
     * Set a listener for search results. 
     * @param listener the listener
     */
    public void setOnRouteDetailsResultListener(OnRouteDetailsResultListener listener) {
        mOnRouteDetailsResultListener = listener;
    }

    /**
     * Set callback for no result. 
     * @param listener the listener
     */
    public void setOnNoResultListener(OnNoRoutesDetailsResultListener listener) {
        this.mOnNoResultListener = listener;
    }

    @Override
    protected ArrayList<String> doInBackground(Route... params) {
        publishProgress();

        try {
            return Planner.getInstance().findRouteDetails(params[0]);
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
    protected void onPostExecute(ArrayList<String> result) {
        mProgress.dismiss();

        if (result != null && !result.isEmpty()) {
            mOnRouteDetailsResultListener.onRouteDetailsResult(result);
        } else if (!mWasSuccess) {
            onNetworkProblem();
        } else {
            if (mOnNoResultListener != null) mOnNoResultListener.onNoRoutesDetailsResult();
        }
    }

    /**
     * Called on network problems. Will display a Dialog with an informative message.
     */
    protected void onNetworkProblem() {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getText(R.string.network_problem_label))
            .setMessage(mActivity.getText(R.string.network_problem_message))
            .setNeutralButton(mActivity.getText(android.R.string.ok), null).create().show();
    }

    /**
     * Result listener for routes details.
     */
    public interface OnRouteDetailsResultListener {
        /**
         * Callback for route details
         * @param details the details
         */
        public void onRouteDetailsResult(ArrayList<String> details);
    }

    /**
     * No result listener for routes details 
     */
    public interface OnNoRoutesDetailsResultListener {
        /**
         * Called when no route details was found. 
         */
        public void onNoRoutesDetailsResult();
    }
}
