package com.counterpick.app.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class MetaJson(
    val tiers: Map<String, List<String>>,
    val credit: MetaCreditDto
)

object MetaJsonParser {

    private val DEFAULT_CREDIT = MetaCreditDto(
        text = "Tier list curated by u/hmmsucks on Reddit",
        url = "https://www.reddit.com/user/hmmsucks/"
    )

    fun parse(json: String): MetaJson {
        val root: JsonObject = Json.parseToJsonElement(json).jsonObject
        val tiers = mutableMapOf<String, List<String>>()
        var credit = DEFAULT_CREDIT

        for ((key, value) in root) {
            if (key == "credit") {
                val obj = value.jsonObject
                val text = obj["text"]?.jsonPrimitive?.content
                val url = obj["url"]?.jsonPrimitive?.content
                if (text != null && url != null) {
                    credit = MetaCreditDto(text, url)
                }
                continue
            }
            val names: JsonArray = value.jsonArray
            tiers[key] = names.map { it.jsonPrimitive.content }
        }

        return MetaJson(tiers = tiers, credit = credit)
    }
}
