package com.github.nikp123.racunica.util

import android.content.Context
import com.github.nikp123.racunica.R

fun unixTimeToRelativeTime(unixTime: Long, context: Context): String {
    val now = System.currentTimeMillis()
    val duration = ((now - unixTime) / 1000).toInt()
    val res = context.resources

    return when {
        // From the future
        duration < 0                  -> res.getString(R.string.relative_time_future)
        // Less than a minute
        duration < 60                 -> res.getString(R.string.relative_time_very_short)
        // Less than an hour
        duration < 3600               -> res.getQuantityString(
            R.plurals.relative_time_minutes,
            (duration/60),
            (duration/60)
        )
        // Less than a day
        duration < 3600 * 24          -> res.getQuantityString(
            R.plurals.relative_time_hours,
            (duration/3600),
            (duration/3600)
        )
        // Less than a month
        duration < 3600 * 24 * 31     -> res.getQuantityString(
            R.plurals.relative_time_days,
            (duration/(3600*24)),
            (duration/(3600*24))
        )
        // Less than a year
        duration < 3600 * 24 * 365.25 -> res.getQuantityString(
            R.plurals.relative_time_months,
            (duration/(3600*24*30.4375)).toInt(),
            (duration/(3600*24*30.4375)).toInt()
        )
        // More than a year
        else                          -> res.getQuantityString(
            R.plurals.relative_time_years,
            (duration/(3600*24*365.25)).toInt(),
            (duration/(3600*24*365.25)).toInt()
        )
    }
}
