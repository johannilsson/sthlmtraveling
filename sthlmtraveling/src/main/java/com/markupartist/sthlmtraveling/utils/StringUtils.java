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
import android.text.SpannableStringBuilder;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.provider.planner.Planner;

/**
 * String and text utils related to this project.
 */
public class StringUtils {

    /**
     * Return a styled representation of the my location string.
     *
     * @return A span
     */
    public static SpannableStringBuilder getStyledMyLocationString(final Context context) {
        CharSequence string = context.getText(R.string.my_location);

        SpannableStringBuilder sb = new SpannableStringBuilder(string);
//        ForegroundColorSpan color = new ForegroundColorSpan(0xFF008ED7); // Set color, failed to set through color.xml
//        sb.setSpan(color, 0, sb.length(), SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);

        //sb.append(string);

        return sb;
    }

    /**
     * Helper that returns the my location text representation. If the {@link android.location.Location}
     * is set the accuracy will also be appended.
     *
     * @param stop the stop
     * @return a text representation of my location
     */
    public static CharSequence getMyLocationString(final Context context, final Planner.Location stop) {
        CharSequence string = context.getText(R.string.my_location);
        return string;
    }
}
