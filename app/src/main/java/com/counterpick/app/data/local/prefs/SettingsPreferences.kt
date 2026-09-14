package com.counterpick.app.data.local.prefs

import android.content.Context
import com.counterpick.app.data.remote.RemoteConstants

class SettingsPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("counterpick_settings", Context.MODE_PRIVATE)

    var dataJsonUrl: String
        get() = prefs.getString(KEY_DATA_URL, RemoteConstants.DEFAULT_DATA_JSON_URL)
            ?: RemoteConstants.DEFAULT_DATA_JSON_URL
        set(value) = prefs.edit().putString(KEY_DATA_URL, value).apply()

    var metaJsonUrl: String
        get() = prefs.getString(KEY_META_URL, RemoteConstants.DEFAULT_META_JSON_URL)
            ?: RemoteConstants.DEFAULT_META_JSON_URL
        set(value) = prefs.edit().putString(KEY_META_URL, value).apply()

    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    companion object {
        private const val KEY_DATA_URL = "data_json_url"
        private const val KEY_META_URL = "meta_json_url"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    }
}
