package com.muse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun StatusBarAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    DisposableEffect(lightIcons) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !lightIcons
        controller.isAppearanceLightNavigationBars = !lightIcons
        onDispose { }
    }
}

@Composable
fun ImmersiveSystemBars(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        if (active) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
}

@Composable
fun FullBackground(color: Color) {
    Box(Modifier.fillMaxSize().background(color))
}
