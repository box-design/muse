package com.muse.app.ui.adaptive

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

@Composable
fun isCompactWidth(): Boolean =
    LocalWindowWidthClass.current == WindowWidthSizeClass.Compact

@Composable
fun isAtLeastMediumWidth(): Boolean =
    LocalWindowWidthClass.current != WindowWidthSizeClass.Compact

@Composable
fun isExpandedWidth(): Boolean =
    LocalWindowWidthClass.current == WindowWidthSizeClass.Expanded
