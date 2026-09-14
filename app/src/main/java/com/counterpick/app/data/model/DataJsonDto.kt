package com.counterpick.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DataJsonDto(
    val generated: String? = null,
    val heroes: Map<String, HeroDto>,
    val best: List<List<Double>>,
    val worst: List<List<Double>>
)

@Serializable
data class HeroDto(
    val name: String,
    val image: String? = null,
    val role: List<String> = emptyList(),
    val lane: List<String> = emptyList()
)

@Serializable
data class MetaCreditDto(
    val text: String,
    val url: String
)
