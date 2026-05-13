package com.flights.studio

data class RemoteUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val updates: List<UpdateBlock>
)