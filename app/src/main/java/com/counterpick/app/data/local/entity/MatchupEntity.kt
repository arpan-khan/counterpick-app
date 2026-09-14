package com.counterpick.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matchups",
    primaryKeys = ["mainId", "otherId", "source"],
    indices = [Index(value = ["mainId", "source"])]
)
data class MatchupEntity(
    val mainId: Int,
    val otherId: Int,
    val score: Double,
    val rank: Int,
    val source: MatchupSource
)

enum class MatchupSource { BEST, WORST }
