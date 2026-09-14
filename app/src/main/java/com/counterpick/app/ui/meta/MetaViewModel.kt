package com.counterpick.app.ui.meta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.scoring.FilterEngine
import com.counterpick.app.scoring.RoleLaneFilterState
import com.counterpick.app.scoring.sortTierKeys
import com.counterpick.app.scoring.toFilterable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class MetaUiState(
    val isLoading: Boolean = true,

    val tierGroups: List<Pair<String, List<HeroEntity>>> = emptyList(),
    val filterState: RoleLaneFilterState = RoleLaneFilterState(),
    val creditText: String? = null,
    val creditUrl: String? = null
)

class MetaViewModel(private val repository: CounterPickRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MetaUiState())
    val uiState: StateFlow<MetaUiState> = _uiState.asStateFlow()

    init {

        combine(
            repository.observeHeroes(),
            repository.observeMetaTiers(),
            repository.observeCredit()
        ) { heroes, tiers, credit ->
            val heroesById = heroes.associateBy { it.id }
            val grouped = tiers.groupBy { it.tier }
            val ordered = sortTierKeys(grouped.keys).map { tierKey ->
                val heroesInTier = grouped[tierKey].orEmpty()
                    .mapNotNull { heroesById[it.heroId] }
                    .sortedBy { it.name }
                tierKey to heroesInTier
            }
            MetaUiState(
                isLoading = false,
                tierGroups = ordered,
                creditText = credit?.text,
                creditUrl = credit?.url
            )
        }
            .onEach { newState -> _uiState.update { current -> newState.copy(filterState = current.filterState) } }
            .launchIn(viewModelScope)
    }

    fun visibleGroupsFor(state: MetaUiState): List<Pair<String, List<HeroEntity>>> =
        state.tierGroups.mapNotNull { (tier, heroes) ->
            val filtered = FilterEngine
                .filter(heroes.map { it.toFilterable() }, state.filterState)
                .mapNotNull { f -> heroes.find { it.id == f.heroId } }
            if (filtered.isEmpty()) null else tier to filtered
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

    class Factory(private val repository: CounterPickRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MetaViewModel(repository) as T
    }
}
