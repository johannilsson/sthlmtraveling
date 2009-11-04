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

import java.util.ArrayList;

import com.markupartist.sthlmtraveling.planner.Route;

/**
 * The callback used when there is a search result.
 */
public interface OnSearchRoutesResultListener {
    /**
     * Called when we have a search result.
     * @param routes the routes
     */
    public void onSearchRoutesResult(ArrayList<Route> routes);
}