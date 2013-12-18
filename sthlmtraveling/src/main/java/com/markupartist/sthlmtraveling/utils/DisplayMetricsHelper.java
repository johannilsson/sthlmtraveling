/*
 * Copyright (C) 2010 Johan Nilsson <http://markupartist.com>
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

import java.lang.reflect.Field;

import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Helper for various methods and field on {@link DisplayMetrics} that is not
 * available prior to Android version 1.5.
 * 
 * @author johan
 */
public class DisplayMetricsHelper {
    /**
     * The log tag.
     */
    private static String TAG = "DisplayMetricsHelper";

    /**
     * Standard quantized DPI for low-density screens.
     */
    public static final int DENSITY_LOW = 120;

    /**
     * Standard quantized DPI for medium-density screens.
     */
    public static final int DENSITY_MEDIUM = 160;

    /**
     * Standard quantized DPI for high-density screens.
     */
    public static final int DENSITY_HIGH = 240;

    /**
     * The reference density used throughout the system.
     */
    public static final int DENSITY_DEFAULT = 160;

    /**
     * Get the density dpi from the passed metrics.
     * 
     * {@link DisplayMetrics} must be initialized like this: 
     * 
     * <pre>
     * DisplayMetrics metrics = new DisplayMetrics();
     * getWindowManager().getDefaultDisplay().getMetrics(metrics);
     * </pre>
     * 
     * Reflection is used internally to deal with older devices that does not
     * have density values set on {@link DisplayMetrics}. The return value is
     * the same as {@link DisplayMetrics} would return since 1.6.
     * 
     * @param initialized {@link DisplayMetrics}
     * @return the density dpi
     */
    public static int getDensityDpi(DisplayMetrics metrics) {
        int density = DENSITY_MEDIUM;
        try {
            Field densityDpiFiled = DisplayMetrics.class.getField("densityDpi");
            switch (densityDpiFiled.getInt(metrics)) {
            case DisplayMetrics.DENSITY_HIGH:
                density = DENSITY_HIGH;
                break;
            case DisplayMetrics.DENSITY_LOW:
                density = DENSITY_LOW;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                density = DENSITY_MEDIUM;
                break;
            }
        } catch (NoSuchFieldException e) {
            // Just pass, we are dealing with an older device.
            Log.d(TAG, "Older device, using DENSITY_MEDIUM.");
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Illegal argument, " + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.w(TAG, "Illegal access, " + e.getMessage());
        }

        return density;
    }
}
