package com.counterpick.app.data.repository

import com.counterpick.app.data.local.dao.CustomCounterRow
import com.counterpick.app.data.local.dao.CustomListDao
import com.counterpick.app.data.local.entity.CustomCounterEntity
import com.counterpick.app.data.local.entity.CustomEntryEntity
import com.counterpick.app.data.local.entity.CustomListEntity

class CustomListsRepository(private val dao: CustomListDao) {

    fun observeLists() = dao.observeLists()
    fun observeActiveList() = dao.observeActiveList()
    suspend fun getActiveList(): CustomListEntity? = dao.getActiveList()

    suspend fun createList(name: String, makeActive: Boolean = false): Long {
        val id = dao.insertList(
            CustomListEntity(name = name, isActive = false, createdAtEpochMs = System.currentTimeMillis())
        )
        if (makeActive) dao.setActiveList(id)
        return id
    }

    suspend fun renameList(list: CustomListEntity, newName: String) {
        dao.updateList(list.copy(name = newName))
    }

    suspend fun deleteList(listId: Long) = dao.deleteList(listId)

    suspend fun setActiveList(listId: Long) = dao.setActiveList(listId)

    suspend fun getEntriesForList(listId: Long): List<CustomEntryEntity> = dao.getEntriesForList(listId)

    suspend fun getOrCreateEntry(listId: Long, enemyHeroId: Int): CustomEntryEntity {
        dao.getEntry(listId, enemyHeroId)?.let { return it }
        val newId = dao.insertEntry(CustomEntryEntity(listId = listId, enemyHeroId = enemyHeroId))
        return CustomEntryEntity(entryId = newId, listId = listId, enemyHeroId = enemyHeroId)
    }

    suspend fun deleteEntry(entryId: Long) = dao.deleteEntry(entryId)

    suspend fun getCountersForEntry(entryId: Long): List<CustomCounterEntity> = dao.getCountersForEntry(entryId)

    suspend fun addCounter(entryId: Long, counterHeroId: Int) {
        val current = dao.getCountersForEntry(entryId)
        val nextPosition = (current.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertCounter(CustomCounterEntity(entryId = entryId, counterHeroId = counterHeroId, position = nextPosition))
    }

    suspend fun removeCounter(counterId: Long, entryId: Long) {
        dao.deleteCounter(counterId)

        val remaining = dao.getCountersForEntry(entryId)
        dao.reorderCounters(entryId, remaining.map { it.counterId })
    }

    suspend fun reorderCounters(entryId: Long, orderedCounterIds: List<Long>) =
        dao.reorderCounters(entryId, orderedCounterIds)

    suspend fun getCounterRowsForList(listId: Long): List<CustomCounterRow> = dao.getCounterRowsForList(listId)
    suspend fun getAllCounterRows(): List<CustomCounterRow> = dao.getAllCounterRows()
}
