package com.github.nikp123.racunica.util

import android.util.Log
import java.time.Instant
import java.time.Duration

fun unixTimeToRelativeTime(unixTime: Long): String {
    val instant = Instant.ofEpochMilli(unixTime)
    val now = Instant.now()

    val duration = Duration.between(instant, now)

    return when {
        duration.seconds < 0     -> "From the future :O"
        duration.toMinutes() < 1 -> "Just now"
        duration.toHours() < 1   -> "${duration.toMinutes()} minutes ago"
        duration.toDays() < 1    -> "${duration.toHours()} hours ago"
        duration.toDays() < 30   -> "${duration.toDays()} days ago"
        duration.toDays() < 365  -> "${duration.toDays() / 30} months ago"
        else                     -> "${duration.toDays() / 365} years ago"
        }
}
