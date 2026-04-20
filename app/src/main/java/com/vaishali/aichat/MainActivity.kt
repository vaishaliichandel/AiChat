package com.vaishali.aichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vaishali.aichat.data.local.ThemePreferences
import com.vaishali.aichat.ui.theme.AiChatTheme
import com.vaishali.aichat.ui.theme.AppTheme
import com.vaishali.aichat.ui.theme.ChatScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val themePreferences = ThemePreferences(this)
        
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val savedTheme by themePreferences.themeFlow.collectAsState(initial = AppTheme.LIGHT)
            
            AiChatTheme(appTheme = savedTheme) {
                ChatScreen(
                    currentTheme = savedTheme,
                    onThemeChange = { newTheme ->
                        scope.launch {
                            themePreferences.saveTheme(newTheme)
                        }
                    }
                )
            }
        }
    }
}
