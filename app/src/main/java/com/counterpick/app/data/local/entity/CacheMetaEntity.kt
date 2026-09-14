package com.counterpick.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "cache_meta", primaryKeys = ["id"])
data class CacheMetaEntity(
    val id: Int = 0,
    val generated: String?,
    val lastFetchedAtEpochMs: Long
)
