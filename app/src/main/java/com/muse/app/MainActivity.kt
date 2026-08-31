package com.muse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.muse.app.ui.MuseRoot
import com.muse.app.ui.adaptive.LocalWindowWidthClass
import com.muse.app.ui.theme.MuseTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(LocalWindowWidthClass provides windowSizeClass.widthSizeClass) {
                MuseTheme {
                    MuseRoot(application as MuseApplication)
                }
            }
        }
    }
}
