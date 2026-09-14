package com.counterpick.app.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.counterpick.app.data.local.entity.CustomCounterEntity
import com.counterpick.app.data.local.entity.CustomListEntity
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.data.repository.CustomListsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EntryUiModel(
    val entryId: Long,
    val enemyHeroId: Int,
    val counters: List<CustomCounterEntity>
)

data class ListsUiState(
    val isLoading: Boolean = true,
    val heroes: List<HeroEntity> = emptyList(),
    val heroesById: Map<Int, HeroEntity> = emptyMap(),
    val lists: List<CustomListEntity> = emptyList(),
    val activeListId: Long? = null,
    val selectedListId: Long? = null,
    val entries: List<EntryUiModel> = emptyList()
)

class ListsViewModel(
    private val repository: CounterPickRepository,
    private val customListsRepository: CustomListsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {

        repository.observeHeroes()
            .onEach { heroes ->
                _uiState.update { it.copy(heroes = heroes, heroesById = heroes.associateBy { h -> h.id }, isLoading = false) }
            }
            .launchIn(viewModelScope)

        customListsRepository.observeLists()
            .onEach { lists ->
                _uiState.update { state ->
                    val stillValidSelection = lists.any { it.listId == state.selectedListId }
                    val newSelected = if (stillValidSelection) state.selectedListId else lists.firstOrNull()?.listId
                    state.copy(lists = lists, selectedListId = newSelected)
                }
                _uiState.value.selectedListId?.let { refreshEntries(it) }
            }
            .launchIn(viewModelScope)

        customListsRepository.observeActiveList()
            .onEach { active -> _uiState.update { it.copy(activeListId = active?.listId) } }
            .launchIn(viewModelScope)
    }

    fun selectList(listId: Long) {
        _uiState.update { it.copy(selectedListId = listId) }
        refreshEntries(listId)
    }

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = customListsRepository.createList(name.trim())
            _uiState.update { it.copy(selectedListId = id) }
        }
    }

    fun renameSelectedList(newName: String) {
        val list = currentSelectedList() ?: return
        if (newName.isBlank()) return
        viewModelScope.launch { customListsRepository.renameList(list, newName.trim()) }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch { customListsRepository.deleteList(listId) }
    }

    fun setActiveList(listId: Long) {
        viewModelScope.launch { customListsRepository.setActiveList(listId) }
    }

    fun addEnemyEntry(enemyHeroId: Int) {
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            customListsRepository.getOrCreateEntry(listId, enemyHeroId)
            refreshEntries(listId)
        }
    }

    fun deleteEntry(entryId: Long) {
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            customListsRepository.deleteEntry(entryId)
            refreshEntries(listId)
        }
    }

    fun addCounter(entryId: Long, counterHeroId: Int) {
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            customListsRepository.addCounter(entryId, counterHeroId)
            refreshEntries(listId)
        }
    }

    fun removeCounter(counterId: Long, entryId: Long) {
        val listId = _uiState.value.selectedListId ?: return
        viewModelScope.launch {
            customListsRepository.removeCounter(counterId, entryId)
            refreshEntries(listId)
        }
    }

    fun previewReorder(entryId: Long, fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val entries = state.entries.map { entry ->
                if (entry.entryId != entryId) entry
                else {
                    val mutable = entry.counters.toMutableList()
                    if (fromIndex in mutable.indices && toIndex in mutable.indices) {
                        val moved = mutable.removeAt(fromIndex)
                        mutable.add(toIndex, moved)
                    }
                    entry.copy(counters = mutable)
                }
            }
            state.copy(entries = entries)
        }
    }

    fun commitReorder(entryId: Long) {
        val entry = _uiState.value.entries.find { it.entryId == entryId } ?: return
        viewModelScope.launch {
            customListsRepository.reorderCounters(entryId, entry.counters.map { it.counterId })
        }
    }

    private fun currentSelectedList(): CustomListEntity? =
        _uiState.value.lists.find { it.listId == _uiState.value.selectedListId }

    private fun refreshEntries(listId: Long) {
        viewModelScope.launch {
            val entries = customListsRepository.getEntriesForList(listId)
            val withCounters = entries.map { entry ->
                EntryUiModel(
                    entryId = entry.entryId,
                    enemyHeroId = entry.enemyHeroId,
                    counters = customListsRepository.getCountersForEntry(entry.entryId)
                )
            }
            _uiState.update { it.copy(entries = withCounters) }
        }
    }

    class Factory(
        private val repository: CounterPickRepository,
        private val customListsRepository: CustomListsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListsViewModel(repository, customListsRepository) as T
    }
}
