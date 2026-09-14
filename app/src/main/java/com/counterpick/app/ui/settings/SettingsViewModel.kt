package com.counterpick.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.counterpick.app.data.local.prefs.SettingsPreferences
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.data.repository.UpdateOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object InProgress : UpdateStatus()
    data class Success(val message: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

data class SettingsUiState(
    val cachedGeneratedDate: String? = null,
    val dataJsonUrl: String = "",
    val metaJsonUrl: String = "",
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val overlayEnabled: Boolean = false
)

class SettingsViewModel(
    private val repository: CounterPickRepository,
    private val prefs: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            dataJsonUrl = prefs.dataJsonUrl,
            metaJsonUrl = prefs.metaJsonUrl,
            overlayEnabled = prefs.overlayEnabled
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        repository.observeCacheMeta()
            .onEach { cache -> _uiState.update { it.copy(cachedGeneratedDate = cache?.generated) } }
            .launchIn(viewModelScope)
    }

    fun setDataJsonUrl(url: String) {
        _uiState.update { it.copy(dataJsonUrl = url) }
    }

    fun setMetaJsonUrl(url: String) {
        _uiState.update { it.copy(metaJsonUrl = url) }
    }

    fun saveUrls() {
        val state = _uiState.value
        if (state.dataJsonUrl.isNotBlank()) prefs.dataJsonUrl = state.dataJsonUrl.trim()
        if (state.metaJsonUrl.isNotBlank()) prefs.metaJsonUrl = state.metaJsonUrl.trim()
    }

    fun updateData() {
        saveUrls()
        val state = _uiState.value
        _uiState.update { it.copy(updateStatus = UpdateStatus.InProgress) }
        viewModelScope.launch {
            val outcome = repository.updateFromRemote(state.dataJsonUrl, state.metaJsonUrl)
            val newStatus = when (outcome) {
                is UpdateOutcome.Updated -> UpdateStatus.Success("Updated to ${outcome.newGenerated ?: "latest"}.")
                is UpdateOutcome.AlreadyUpToDate -> UpdateStatus.Success("Already up to date.")
                is UpdateOutcome.Failed -> UpdateStatus.Error(outcome.reason)
            }
            _uiState.update { it.copy(updateStatus = newStatus) }
        }
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs.overlayEnabled = enabled
        _uiState.update { it.copy(overlayEnabled = enabled) }
    }

    class Factory(
        private val repository: CounterPickRepository,
        private val prefs: SettingsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository, prefs) as T
    }
}
