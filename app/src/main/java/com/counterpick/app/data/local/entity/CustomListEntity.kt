package com.counterpick.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "custom_lists")
data class CustomListEntity(
    @PrimaryKey(autoGenerate = true) val listId: Long = 0,
    val name: String,
    val isActive: Boolean = false,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "custom_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomListEntity::class,
            parentColumns = ["listId"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class CustomEntryEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val listId: Long,
    val enemyHeroId: Int
)

@Entity(
    tableName = "custom_counters",
    foreignKeys = [
        ForeignKey(
            entity = CustomEntryEntity::class,
            parentColumns = ["entryId"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class CustomCounterEntity(
    @PrimaryKey(autoGenerate = true) val counterId: Long = 0,
    val entryId: Long,
    val counterHeroId: Int,
    val position: Int
)
