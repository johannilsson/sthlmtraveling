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
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.text.BidiFormatter;
import androidx.core.util.Pair;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.markupartist.sthlmtraveling.R;
import com.markupartist.sthlmtraveling.data.models.RealTimeState;
import com.markupartist.sthlmtraveling.data.models.Route;
import com.markupartist.sthlmtraveling.utils.text.SpanUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Date & Time utils
 */
public class DateTimeUtil {
    private static final String TAG = "DateTimeUtil";
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
    private static final SimpleDateFormat HOUR_MINUTE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private static final SimpleDateFormat DATE_TIME_FORMAT_2445 =
            new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);


    public static String format2445(Date date) {
        return DATE_TIME_FORMAT_2445.format(date);
    }

    public static Date parse2445(String dateStr) {
        try {
            return DATE_TIME_FORMAT_2445.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateString In the yy.MM.dd format
     * @param timeString In the HH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    public static Date fromSlDateTime(final String dateString, final String timeString) {
        String dateTime = String.format("%s %s", dateString, timeString);
        Date date = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.US);
        try {
            date = simpleDateFormat.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateString In the yyyy-MM-dd format
     * @param timeString In the HH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    public static Date fromDateTime(final String dateString, final String timeString) {
        String dateTime = String.format("%s %s", dateString, timeString);
        Date date = null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        try {
            date = simpleDateFormat.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateTimeString In the yyyy-MM-ddTHH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    public static Date fromDateTime(@Nullable final String dateTimeString) {
        if (TextUtils.isEmpty(dateTimeString)) {
            return null;
        }
        Date date = null;
        try {
            date = DATE_TIME_FORMAT.parse(dateTimeString);
        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date " + dateTimeString, e);
        }
        return date;
    }

    public static String formatDate(Date date) {
        return DATE_TIME_FORMAT.format(date);
    }

    /**
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     * <p/>
     * From android.text.format.DateUtils
     */
    public static CharSequence formatDuration(final Resources res, final long millis) {
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_hours, hours, hours);
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_minutes, minutes, minutes);
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_seconds, seconds, seconds);
        }
    }

    public static CharSequence formatShortDuration(final Resources res, final long millis) {
        if (millis >= HOUR_IN_MILLIS) {
            final int hours = (int) ((millis + 1800000) / HOUR_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_short_hours, hours, hours);
        } else if (millis >= MINUTE_IN_MILLIS) {
            final int minutes = (int) ((millis + 30000) / MINUTE_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_short_minutes, minutes, minutes);
        } else {
            final int seconds = (int) ((millis + 500) / SECOND_IN_MILLIS);
            return res.getQuantityString(R.plurals.duration_seconds, seconds, seconds);
        }
    }

    /**
     * Format duration without rounding. Seconds are still rounded though.
     *
     * @param resources a resource
     * @param millis    duration in millis
     * @return A string representing the duration
     */
    public static CharSequence formatDetailedDuration(@NonNull final Resources resources, long millis) {
        int days = 0, hours = 0, minutes = 0;
        if (millis > 0) {
            days = (int) TimeUnit.MILLISECONDS.toDays(millis);
            millis -= TimeUnit.DAYS.toMillis(days);
            hours = (int) TimeUnit.MILLISECONDS.toHours(millis);
            millis -= TimeUnit.HOURS.toMillis(hours);
            minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        }
        if (days > 0) {
            return resources.getQuantityString(R.plurals.duration_days, days, days);
        }
        if (hours > 0) {
            if (minutes == 0) {
                return resources.getQuantityString(R.plurals.duration_short_hours, hours, hours);
            }
            return resources.getString(R.string.duration_short_hours_minutes, hours, minutes);
        }
        return resources.getQuantityString(R.plurals.duration_short_minutes, minutes, minutes);
    }

    /**
     * Formats the passed display time to something translated and human readable.
     *
     * @param displayTime The display time, can be null, Nu, HH:MM or in the M min.
     * @param context     A contect
     * @return
     */
    public static CharSequence formatDisplayTime(@Nullable String displayTime,
                                                 @NonNull Context context) {
        // time str can be "Nu", "2 min" or in the "12:00" format, and possible some unknown
        // ones as well.

        // Maybe we should use some memoization func for this.

        // Catch when we should show "Now"
        if (TextUtils.isEmpty(displayTime)
                || TextUtils.equals(displayTime, "0 min")
                || TextUtils.equals(displayTime, "Nu")) {
            return context.getString(R.string.now);
        }
        // Catch "2 min"
        if (displayTime.contains("min")) {
            String minutes = displayTime.replace(" min", "");
            // Try to convert minutes to millis.
            try {
                long minutesConverted = Long.valueOf(minutes);
                return formatShortDuration(context.getResources(), minutesConverted * 60000);
            } catch (NumberFormatException e) {
                return displayTime;
            }
        }
        // Catch the "HH:MM" format.
        try {
            Date date = HOUR_MINUTE_FORMAT.parse(displayTime);
            return DateFormat.getTimeFormat(context).format(date);
        } catch (ParseException e) {
            return displayTime;
        }
    }

    public static CharSequence routeToTimeDisplay(Context context, Route route) {
        java.text.DateFormat format = android.text.format.DateFormat.getTimeFormat(context);
        BidiFormatter bidiFormatter = BidiFormatter.getInstance(RtlUtils.isRtl(Locale.getDefault()));

        Pair<Date, RealTimeState> departsAt = route.departsAt(true);
        Pair<Date, RealTimeState> arrivesAt = route.arrivesAt(true);

        String departsAtStr = format.format(departsAt.first);
        String arrivesAtStr = format.format(arrivesAt.first);
        CharSequence displayTime;
        if (!DateUtils.isToday(departsAt.first.getTime())) {
            displayTime = String.format("%s %s – %s",
                    bidiFormatter.unicodeWrap(DateUtils.getRelativeTimeSpanString(
                            departsAt.first.getTime(), System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS).toString()),
                    bidiFormatter.unicodeWrap(departsAtStr),
                    bidiFormatter.unicodeWrap(arrivesAtStr));
        } else {
            displayTime = String.format("%s – %s",
                    bidiFormatter.unicodeWrap(departsAtStr),
                    bidiFormatter.unicodeWrap(arrivesAtStr));
        }

        ForegroundColorSpan spanDepartsAt = new ForegroundColorSpan(ContextCompat.getColor(
                context, ViewHelper.getTextColorByRealtimeState(departsAt.second)));
        Pattern patternDepartsAt = Pattern.compile(departsAtStr);
        displayTime = SpanUtils.createSpannable(displayTime, patternDepartsAt, spanDepartsAt);

        ForegroundColorSpan spanArrivessAt = new ForegroundColorSpan(ContextCompat.getColor(
                context, ViewHelper.getTextColorByRealtimeState(arrivesAt.second)));
        Pattern patternArrivesAt = Pattern.compile(arrivesAtStr);
        displayTime = SpanUtils.createSpannable(displayTime, patternArrivesAt, spanArrivessAt);

        return displayTime;
    }

    /**
     * Get the delay between a scheduled and expected time.
     * <p/>
     * If running ahead of schedule a negative delay is returned.
     *
     * @param scheduledDateTime The scheduled time
     * @param expectedDateTime The expected time
     * @return Delay in seconds
     */
    public static int getDelay(Date scheduledDateTime, Date expectedDateTime) {
        if (scheduledDateTime == null || expectedDateTime == null) {
            throw new IllegalArgumentException("Scheduled and or excepted date must not be null");
        }
        int delay = (int) (Math.abs(TimeUnit.MILLISECONDS.toSeconds(
                expectedDateTime.getTime() - scheduledDateTime.getTime())));
        if (expectedDateTime.getTime() < scheduledDateTime.getTime()) {
            return -delay;
        }
        return delay;
    }

    /**
     * Get the real-time state based on the passed delay.
     * @param delay The current delay
     * @return The real-time state
     */
    public static RealTimeState getRealTimeStateFromDelay(int delay) {
        if (delay > 0) {
            return RealTimeState.BEHIND_SCHEDULE;
        } else if (delay < 0) {
            return RealTimeState.AHEAD_OF_SCHEDULE;
        }
        return RealTimeState.ON_TIME;
    }
}