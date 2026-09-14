package com.counterpick.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.counterpick.app.data.local.entity.CustomCounterEntity
import com.counterpick.app.data.local.entity.CustomEntryEntity
import com.counterpick.app.data.local.entity.CustomListEntity
import kotlinx.coroutines.flow.Flow

data class CustomCounterRow(
    val enemyHeroId: Int,
    val counterHeroId: Int,
    val position: Int
)

@Dao
interface CustomListDao {

    @Query("SELECT * FROM custom_lists ORDER BY createdAtEpochMs ASC")
    fun observeLists(): Flow<List<CustomListEntity>>

    @Query("SELECT * FROM custom_lists WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveList(): CustomListEntity?

    @Query("SELECT * FROM custom_lists WHERE isActive = 1 LIMIT 1")
    fun observeActiveList(): Flow<CustomListEntity?>

    @Insert
    suspend fun insertList(list: CustomListEntity): Long

    @Update
    suspend fun updateList(list: CustomListEntity)

    @Query("DELETE FROM custom_lists WHERE listId = :listId")
    suspend fun deleteList(listId: Long)

    @Query("UPDATE custom_lists SET isActive = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE custom_lists SET isActive = 1 WHERE listId = :listId")
    suspend fun setActiveFlag(listId: Long)

    @Transaction
    suspend fun setActiveList(listId: Long) {
        clearActiveFlag()
        setActiveFlag(listId)
    }

    @Query("SELECT * FROM custom_entries WHERE listId = :listId")
    fun observeEntriesForList(listId: Long): Flow<List<CustomEntryEntity>>

    @Query("SELECT * FROM custom_entries WHERE listId = :listId")
    suspend fun getEntriesForList(listId: Long): List<CustomEntryEntity>

    @Query("SELECT * FROM custom_entries WHERE listId = :listId AND enemyHeroId = :enemyHeroId LIMIT 1")
    suspend fun getEntry(listId: Long, enemyHeroId: Int): CustomEntryEntity?

    @Insert
    suspend fun insertEntry(entry: CustomEntryEntity): Long

    @Query("DELETE FROM custom_entries WHERE entryId = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("SELECT * FROM custom_counters WHERE entryId = :entryId ORDER BY position ASC")
    fun observeCountersForEntry(entryId: Long): Flow<List<CustomCounterEntity>>

    @Query("SELECT * FROM custom_counters WHERE entryId = :entryId ORDER BY position ASC")
    suspend fun getCountersForEntry(entryId: Long): List<CustomCounterEntity>

    @Query(
        """
        SELECT cc.* FROM custom_counters cc
        INNER JOIN custom_entries ce ON cc.entryId = ce.entryId
        WHERE ce.listId = :listId
        """
    )
    suspend fun getAllCountersForList(listId: Long): List<CustomCounterEntity>

    @Query(
        """
        SELECT ce.enemyHeroId AS enemyHeroId, cc.counterHeroId AS counterHeroId, cc.position AS position
        FROM custom_counters cc
        INNER JOIN custom_entries ce ON cc.entryId = ce.entryId
        WHERE ce.listId = :listId
        """
    )
    suspend fun getCounterRowsForList(listId: Long): List<CustomCounterRow>

    @Query(
        """
        SELECT ce.enemyHeroId AS enemyHeroId, cc.counterHeroId AS counterHeroId, cc.position AS position
        FROM custom_counters cc
        INNER JOIN custom_entries ce ON cc.entryId = ce.entryId
        """
    )
    suspend fun getAllCounterRows(): List<CustomCounterRow>

    @Insert
    suspend fun insertCounter(counter: CustomCounterEntity): Long

    @Query("DELETE FROM custom_counters WHERE counterId = :counterId")
    suspend fun deleteCounter(counterId: Long)

    @Update
    suspend fun updateCounters(counters: List<CustomCounterEntity>)

    @Transaction
    suspend fun reorderCounters(entryId: Long, orderedCounterIds: List<Long>) {
        val current = getCountersForEntry(entryId).associateBy { it.counterId }
        val updated = orderedCounterIds.mapIndexedNotNull { index, counterId ->
            current[counterId]?.copy(position = index)
        }
        if (updated.isNotEmpty()) updateCounters(updated)
    }
}
