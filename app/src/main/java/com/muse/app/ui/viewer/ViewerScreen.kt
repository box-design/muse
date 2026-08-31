package com.muse.app.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.R
import com.muse.app.data.model.CollectOutcome
import com.muse.app.data.model.Photo
import com.muse.app.ui.components.ImmersiveSystemBars
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.theme.MuseColors
import com.muse.app.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ViewerContent(
    req: ViewerRequest,
    collectedIds: Set<Long>,
    onToggleCollect: suspend (Photo, String?) -> CollectOutcome,
    onCreatePostcard: (Photo) -> Unit,
    onExtractPalette: (Photo) -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()

        val progress = remember { Animatable(0f) }
        var closing by remember { mutableStateOf(false) }
        var menuOpen by remember { mutableStateOf(false) }
        var burst by remember { mutableIntStateOf(0) }
        var chipNumber by remember { mutableStateOf<Int?>(null) }
        var chipVisible by remember { mutableStateOf(false) }

        fun collect(photo: Photo) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                val outcome = onToggleCollect(photo, req.contextTitle)
                if (outcome.collected) {
                    chipNumber = outcome.archiveNumber
                    burst += 1
                }
            }
        }

        fun closeAnimated() {
            if (closing) return
            closing = true
            menuOpen = false
            scope.launch {
                if (req.origin != null) {
                    progress.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 380f))
                } else {
                    progress.animateTo(0f, tween(200))
                }
                onClose()
            }
        }

        BackHandler(onBack = ::closeAnimated)
        ImmersiveSystemBars(active = true)

        LaunchedEffect(Unit) {
            progress.animateTo(
                1f,
                if (req.origin != null) spring(dampingRatio = 0.75f, stiffness = 300f)
                else tween(260)
            )
        }

        LaunchedEffect(chipNumber) {
            if (chipNumber != null) {
                chipVisible = true
                delay(1700)
                chipVisible = false
            }
        }

        val heartScale = remember { Animatable(0f) }
        val heartAlpha = remember { Animatable(0f) }
        LaunchedEffect(burst) {
            if (burst > 0) {
                heartAlpha.snapTo(1f)
                heartScale.snapTo(0f)
                heartScale.animateTo(1.18f, spring(dampingRatio = 0.55f, stiffness = 320f))
                heartScale.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
                delay(320)
                heartAlpha.animateTo(0f, tween(220))
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color.Black.copy(alpha = 0.97f))
        )

        val lastIndex = (req.photos.size - 1).coerceAtLeast(0)
        val start = req.startIndex.coerceIn(0, lastIndex)
        val pagerState = rememberPagerState(
            initialPage = start,
            pageCount = { req.photos.size }
        )
        var page by remember { mutableIntStateOf(start) }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page = it }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 26.dp,
            userScrollEnabled = !closing
        ) { idx ->
            PagePhoto(
                photo = req.photos[idx],
                originRect = if (idx == start) req.origin else null,
                progress = progress.value,
                focused = idx == page,
                containerW = cw,
                containerH = ch,
                closing = closing,
                collectPulse = burst,
                onDismiss = ::closeAnimated,
                onDoubleTap = ::collect,
                onLongPress = { menuOpen = true }
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_vector_heart_filled),
            contentDescription = null,
            tint = MuseColors.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(104.dp)
                .graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                    alpha = heartAlpha.value
                }
        )

        val infoAlpha = ((progress.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
        if (infoAlpha > 0f && req.photos.isNotEmpty()) {
            val safePage = page.coerceIn(0, lastIndex)

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = infoAlpha
                        translationY = (1f - infoAlpha) * 60.dp.toPx()
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                AnimatedContent(
                    targetState = safePage,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val spec = spring<IntOffset>(dampingRatio = 0.7f, stiffness = 250f)
                        if (forward) {
                            (slideInVertically(spec) { it / 2 } + fadeIn(tween(120))) togetherWith
                                (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(110)))
                        } else {
                            (slideInVertically(spec) { -it / 2 } + fadeIn(tween(120))) togetherWith
                                (slideOutVertically(tween(160)) { it / 2 } + fadeOut(tween(110)))
                        }
                    },
                    label = "counter"
                ) { p ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(Modifier.weight(1f)) {
                            MetaText(Fmt.photoDate(req.photos[p].dateMs), color = MuseColors.Gray1, size = 11.sp)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = (req.contextTitle ?: "").uppercase(),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = MuseColors.White
                                ),
                                maxLines = 1
                            )
                        }
                        MetaText(
                            "${p + 1} / ${req.photos.size}",
                            color = MuseColors.Gray2,
                            size = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val current = req.photos[safePage]
                    val collected = current.id in collectedIds

                    Pressable(onClick = { collect(current) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(
                                    if (collected) R.drawable.ic_vector_heart_filled
                                    else R.drawable.ic_vector_heart_outline
                                ),
                                contentDescription = null,
                                tint = if (collected) MuseColors.White else MuseColors.Gray1,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            MetaText(
                                if (collected) "ARCHIVED" else "ARCHIVE",
                                color = if (collected) MuseColors.White else MuseColors.Gray1,
                                size = 10.sp,
                                letterSpacing = 2.5.sp
                            )
                        }
                    }
                    Pressable(onClick = { onCreatePostcard(current) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_vector_postcard),
                                contentDescription = null,
                                tint = MuseColors.Gray1,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            MetaText("POSTCARD", color = MuseColors.Gray1, size = 10.sp, letterSpacing = 2.5.sp)
                        }
                    }
                    Pressable(onClick = { onExtractPalette(current) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_vector_palette),
                                contentDescription = null,
                                tint = MuseColors.Gray1,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(9.dp))
                            MetaText("PALETTE", color = MuseColors.Gray1, size = 10.sp, letterSpacing = 2.5.sp)
                        }
                    }
                }
            }

            Pressable(
                onClick = { menuOpen = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 10.dp, start = 14.dp)
                    .graphicsLayer { alpha = infoAlpha }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_vector_dots),
                    contentDescription = null,
                    tint = MuseColors.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(40.dp).padding(12.dp)
                )
            }

            Pressable(
                onClick = ::closeAnimated,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 10.dp, end = 14.dp)
                    .graphicsLayer { alpha = infoAlpha }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_vector_close),
                    contentDescription = null,
                    tint = MuseColors.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp).padding(11.dp)
                )
            }
        }

        if (menuOpen) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuOpen = false }
            )
        }

        AnimatedVisibility(
            visible = menuOpen,
            enter = slideInVertically(spring(0.75f, 300f)) { it } + fadeIn(tween(140)),
            exit = slideOutVertically(spring(0.75f, 340f)) { it } + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MuseColors.ScrimVoid)
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                MenuRow(R.drawable.ic_vector_heart_outline, "COLLECT TO ARCHIVE") {
                    menuOpen = false
                    collect(req.photos[page.coerceIn(0, lastIndex)])
                }
                MenuRow(R.drawable.ic_vector_postcard, "CREATE POSTCARD") {
                    menuOpen = false
                    onCreatePostcard(req.photos[page.coerceIn(0, lastIndex)])
                }
                MenuRow(R.drawable.ic_vector_palette, "EXTRACT PALETTE") {
                    menuOpen = false
                    onExtractPalette(req.photos[page.coerceIn(0, lastIndex)])
                }
            }
        }

        AnimatedVisibility(
            visible = chipVisible,
            enter = fadeIn(tween(180)) + slideInVertically(spring(0.7f, 250f)) { -it },
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 14.dp)
        ) {
            Row(
                Modifier
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_vector_check),
                    contentDescription = null,
                    tint = MuseColors.White,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(8.dp))
                MetaText(
                    text = chipNumber?.let { "ARCHIVED · ${Fmt.no(it)}" } ?: "",
                    color = MuseColors.White,
                    size = 10.sp,
                    letterSpacing = 2.5.sp
                )
            }
        }
    }
}
