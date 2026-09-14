package com.counterpick.app.scoring

import com.counterpick.app.data.local.dao.CustomCounterRow

data class FromYourListsResult(
    val heroId: Int,
    val matchedEnemyCount: Int,
    val bestPosition: Int
)

object FromYourListsEngine {

    fun compute(selectedEnemyIds: List<Int>, rows: List<CustomCounterRow>): List<FromYourListsResult> {
        if (selectedEnemyIds.isEmpty()) return emptyList()
        val enemySet = selectedEnemyIds.toHashSet()

        val matchedEnemiesByHero = mutableMapOf<Int, MutableSet<Int>>()
        val bestPositionByHero = mutableMapOf<Int, Int>()

        for (row in rows) {
            if (row.enemyHeroId !in enemySet) continue
            matchedEnemiesByHero.getOrPut(row.counterHeroId) { mutableSetOf() }.add(row.enemyHeroId)
            val currentBest = bestPositionByHero[row.counterHeroId]
            if (currentBest == null || row.position < currentBest) {
                bestPositionByHero[row.counterHeroId] = row.position
            }
        }

        return matchedEnemiesByHero.keys
            .map { heroId ->
                FromYourListsResult(
                    heroId = heroId,
                    matchedEnemyCount = matchedEnemiesByHero.getValue(heroId).size,
                    bestPosition = bestPositionByHero[heroId] ?: Int.MAX_VALUE
                )
            }
            .sortedWith(
                compareByDescending<FromYourListsResult> { it.matchedEnemyCount }
                    .thenBy { it.bestPosition }
            )
    }
}
