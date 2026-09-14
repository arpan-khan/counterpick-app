package com.counterpick.app.ui.lookup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.scoring.MainRecord
import com.counterpick.app.scoring.Matchup
import com.counterpick.app.scoring.RoleLaneFilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class LookupUiState(
    val isLoading: Boolean = true,
    val heroes: List<HeroEntity> = emptyList(),
    val selectedHeroId: Int? = null,

    val beatsWell: List<Matchup> = emptyList(),

    val bestPicksAgainst: List<Matchup> = emptyList(),

    val browseFilterState: RoleLaneFilterState = RoleLaneFilterState()
)

class LookupViewModel(private val repository: CounterPickRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LookupUiState())
    val uiState: StateFlow<LookupUiState> = _uiState.asStateFlow()

    private var byMain: Map<Int, MainRecord> = emptyMap()
    var heroesById: Map<Int, HeroEntity> = emptyMap()
        private set

    init {
        repository.observeHeroes()
            .onEach { heroes ->
                heroesById = heroes.associateBy { it.id }
                _uiState.update { it.copy(isLoading = false, heroes = heroes) }
            }
            .launchIn(viewModelScope)

        repository.byMain
            .onEach { byMain = it }
            .launchIn(viewModelScope)
    }

    fun selectHero(heroId: Int) {
        val rec = byMain[heroId] ?: MainRecord()
        _uiState.update {
            it.copy(
                selectedHeroId = heroId,
                beatsWell = rec.beats.sortedBy { m -> m.rank },
                bestPicksAgainst = rec.beatenBy.sortedBy { m -> m.rank }
            )
        }
    }

    fun toggleBrowseRole(role: String) {
        _uiState.update { it.copy(browseFilterState = it.browseFilterState.toggleRole(role)) }
    }

    fun toggleBrowseLane(lane: String) {
        _uiState.update { it.copy(browseFilterState = it.browseFilterState.toggleLane(lane)) }
    }

    fun setAllBrowseRoles(selectAll: Boolean) {
        _uiState.update { it.copy(browseFilterState = it.browseFilterState.withAllRoles(selectAll)) }
    }

    fun setAllBrowseLanes(selectAll: Boolean) {
        _uiState.update { it.copy(browseFilterState = it.browseFilterState.withAllLanes(selectAll)) }
    }

    fun resetBrowseFilters() {
        _uiState.update { it.copy(browseFilterState = RoleLaneFilterState()) }
    }

    class Factory(private val repository: CounterPickRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LookupViewModel(repository) as T
    }
}
