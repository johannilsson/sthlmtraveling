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

    public AbstractSearchRoutesTask(Activity activity) {
        mActivity = activity;

        mProgress = new ProgressDialog(mActivity);
        mProgress.setMessage(mActivity.getText(R.string.loading));
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

    abstract ArrayList<Route> doSearchInBackground(Object... params) 
            throws IOException;
    
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

    public void setOnSearchRoutesResultListener(
            OnSearchRoutesResultListener listener) {
        mOnSearchRoutesResultListener = listener;
    }

    protected void onNetworkProblem() {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getText(R.string.network_problem_label))
            .setMessage(mActivity.getText(R.string.network_problem_message))
            .setNeutralButton(mActivity.getText(android.R.string.ok), null).create().show();
    }

    protected void onNoRoutesFound() {
        new AlertDialog.Builder(mActivity)
            .setTitle("Unfortunately no routes was found")
            .setMessage("If searhing for an address try adding a house number.")
            .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
               }
            }).create().show();        
    }
}
