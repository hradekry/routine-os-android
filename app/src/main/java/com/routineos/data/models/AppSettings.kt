package com.routineos.data.models

import com.google.gson.annotations.SerializedName

data class AppSettings(
    @SerializedName("notificationsEnabled") val notificationsEnabled: Boolean = true,
    @SerializedName("alarmsEnabled") val alarmsEnabled: Boolean = true
)
