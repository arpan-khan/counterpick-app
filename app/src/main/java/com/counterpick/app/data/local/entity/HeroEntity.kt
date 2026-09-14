package com.counterpick.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heroes")
data class HeroEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String?,
    val roleCsv: String,
    val laneCsv: String
) {
    val roles: List<String> get() = roleCsv.split("|").filter { it.isNotEmpty() }
    val lanes: List<String> get() = laneCsv.split("|").filter { it.isNotEmpty() }

    companion object {
        fun packList(values: List<String>): String = values.joinToString("|")
    }
}
