package com.example.tabloapp.data.model

data class DeviceStatus(
    val device_id: String,
    val status: String,
    val last_seen: String,
    val message: String,
    val brightness: Int,
    val temperature: Int,
    val free_space: Long,
    val uptime: Long,
    )