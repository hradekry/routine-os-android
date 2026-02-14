package com.routineos.data.models

import com.google.gson.annotations.SerializedName

data class Task(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("completed") val completed: Boolean = false,
    @SerializedName("date") val date: String,
    @SerializedName("type") val type: String = "onetime", // "onetime" or "recurring"
    @SerializedName("skippedDates") val skippedDates: List<String> = emptyList(),
    @SerializedName("trackable") val trackable: Boolean = false,
    @SerializedName("trackTarget") val trackTarget: Float? = null,
    @SerializedName("trackUnit") val trackUnit: String? = null,
    @SerializedName("trackIncrement") val trackIncrement: Float? = null,
    @SerializedName("trackProgress") val trackProgress: Map<String, Float> = emptyMap()
)
