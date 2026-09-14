package com.counterpick.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "meta_tiers", primaryKeys = ["heroId"])
data class MetaTierEntity(
    val heroId: Int,
    val tier: String
)

@Entity(tableName = "meta_credit", primaryKeys = ["id"])
data class MetaCreditEntity(
    val id: Int = 0,
    val text: String,
    val url: String
)
