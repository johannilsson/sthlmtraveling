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
import android.text.format.Time;

import com.markupartist.sthlmtraveling.planner.Planner;
import com.markupartist.sthlmtraveling.planner.Route;

/**
 * Background task for searching for routes.
 */
public class SearchRoutesTask extends AbstractSearchRoutesTask {

    /**
     * Constructs a new SearchRoutesTask
     * @param activity the activity
     */
    public SearchRoutesTask(Activity activity) {
        super(activity);
    }

    @Override
    ArrayList<Route> doSearchInBackground(Object... params) throws IOException {
        return Planner.getInstance().findRoutes((String) params[0], 
                (String) params[1], (Time) params[2]);
    }
}
