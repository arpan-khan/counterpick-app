package com.counterpick.app.scoring

import kotlin.math.abs

enum class MatchType { GOOD, BAD }

data class MatchedEntry(
    val enemyId: Int,
    val score: Double,
    val type: MatchType,
    val rank: Int
)

data class CounterResult(
    val heroId: Int,
    val total: Double,
    val matched: List<MatchedEntry>,
    val topRankHits: Int
) {

    val sortKey: Double get() = total + topRankHits * 0.003
}

object DraftScorer {

    fun rank(enemyIds: List<Int>, byMain: Map<Int, MainRecord>): List<CounterResult> {
        val excluded = enemyIds.toHashSet()
        val totals = mutableMapOf<Int, Double>()
        val topRankHits = mutableMapOf<Int, Int>()
        val matched = mutableMapOf<Int, MutableList<MatchedEntry>>()

        for (enemyId in enemyIds) {
            val rec = byMain[enemyId] ?: continue

            for (m in rec.beatenBy) {
                if (m.other in excluded) continue
                totals[m.other] = (totals[m.other] ?: 0.0) + m.score
                matched.getOrPut(m.other) { mutableListOf() }
                    .add(MatchedEntry(enemyId, m.score, MatchType.GOOD, m.rank))
                if (m.rank == 1) topRankHits[m.other] = (topRankHits[m.other] ?: 0) + 1
            }

            for (m in rec.beats) {
                if (m.other in excluded) continue
                totals[m.other] = (totals[m.other] ?: 0.0) - m.score
                matched.getOrPut(m.other) { mutableListOf() }
                    .add(MatchedEntry(enemyId, -m.score, MatchType.BAD, m.rank))
            }
        }

        return totals.keys
            .map { id ->
                CounterResult(
                    heroId = id,
                    total = totals[id] ?: 0.0,
                    matched = matched[id].orEmpty(),
                    topRankHits = topRankHits[id] ?: 0
                )
            }
            .sortedByDescending { it.sortKey }
    }

    fun topN(ranked: List<CounterResult>, n: Int = 15): List<CounterResult> = ranked.take(n)

    fun maxAbsTotal(top: List<CounterResult>): Double {
        val realMax = top.maxOfOrNull { abs(it.total) } ?: 0.0
        return kotlin.math.max(realMax, 0.0001)
    }
}
