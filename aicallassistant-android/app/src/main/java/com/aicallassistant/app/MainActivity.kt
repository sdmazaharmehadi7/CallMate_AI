package com.aicallassistant.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aicallassistant.app.ui.navigation.AppNavGraph
import com.aicallassistant.app.ui.theme.AiCallAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiCallAssistantTheme {
                AppNavGraph()
            }
        }
    }
}
