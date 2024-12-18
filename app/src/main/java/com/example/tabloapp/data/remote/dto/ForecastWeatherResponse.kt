package com.example.tabloapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ForecastWeatherResponse(
    @SerializedName("now_dt") val nowDt: String,
    @SerializedName("fact") val fact: Fact
)

data class Fact(
    @SerializedName("temp") val temp: Int,
    @SerializedName("condition") val condition: String,
    @SerializedName("icon") val icon: String
)
