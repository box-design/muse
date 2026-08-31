package com.muse.app.ui.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.R
import com.muse.app.data.model.Photo
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.theme.MuseColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun PagePhoto(
    photo: Photo,
    originRect: Rect?,
    progress: Float,
    focused: Boolean,
    containerW: Float,
    containerH: Float,
    closing: Boolean,
    collectPulse: Int,
    onDismiss: () -> Unit,
    onDoubleTap: (Photo) -> Unit,
    onLongPress: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(closing) {
        if (closing) drag.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 500f))
    }

    LaunchedEffect(collectPulse) {
        if (collectPulse > 0) {
            pulse.snapTo(1f)
            pulse.animateTo(1.02f, spring(dampingRatio = 0.8f, stiffness = 600f))
            pulse.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
        }
    }

    val fit = remember(photo.aspect, containerW, containerH) {
        fitRect(containerW, containerH, photo.aspect)
    }

    val norm = abs(drag.value) / (containerH * 0.9f)
    val sc = (1f - 0.32f * norm.coerceIn(0f, 1f)) * pulse.value

    val morphT = if (originRect != null) progress else 1f
    val baseRect = if (originRect != null) lerpRect(originRect, fit, morphT) else fit

    val w = baseRect.width * sc
    val h = baseRect.height * sc
    val left = baseRect.center.x - w / 2f
    val top = baseRect.center.y + drag.value - h / 2f

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            w.roundToInt().coerceAtLeast(1),
                            h.roundToInt().coerceAtLeast(1)
                        )
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(IntOffset(left.roundToInt(), top.roundToInt()))
                    }
                }
                .graphicsLayer {
                    if (!focused) alpha = ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)
                }
                .then(
                    if (focused) {
                        Modifier
                            .pointerInput(focused) {
                                val tracker = VelocityTracker()
                                detectVerticalDragGestures(
                                    onDragStart = { tracker.resetTracking() },
                                    onVerticalDrag = { change, dy ->
                                        change.consume()
                                        scope.launch { drag.snapTo(drag.value + dy) }
                                        tracker.addPosition(change.uptimeMillis, change.position)
                                    },
                                    onDragEnd = {
                                        val velocity = tracker.calculateVelocity()
                                        val freshNorm =
                                            abs(drag.value) / (containerH * 0.9f)
                                        val fast = abs(velocity.y) > 800.dp.toPx()
                                        if (freshNorm >= 0.3f || fast) {
                                            onDismiss()
                                        } else {
                                            scope.launch {
                                                drag.animateTo(0f, spring(0.75f, 350f))
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch { drag.animateTo(0f, spring(0.75f, 350f)) }
                                    }
                                )
                            }
                            .pointerInput(focused) {
                                detectTapGestures(
                                    onDoubleTap = { onDoubleTap(photo) },
                                    onLongPress = { onLongPress() }
                                )
                            }
                    } else {
                        Modifier
                    }
                )
        ) {
            PhotoImage(
                uri = photo.uri,
                modifier = Modifier.fillMaxSize(),
                thumbnail = false,
                crossfade = 0,
                contentScale = ContentScale.Crop
            )
        }
    }
}

internal fun fitRect(w: Float, h: Float, aspect: Float): Rect {
    val safeAspect = if (aspect <= 0f) 0.75f else aspect
    return if (w / h > safeAspect) {
        val width = h * safeAspect
        Rect((w - width) / 2f, 0f, (w + width) / 2f, h)
    } else {
        val height = w / safeAspect
        Rect(0f, (h - height) / 2f, w, (h + height) / 2f)
    }
}

internal fun lerpRect(a: Rect, b: Rect, t: Float): Rect {
    val k = t.coerceIn(0f, 1f)
    fun l(x: Float, y: Float): Float = x + (y - x) * k
    return Rect(
        l(a.left, b.left),
        l(a.top, b.top),
        l(a.right, b.right),
        l(a.bottom, b.bottom)
    )
}

@Composable
internal fun MenuRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Pressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MuseColors.White,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MuseColors.White
                ),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
