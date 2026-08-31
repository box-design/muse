package com.muse.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object MuseColors {
    val Void = Color(0xFF050505)
    val ScrimVoid = Color(0xFF0A0A09)
    val Paper = Color(0xFFF6F3EC)
    val PaperPure = Color(0xFFFFFFFF)
    val PaperGrey = Color(0xFFE9E5DC)
    val Ink = Color(0xFF161512)
    val White = Color(0xFFEDEDEA)
    val Gray1 = Color(0xFFB8B6B1)
    val Gray2 = Color(0xFF807E79)
    val Gray3 = Color(0xFF4C4A46)
    val HairDark = Color(0xFF232320)
    val HairLight = Color(0xFFDCD8CF)
}

object MuseMotion {
    fun standard() = spring<Float>(dampingRatio = 0.75f, stiffness = 300f)
    fun soft() = spring<Float>(dampingRatio = 0.7f, stiffness = 250f)
    fun snappy() = spring<Float>(dampingRatio = 0.8f, stiffness = 380f)
    fun bouncy() = spring<Float>(dampingRatio = 0.6f, stiffness = 300f)
    fun <T> unfold(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 260f)
}

@Composable
fun MuseTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
