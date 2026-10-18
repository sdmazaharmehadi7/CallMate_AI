package com.aicallassistant.app.core.config

import android.content.Context
import android.content.SharedPreferences
import com.aicallassistant.app.BuildConfig

/**
 * Global application configuration managing backend URLs and Supabase parameters.
 * Supports dynamic local runtime override for LAN IP physical device testing.
 */
object AppConfig {
    private const val PREFS_NAME = "app_config_prefs"
    private const val KEY_CUSTOM_BACKEND_URL = "custom_backend_url"

    // Default configuration from BuildConfig
    val defaultBackendUrl: String = BuildConfig.DEFAULT_BACKEND_URL
    val supabaseUrl: String = BuildConfig.SUPABASE_URL
    val supabasePublishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Resolves the active backend URL. Returns user-configured custom URL if set,
     * otherwise falls back to BuildConfig.DEFAULT_BACKEND_URL (e.g. http://10.0.2.2:8000).
     */
    fun getActiveBackendUrl(): String {
        val custom = sharedPreferences?.getString(KEY_CUSTOM_BACKEND_URL, null)
        return if (!custom.isNullOrBlank()) {
            custom.trimEnd('/')
        } else {
            defaultBackendUrl.trimEnd('/')
        }
    }

    /**
     * Resolves the active WebSocket URL corresponding to getActiveBackendUrl().
     * Transforms http:// to ws:// and https:// to wss://.
     */
    fun getActiveWebSocketUrl(): String {
        val httpUrl = getActiveBackendUrl()
        return when {
            httpUrl.startsWith("https://", ignoreCase = true) -> "wss://" + httpUrl.substring(8)
            httpUrl.startsWith("http://", ignoreCase = true) -> "ws://" + httpUrl.substring(7)
            else -> "ws://$httpUrl"
        }
    }

    fun setCustomBackendUrl(url: String?) {
        sharedPreferences?.edit()?.apply {
            if (url.isNullOrBlank()) {
                remove(KEY_CUSTOM_BACKEND_URL)
            } else {
                putString(KEY_CUSTOM_BACKEND_URL, url.trim().trimEnd('/'))
            }
            apply()
        }
    }

    fun isSupabaseConfigured(): Boolean {
        return supabasePublishableKey.isNotBlank() &&
                !supabasePublishableKey.contains("placeholder") &&
                supabaseUrl.isNotBlank() &&
                !supabaseUrl.contains("placeholder")
    }
}
