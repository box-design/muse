package com.muse.app.ui.zine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.model.ZineRecord
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import kotlin.math.abs

@Composable
fun ZineReaderScreen(
    application: MuseApplication,
    zineId: Long,
    onClose: () -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val media = application.container.media
    val zines by application.container.creations.zines.collectAsStateWithLifecycle()
    val record = remember(zines, zineId) { zines.find { it.id == zineId } }

    if (record == null) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(120)
            onClose()
        }
        Box(Modifier.fillMaxSize().background(MuseColors.Void))
        return
    }

    var dates by remember(record.id) { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(record.id, record.pages) {
        val map = mutableMapOf<Long, Long>()
        record.pages.map { it.mediaId }.distinct().forEach { id ->
            runCatching { media.findById(id) }.getOrNull()?.let { map[id] = it.dateMs }
        }
        dates = map
    }

    val pageCount = record.pages.size + 2
    val pagerState = rememberPagerState(pageCount = { pageCount })
    var chromeVisible by remember { mutableStateOf(true) }
    var everOpened by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
            .collect { (page, fraction) ->
                if (page >= 1 || abs(fraction) > 0.02f) everOpened = true
            }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MuseColors.Void)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { chromeVisible = !chromeVisible })
            }
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 18.dp,
            beyondViewportPageCount = 1
        ) { index ->
            ReaderPage(index, record, dates, pagerState)
        }

        Box(Modifier.align(Alignment.BottomCenter)) {
            SwipeHint(visible = !everOpened)
        }

        ReaderChrome(
            visible = chromeVisible,
            record = record,
            pagerState = pagerState,
            pageCount = pageCount,
            onClose = onClose
        )
    }
}

@Composable
private fun ReaderPage(
    index: Int,
    record: ZineRecord,
    dates: Map<Long, Long>,
    pagerState: PagerState,
) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val scale = with(density) {
            rememberZineScale(
                maxWidthPx = constraints.maxWidth - 40.dp.roundToPx(),
                maxHeightPx = constraints.maxHeight - 32.dp.roundToPx()
            )
        }.coerceAtMost(2f)
        ScaledZinePage(
            scale = scale,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.6f)
                )
                .pageTurn(pagerState, index)
        ) {
            ZineSheet(paper = record.paper) {
                when {
                    index == 0 -> ZineCoverContent(record, dates)
                    index <= record.pageCount -> ZineInnerPageContent(record, index - 1, dates)
                    else -> ZineColophonContent(record, boundAt = record.updatedAt)
                }
            }
        }
    }
}

private fun Modifier.pageTurn(state: PagerState, page: Int): Modifier =
    graphicsLayer {
        val distance = (state.currentPage - page).toFloat() + state.currentPageOffsetFraction
        val t = abs(distance).coerceIn(0f, 1f)
        alpha = 1f - 0.28f * t
        scaleX = 1f - 0.05f * t
        scaleY = 1f - 0.05f * t
        rotationX = -7f * distance.coerceIn(-1f, 1f)
        cameraDistance = 26f * density
    }.drawWithContent {
        drawContent()
        val distance = (state.currentPage - page).toFloat() + state.currentPageOffsetFraction
        val t = abs(distance).coerceIn(0f, 1f)
        if (t > 0.01f) {
            drawRect(Color.Black.copy(alpha = 0.16f * t))
        }
    }

@Composable
private fun SwipeHint(visible: Boolean) {
    val transition = rememberInfiniteTransition(label = "zineHint")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "zineHintAlpha"
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(260))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .padding(bottom = 84.dp)
        ) {
            MetaText("SWIPE TO OPEN", color = MuseColors.White.copy(alpha = 0.75f), size = 9.sp, letterSpacing = 4.sp)
            Spacer(Modifier.height(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_vector_arrow_down),
                contentDescription = null,
                tint = MuseColors.White.copy(alpha = 0.65f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ReaderChrome(
    visible: Boolean,
    record: ZineRecord,
    pagerState: PagerState,
    pageCount: Int,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuseColors.Void.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 22.dp, top = 8.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pressable(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vector_arrow_back),
                        contentDescription = null,
                        tint = MuseColors.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp).padding(9.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                MetaText(
                    text = zineTitle(record),
                    color = MuseColors.Gray2,
                    size = 9.sp,
                    modifier = Modifier.weight(1f)
                )
                val label = when {
                    pagerState.currentPage == 0 -> "COVER"
                    pagerState.currentPage >= pageCount - 1 -> "END"
                    else -> folioLabel(pagerState.currentPage - 1, record.pageCount)
                }
                MetaText(text = label, color = MuseColors.White.copy(alpha = 0.85f), size = 9.sp)
            }
        }

        Spacer(Modifier.weight(1f))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            val progress by animateFloatAsState(
                targetValue = (pagerState.currentPage + 1f) / pageCount,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 220f),
                label = "zineProgress"
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 2 }
            ) {
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 18.dp)
                        .width(160.dp)
                        .height(1.dp)
                        .background(MuseColors.HairDark)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(1.dp)
                            .background(MuseColors.White.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}
