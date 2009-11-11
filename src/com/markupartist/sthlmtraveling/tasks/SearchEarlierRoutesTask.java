package com.markupartist.sthlmtraveling.tasks;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.planner.Planner;
import com.markupartist.sthlmtraveling.planner.Route;

public class SearchEarlierRoutesTask extends AbstractSearchRoutesTask {

    public SearchEarlierRoutesTask(Activity activity) {
        super(activity);
    }

    @Override
    ArrayList<Route> doSearchInBackground(Object... params) throws IOException {
        return Planner.getInstance().findEarlierRoutes();
    }

    @Override
    protected void onNoRoutesFound() {
        new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getActivity().getText(R.string.attention_label))
            .setMessage(getActivity().getText(R.string.session_timeout_message))
            .setNeutralButton(getActivity().getText(android.R.string.ok), null)
            .create()
            .show();        
    }
}
