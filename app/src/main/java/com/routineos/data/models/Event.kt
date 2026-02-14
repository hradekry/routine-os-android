package com.routineos.data.models

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("time") val time: String? = null,
    @SerializedName("type") val type: String = "onetime", // "onetime" or "recurring"
    @SerializedName("date") val date: String,
    @SerializedName("alarmEnabled") val alarmEnabled: Boolean = false,
    @SerializedName("alarmTimestamp") val alarmTimestamp: Long? = null,
    @SerializedName("alarmNotified") val alarmNotified: Boolean = false
)
