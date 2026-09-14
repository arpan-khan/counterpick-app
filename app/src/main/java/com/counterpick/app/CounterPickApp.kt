package com.counterpick.app

import android.app.Application
import com.counterpick.app.data.local.AppDatabase
import com.counterpick.app.data.local.prefs.SettingsPreferences
import com.counterpick.app.data.repository.CounterPickRepository
import com.counterpick.app.data.repository.CustomListsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CounterPickApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val repository: CounterPickRepository by lazy {
        CounterPickRepository(
            heroDao = database.heroDao(),
            matchupDao = database.matchupDao(),
            metaDao = database.metaDao()
        )
    }

    val customListsRepository: CustomListsRepository by lazy {
        CustomListsRepository(database.customListDao())
    }

    val settingsPreferences: SettingsPreferences by lazy { SettingsPreferences(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            if (!repository.hasCachedData()) {
                repository.updateFromRemote(
                    settingsPreferences.dataJsonUrl,
                    settingsPreferences.metaJsonUrl
                )
            }
        }
    }
}
