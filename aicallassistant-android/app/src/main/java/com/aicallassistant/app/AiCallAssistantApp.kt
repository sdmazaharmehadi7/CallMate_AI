package com.aicallassistant.app

import android.app.Application
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.data.audio.LocalRecordingManager
import com.aicallassistant.app.data.remote.SupabaseManager

class AiCallAssistantApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic config, local recording manager, and Supabase auth client
        AppConfig.initialize(this)
        LocalRecordingManager.initialize(this)
        SupabaseManager.initialize()
    }
}
