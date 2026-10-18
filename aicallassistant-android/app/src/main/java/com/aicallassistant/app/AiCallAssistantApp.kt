package com.aicallassistant.app

import android.app.Application
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.data.remote.SupabaseManager

class AiCallAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic config and Supabase authentication client
        AppConfig.initialize(this)
        SupabaseManager.initialize()
    }
}
