package com.markupartist.sthlmtraveling.tasks;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;

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
}
