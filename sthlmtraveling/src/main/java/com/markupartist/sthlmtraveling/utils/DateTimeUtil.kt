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

package com.markupartist.sthlmtraveling.utils

import android.content.Context
import android.content.res.Resources
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.text.BidiFormatter
import androidx.core.util.Pair
import com.markupartist.sthlmtraveling.R
import com.markupartist.sthlmtraveling.data.models.RealTimeState
import com.markupartist.sthlmtraveling.data.models.Route
import com.markupartist.sthlmtraveling.utils.text.SpanUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Date & Time utils
 */
object DateTimeUtil {
    private const val TAG = "DateTimeUtil"
    const val SECOND_IN_MILLIS: Long = 1000
    const val MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60
    const val HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60
    const val DAY_IN_MILLIS = HOUR_IN_MILLIS * 24
    const val WEEK_IN_MILLIS = DAY_IN_MILLIS * 7

    private val HOUR_MINUTE_FORMAT = SimpleDateFormat("HH:mm", Locale.US)
    private val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val DATE_TIME_FORMAT_2445 = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)

    @JvmStatic
    fun format2445(date: Date): String {
        return DATE_TIME_FORMAT_2445.format(date)
    }

    @JvmStatic
    fun parse2445(dateStr: String): Date? {
        return try {
            DATE_TIME_FORMAT_2445.parse(dateStr)
        } catch (e: ParseException) {
            null
        }
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateString In the yy.MM.dd format
     * @param timeString In the HH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    @JvmStatic
    fun fromSlDateTime(dateString: String, timeString: String): Date? {
        val dateTime = String.format("%s %s", dateString, timeString)
        val simpleDateFormat = SimpleDateFormat("dd.MM.yy HH:mm", Locale.US)
        return try {
            simpleDateFormat.parse(dateTime)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateString In the yyyy-MM-dd format
     * @param timeString In the HH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    @JvmStatic
    fun fromDateTime(dateString: String, timeString: String): Date? {
        val dateTime = String.format("%s %s", dateString, timeString)
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return try {
            simpleDateFormat.parse(dateTime)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Constructs a Date from the provided date and time.
     *
     * @param dateTimeString In the yyyy-MM-ddTHH:mm format
     * @return A Date or null if failed to process the provided strings.
     */
    @JvmStatic
    fun fromDateTime(dateTimeString: String?): Date? {
        if (TextUtils.isEmpty(dateTimeString)) {
            return null
        }
        return try {
            DATE_TIME_FORMAT.parse(dateTimeString)
        } catch (e: ParseException) {
            Log.e(TAG, "Failed to parse date $dateTimeString", e)
            null
        }
    }

    @JvmStatic
    fun formatDate(date: Date): String {
        return DATE_TIME_FORMAT.format(date)
    }

    /**
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     *
     * From android.text.format.DateUtils
     */
    @JvmStatic
    fun formatDuration(res: Resources, millis: Long): CharSequence {
        return when {
            millis >= HOUR_IN_MILLIS -> {
                val hours = ((millis + 1800000) / HOUR_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_hours, hours, hours)
            }
            millis >= MINUTE_IN_MILLIS -> {
                val minutes = ((millis + 30000) / MINUTE_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
            }
            else -> {
                val seconds = ((millis + 500) / SECOND_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_seconds, seconds, seconds)
            }
        }
    }

    @JvmStatic
    fun formatShortDuration(res: Resources, millis: Long): CharSequence {
        return when {
            millis >= HOUR_IN_MILLIS -> {
                val hours = ((millis + 1800000) / HOUR_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_short_hours, hours, hours)
            }
            millis >= MINUTE_IN_MILLIS -> {
                val minutes = ((millis + 30000) / MINUTE_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_short_minutes, minutes, minutes)
            }
            else -> {
                val seconds = ((millis + 500) / SECOND_IN_MILLIS).toInt()
                res.getQuantityString(R.plurals.duration_seconds, seconds, seconds)
            }
        }
    }

    /**
     * Format duration without rounding. Seconds are still rounded though.
     *
     * @param resources a resource
     * @param millis    duration in millis
     * @return A string representing the duration
     */
    @JvmStatic
    fun formatDetailedDuration(resources: Resources, millis: Long): CharSequence {
        var remaining = millis
        val days: Int
        val hours: Int
        val minutes: Int

        if (remaining > 0) {
            days = TimeUnit.MILLISECONDS.toDays(remaining).toInt()
            remaining -= TimeUnit.DAYS.toMillis(days.toLong())
            hours = TimeUnit.MILLISECONDS.toHours(remaining).toInt()
            remaining -= TimeUnit.HOURS.toMillis(hours.toLong())
            minutes = TimeUnit.MILLISECONDS.toMinutes(remaining).toInt()
        } else {
            days = 0
            hours = 0
            minutes = 0
        }

        return when {
            days > 0 -> resources.getQuantityString(R.plurals.duration_days, days, days)
            hours > 0 -> {
                if (minutes == 0) {
                    resources.getQuantityString(R.plurals.duration_short_hours, hours, hours)
                } else {
                    resources.getString(R.string.duration_short_hours_minutes, hours, minutes)
                }
            }
            else -> resources.getQuantityString(R.plurals.duration_short_minutes, minutes, minutes)
        }
    }

    /**
     * Formats the passed display time to something translated and human readable.
     *
     * @param displayTime The display time, can be null, Nu, HH:MM or in the M min.
     * @param context     A context
     * @return
     */
    @JvmStatic
    fun formatDisplayTime(displayTime: String?, context: Context): CharSequence {
        // time str can be "Nu", "2 min" or in the "12:00" format, and possible some unknown
        // ones as well.

        // Maybe we should use some memoization func for this.

        // Catch when we should show "Now"
        if (TextUtils.isEmpty(displayTime) ||
            TextUtils.equals(displayTime, "0 min") ||
            TextUtils.equals(displayTime, "Nu")
        ) {
            return context.getString(R.string.now)
        }

        // Catch "2 min"
        if (displayTime!!.contains("min")) {
            val minutes = displayTime.replace(" min", "")
            // Try to convert minutes to millis.
            return try {
                val minutesConverted = minutes.toLong()
                formatShortDuration(context.resources, minutesConverted * 60000)
            } catch (e: NumberFormatException) {
                displayTime
            }
        }

        // Catch the "HH:MM" format.
        return try {
            val date = HOUR_MINUTE_FORMAT.parse(displayTime)
            DateFormat.getTimeFormat(context).format(date)
        } catch (e: ParseException) {
            displayTime
        }
    }

    @JvmStatic
    fun routeToTimeDisplay(context: Context, route: Route): CharSequence {
        val format = DateFormat.getTimeFormat(context)
        val bidiFormatter = BidiFormatter.getInstance(RtlUtils.isRtl(Locale.getDefault()))

        val departsAt = route.departsAt(true)
        val arrivesAt = route.arrivesAt(true)

        val departsAtStr = format.format(departsAt.first)
        val arrivesAtStr = format.format(arrivesAt.first)

        var displayTime: CharSequence = if (!DateUtils.isToday(departsAt.first.time)) {
            String.format(
                "%s %s – %s",
                bidiFormatter.unicodeWrap(
                    DateUtils.getRelativeTimeSpanString(
                        departsAt.first.time, System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS
                    ).toString()
                ),
                bidiFormatter.unicodeWrap(departsAtStr),
                bidiFormatter.unicodeWrap(arrivesAtStr)
            )
        } else {
            String.format(
                "%s – %s",
                bidiFormatter.unicodeWrap(departsAtStr),
                bidiFormatter.unicodeWrap(arrivesAtStr)
            )
        }

        val spanDepartsAt = ForegroundColorSpan(
            ContextCompat.getColor(
                context, ViewHelper.getTextColorByRealtimeState(departsAt.second)
            )
        )
        val patternDepartsAt = Pattern.compile(departsAtStr)
        displayTime = SpanUtils.createSpannable(displayTime, patternDepartsAt, spanDepartsAt)

        val spanArrivesAt = ForegroundColorSpan(
            ContextCompat.getColor(
                context, ViewHelper.getTextColorByRealtimeState(arrivesAt.second)
            )
        )
        val patternArrivesAt = Pattern.compile(arrivesAtStr)
        displayTime = SpanUtils.createSpannable(displayTime, patternArrivesAt, spanArrivesAt)

        return displayTime
    }

    /**
     * Get the delay between a scheduled and expected time.
     *
     * If running ahead of schedule a negative delay is returned.
     *
     * @param scheduledDateTime The scheduled time
     * @param expectedDateTime The expected time
     * @return Delay in seconds
     */
    @JvmStatic
    fun getDelay(scheduledDateTime: Date?, expectedDateTime: Date?): Int {
        if (scheduledDateTime == null || expectedDateTime == null) {
            throw IllegalArgumentException("Scheduled and or excepted date must not be null")
        }
        val delay = TimeUnit.MILLISECONDS.toSeconds(
            Math.abs(expectedDateTime.time - scheduledDateTime.time)
        ).toInt()
        return if (expectedDateTime.time < scheduledDateTime.time) {
            -delay
        } else {
            delay
        }
    }

    /**
     * Get the real-time state based on the passed delay.
     * @param delay The current delay
     * @return The real-time state
     */
    @JvmStatic
    fun getRealTimeStateFromDelay(delay: Int): RealTimeState {
        return when {
            delay > 0 -> RealTimeState.BEHIND_SCHEDULE
            delay < 0 -> RealTimeState.AHEAD_OF_SCHEDULE
            else -> RealTimeState.ON_TIME
        }
    }
}
