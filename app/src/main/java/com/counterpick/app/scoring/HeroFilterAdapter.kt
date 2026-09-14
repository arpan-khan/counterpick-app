package com.counterpick.app.scoring

import com.counterpick.app.data.local.entity.HeroEntity

data class FilterableHero(
    val heroId: Int,
    override val roles: List<String>,
    override val lanes: List<String>
) : RoleLaneTagged

fun HeroEntity.toFilterable(): FilterableHero = FilterableHero(id, roles, lanes)

data class FilterableCounterResult(
    val result: CounterResult,
    override val roles: List<String>,
    override val lanes: List<String>
) : RoleLaneTagged

fun CounterResult.toFilterable(heroesById: Map<Int, HeroEntity>): FilterableCounterResult {
    val hero = heroesById[heroId]
    return FilterableCounterResult(this, hero?.roles.orEmpty(), hero?.lanes.orEmpty())
}
