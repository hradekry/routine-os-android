package com.routineos.data

import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

fun formatDateKey(date: Date): String {
    return dateFormat.format(date)
}

fun Date.toISOString(): String {
    return isoFormat.format(this)
}
