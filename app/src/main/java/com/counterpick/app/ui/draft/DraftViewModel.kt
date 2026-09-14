package com.counterpick.app.ui.draft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.data.repository.CustomListsRepository
import com.counterpick.app.scoring.CounterResult
import com.counterpick.app.scoring.DraftScorer
import com.counterpick.app.scoring.FromYourListsEngine
import com.counterpick.app.scoring.FromYourListsResult
import com.counterpick.app.scoring.MainRecord
import com.counterpick.app.scoring.RoleLaneFilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAX_ENEMY_SLOTS = 5

data class DraftUiState(
    val isLoading: Boolean = true,
    val heroes: List<HeroEntity> = emptyList(),
    val enemySlots: List<Int?> = List(MAX_ENEMY_SLOTS) { null },
    val hasSearched: Boolean = false,
    val rankedCount: Int = 0,
    val top15: List<CounterResult> = emptyList(),
    val maxAbsTotal: Double = 0.0001,
    val filterState: RoleLaneFilterState = RoleLaneFilterState(),
    val expandedHeroId: Int? = null,

    val hasAnyCustomList: Boolean = false,
    val activeListName: String? = null,
    val useAllListsForSuggestions: Boolean = false,
    val fromYourLists: List<FromYourListsResult> = emptyList()
)

class DraftViewModel(
    private val repository: CounterPickRepository,
    private val customListsRepository: CustomListsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftUiState())
    val uiState: StateFlow<DraftUiState> = _uiState.asStateFlow()

    private var byMain: Map<Int, MainRecord> = emptyMap()
    var heroesById: Map<Int, HeroEntity> = emptyMap()
        private set

    init {
        repository.observeHeroes()
            .onEach { heroes ->
                heroesById = heroes.associateBy { it.id }
                _uiState.update { it.copy(heroes = heroes, isLoading = false) }
            }
            .launchIn(viewModelScope)

        repository.byMain
            .onEach { byMain = it }
            .launchIn(viewModelScope)

        customListsRepository.observeLists()
            .onEach { lists -> _uiState.update { it.copy(hasAnyCustomList = lists.isNotEmpty()) } }
            .launchIn(viewModelScope)

        customListsRepository.observeActiveList()
            .onEach { active -> _uiState.update { it.copy(activeListName = active?.name) } }
            .launchIn(viewModelScope)
    }

    fun setEnemySlot(slotIndex: Int, heroId: Int?) {
        _uiState.update { state ->
            val slots = state.enemySlots.toMutableList()
            slots[slotIndex] = heroId
            state.copy(enemySlots = slots)
        }
    }

    fun clearEnemySlot(slotIndex: Int) = setEnemySlot(slotIndex, null)

    fun findBestPicks() {
        val enemyIds = _uiState.value.enemySlots.filterNotNull()
        if (enemyIds.isEmpty()) {
            _uiState.update {
                it.copy(hasSearched = true, rankedCount = 0, top15 = emptyList(), fromYourLists = emptyList())
            }
            return
        }

        val ranked = DraftScorer.rank(enemyIds, byMain)
        val top15 = DraftScorer.topN(ranked)
        val maxAbs = DraftScorer.maxAbsTotal(top15)

        _uiState.update {
            it.copy(
                hasSearched = true,
                rankedCount = ranked.size,
                top15 = top15,
                maxAbsTotal = maxAbs,
                filterState = RoleLaneFilterState(),
                expandedHeroId = null
            )
        }

        refreshFromYourLists(enemyIds)
    }

    fun setUseAllListsForSuggestions(useAll: Boolean) {
        _uiState.update { it.copy(useAllListsForSuggestions = useAll) }
        val enemyIds = _uiState.value.enemySlots.filterNotNull()
        if (enemyIds.isNotEmpty()) refreshFromYourLists(enemyIds)
    }

    private fun refreshFromYourLists(enemyIds: List<Int>) {
        viewModelScope.launch {
            val useAll = _uiState.value.useAllListsForSuggestions
            val rows = if (useAll) {
                customListsRepository.getAllCounterRows()
            } else {
                val active = customListsRepository.getActiveList()
                if (active == null) emptyList() else customListsRepository.getCounterRowsForList(active.listId)
            }
            val results = FromYourListsEngine.compute(enemyIds, rows)
            _uiState.update { it.copy(fromYourLists = results) }
        }
    }

    fun toggleRole(role: String) {
        _uiState.update { it.copy(filterState = it.filterState.toggleRole(role)) }
    }

    fun toggleLane(lane: String) {
        _uiState.update { it.copy(filterState = it.filterState.toggleLane(lane)) }
    }

    fun setAllRoles(selectAll: Boolean) {
        _uiState.update { it.copy(filterState = it.filterState.withAllRoles(selectAll)) }
    }

    fun setAllLanes(selectAll: Boolean) {
        _uiState.update { it.copy(filterState = it.filterState.withAllLanes(selectAll)) }
    }

    fun resetFilters() {
        _uiState.update { it.copy(filterState = RoleLaneFilterState()) }
    }

    fun toggleExpanded(heroId: Int) {
        _uiState.update {
            it.copy(expandedHeroId = if (it.expandedHeroId == heroId) null else heroId)
        }
    }

    class Factory(
        private val repository: CounterPickRepository,
        private val customListsRepository: CustomListsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DraftViewModel(repository, customListsRepository) as T
    }
}
