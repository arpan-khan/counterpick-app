package com.counterpick.app.scoring

import com.counterpick.app.data.local.entity.MatchupEntity
import com.counterpick.app.data.local.entity.MatchupSource

data class Matchup(val other: Int, val score: Double, val rank: Int)

data class MainRecord(
    val beats: List<Matchup> = emptyList(),
    val beatenBy: List<Matchup> = emptyList()
)

fun buildByMain(rows: List<MatchupEntity>): Map<Int, MainRecord> {
    val beats = mutableMapOf<Int, MutableList<Matchup>>()
    val beatenBy = mutableMapOf<Int, MutableList<Matchup>>()

    for (row in rows) {
        when (row.source) {
            MatchupSource.BEST ->
                beats.getOrPut(row.mainId) { mutableListOf() }
                    .add(Matchup(row.otherId, row.score, row.rank))
            MatchupSource.WORST ->
                beatenBy.getOrPut(row.mainId) { mutableListOf() }
                    .add(Matchup(row.otherId, kotlin.math.abs(row.score), row.rank))
        }
    }

    val allMainIds = beats.keys + beatenBy.keys
    return allMainIds.associateWith { id ->
        MainRecord(
            beats = beats[id].orEmpty(),
            beatenBy = beatenBy[id].orEmpty()
        )
    }
}
