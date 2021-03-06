/*
 * Copyright (C) 2009-2014 Johan Nilsson <http://markupartist.com>
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

package com.markupartist.sthlmtraveling.utils;

import android.content.Context;

public class Analytics {

    private static String ACTION_CLICK = "Click";

    private static Analytics sInstance;
    private final Context mContext;

    public Analytics(final Context context) {
        mContext = context;
    }

    public static Analytics getInstance(final Context context) {
        if (sInstance == null) {
            sInstance = new Analytics(context);
        }
        return sInstance;
    }

    public void registerScreen(final String screenName) {
        // Noop
    }

    public void event(final String category, final String action) {
        event(category, action, null);
    }

    public void event(final String category, final String action, final String label) {
        // Noop
    }

}
