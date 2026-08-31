package com.muse.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muse.app.ui.theme.MuseColors

data class MuseTab(
    val route: String,
    val iconRes: Int,
)

private val PillDark = Color(0xCC171614)
private val PillPaper = Color(0xFFE7E2D7)

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun MuseBottomBar(
    current: String?,
    paper: Boolean,
    items: List<MuseTab>,
    onSelect: (String) -> Unit,
) {
    val pillColor by animateColorAsState(
        targetValue = if (paper) PillPaper else PillDark,
        animationSpec = tween(240),
        label = "pillBg"
    )
    val edgeColor by animateColorAsState(
        targetValue = if (paper) MuseColors.HairLight else MuseColors.HairDark,
        animationSpec = tween(240),
        label = "pillEdge"
    )
    val highlightColor by animateColorAsState(
        targetValue = if (paper) MuseColors.Ink.copy(alpha = 0.08f) else MuseColors.White.copy(alpha = 0.12f),
        animationSpec = tween(240),
        label = "pillHighlight"
    )
    val activeTint = if (paper) MuseColors.Ink else MuseColors.White
    val inactiveTint = MuseColors.Gray2

    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(60.dp)
                .shadow(elevation = 18.dp, shape = CircleShape, spotColor = Color.Black)
                .background(pillColor, CircleShape)
                .border(width = 1.dp, color = edgeColor, shape = CircleShape)
        ) {
            val tabWidth = maxWidth / items.size
            val activeIndex = items.indexOfFirst { it.route == current }.coerceAtLeast(0)
            val indicatorX by animateDpAsState(
                targetValue = tabWidth * activeIndex,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
                label = "indicatorX"
            )

            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 9.dp)
                    .background(highlightColor, CircleShape)
            )

            Row(Modifier.fillMaxSize()) {
                items.forEach { tab ->
                    val active = tab.route == current
                    val tint by animateColorAsState(
                        targetValue = if (active) activeTint else inactiveTint,
                        animationSpec = tween(220),
                        label = "tabTint"
                    )
                    val bounce = remember { Animatable(1f) }
                    LaunchedEffect(active) {
                        if (active && bounce.value == 1f) {
                            bounce.snapTo(0.68f)
                            bounce.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f))
                        }
                    }
                    Pressable(
                        onClick = { onSelect(tab.route) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(tab.iconRes),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = bounce.value
                                        scaleY = bounce.value
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}
