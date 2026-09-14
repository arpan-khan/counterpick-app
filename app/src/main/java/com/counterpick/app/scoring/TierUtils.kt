package com.counterpick.app.scoring

val TIER_ORDER = listOf("S+", "S", "A+", "A", "B+", "B", "C+", "C", "D")

fun sortTierKeys(keys: Collection<String>): List<String> =
    keys.sortedWith(compareBy(
        { key -> TIER_ORDER.indexOf(key).let { if (it == -1) Int.MAX_VALUE else it } },
        { key -> key }
    ))

enum class TierBucket { S, A, B, OTHER }

fun tierBucketFor(tier: String): TierBucket = when (tier) {
    "S", "S+" -> TierBucket.S
    "A", "A+" -> TierBucket.A
    "B", "B+" -> TierBucket.B
    else -> TierBucket.OTHER
}
