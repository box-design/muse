package com.muse.app.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.MediaRepository
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.data.model.DayGroup
import com.muse.app.data.model.ExploreUi
import com.muse.app.data.model.Photo
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.SectionTitle
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import com.muse.app.ui.theme.MuseMotion
import com.muse.app.ui.viewer.ViewerRequest
import com.muse.app.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.ceil

class ExploreViewModel(media: MediaRepository) : ViewModel() {
    val ui: StateFlow<ExploreUi> = flow {
        emit(ExploreUi.Loading)
        val photos = runCatching { media.loadPhotos() }.getOrDefault(emptyList())
        val days = buildDayGroups(photos)
        emit(if (days.isEmpty()) ExploreUi.Empty else ExploreUi.Ready(days))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUi.Loading)
}

private fun buildDayGroups(photos: List<Photo>): List<DayGroup> =
    photos.sortedWith(compareByDescending<Photo> { it.dateMs }.thenByDescending { it.id })
        .groupBy { Fmt.dayKey(it.dateMs) }
        .map { (key, list) -> DayGroup(key, list.first().dateMs, list) }
        .sortedByDescending { it.dateMs }

@Composable
fun ExploreScreen(
    application: MuseApplication,
    onOpenViewer: (ViewerRequest) -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: ExploreViewModel = viewModel {
        ExploreViewModel(application.container.media)
    }
    val state by vm.ui.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(MuseColors.Void)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("TIMELINE")
            MetaText("MUSE", color = MuseColors.Gray3, letterSpacing = 4.sp)
        }

        when (val s = state) {
            is ExploreUi.Loading -> LoadingHint()
            is ExploreUi.Empty -> EmptyHint()
            is ExploreUi.Ready -> DayCardDeck(s.days, onOpenViewer)
        }
    }
}

@Composable
private fun LoadingHint() {
    var dots by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(450)
            dots = (dots + 1) % 4
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MetaText(
            text = "OPENING THE MUSEUM" + ".".repeat(dots),
            color = MuseColors.Gray3,
            align = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MetaText("NO PHOTOGRAPHS YET", color = MuseColors.Gray1)
            Spacer(Modifier.height(12.dp))
            MetaText(
                "YOUR DAYS WILL BE FOLDED INTO CARDS AUTOMATICALLY",
                color = MuseColors.Gray3,
                align = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}

@Composable
private fun DayCardDeck(
    days: List<DayGroup>,
    onOpenViewer: (ViewerRequest) -> Unit,
) {
    var expandedKeys by remember { mutableStateOf(emptySet<Int>()) }

    if (isAtLeastMediumWidth()) {
        LazyVerticalGrid(
            state = rememberLazyGridState(),
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 118.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            items(days.size, key = { days[it].key }) { idx ->
                DayFoldCard(
                    group = days[idx],
                    index = idx,
                    expanded = days[idx].key in expandedKeys,
                    onToggle = {
                        val key = days[idx].key
                        expandedKeys =
                            if (key in expandedKeys) expandedKeys - key else expandedKeys + key
                    },
                    onOpenViewer = onOpenViewer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    } else {
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 2.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            items(days.size, key = { days[it].key }) { idx ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    DayFoldCard(
                        group = days[idx],
                        index = idx,
                        expanded = days[idx].key in expandedKeys,
                        onToggle = {
                            val key = days[idx].key
                            expandedKeys =
                                if (key in expandedKeys) expandedKeys - key else expandedKeys + key
                        },
                        onOpenViewer = onOpenViewer,
                        modifier = Modifier.widthIn(max = 540.dp)
                    )
                }
            }
        }
    }
}

private val STAGGER = listOf(0.dp, 12.dp, 24.dp)

@Composable
private fun DayFoldCard(
    group: DayGroup,
    index: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenViewer: (ViewerRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stagger = STAGGER[index % STAGGER.size]
    var shown by remember(group.key) { mutableStateOf(false) }
    LaunchedEffect(group.key) {
        delay(45L * (index % 4))
        shown = true
    }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = MuseMotion.standard(),
        label = "cardEntrance"
    )

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp + stagger)
            .graphicsLayer {
                alpha = 0.7f + 0.3f * entrance
                translationY = (1f - entrance) * 18.dp.toPx()
            }
    ) {
        CardHeader(group, expanded, onToggle)
        Spacer(Modifier.height(12.dp))
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                if (targetState) {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(durationMillis = 200, delayMillis = 60)) +
                            scaleIn(
                                initialScale = 0.96f,
                                animationSpec = tween(durationMillis = 240, delayMillis = 60)
                            ),
                        initialContentExit = fadeOut(tween(durationMillis = 140)),
                        sizeTransform = SizeTransform(clip = false) { _, _ -> MuseMotion.unfold() }
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = fadeIn(tween(durationMillis = 180)),
                        initialContentExit = fadeOut(tween(durationMillis = 120)),
                        sizeTransform = SizeTransform(clip = false) { _, _ -> MuseMotion.unfold() }
                    )
                }
            }
        ) { isExpanded ->
            if (isExpanded) {
                ExpandedGrid(group, onOpenViewer)
            } else {
                FoldedPreview(group, onOpenViewer)
            }
        }
    }
}

@Composable
private fun CardHeader(
    group: DayGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MuseMotion.snappy(),
        label = "foldChevron"
    )
    Pressable(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Fmt.photoDate(group.dateMs),
                style = TextStyle(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.W500,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 1.sp,
                    color = MuseColors.White
                ),
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            MetaText(Fmt.artifacts(group.photos.size), color = MuseColors.Gray3, size = 10.sp)
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_vector_arrow_up),
                contentDescription = null,
                tint = MuseColors.Gray1.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun FoldedPreview(
    group: DayGroup,
    onOpenViewer: (ViewerRequest) -> Unit,
) {
    val rects = remember(group.key) { mutableMapOf<Long, Rect>() }
    val pagerState = rememberPagerState(pageCount = { group.photos.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 10.dp,
        key = { group.photos[it].id }
    ) { page ->
        val photo = group.photos[page]
        val off = abs(
            pagerState.currentPage + pagerState.currentPageOffsetFraction - page
        ).coerceIn(0f, 1f)
        Pressable(
            onClick = {
                onOpenViewer(
                    ViewerRequest(
                        photos = group.photos,
                        startIndex = page,
                        origin = rects[photo.id],
                        contextTitle = Fmt.photoDate(group.dateMs)
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer {
                        scaleX = 1f - off * 0.07f
                        scaleY = 1f - off * 0.07f
                        alpha = 1f - off * 0.4f
                    }
                    .onGloballyPositioned { coords ->
                        rects[photo.id] = coords.boundsInRoot()
                    }
            ) {
                PhotoImage(uri = photo.uri, modifier = Modifier.fillMaxSize(), thumbnail = true)
            }
        }
    }
}

@Composable
private fun ExpandedGrid(
    group: DayGroup,
    onOpenViewer: (ViewerRequest) -> Unit,
) {
    val rects = remember(group.key) { mutableMapOf<Long, Rect>() }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 7.dp
        val cols = (maxWidth / 106.dp).toInt().coerceIn(3, 5)
        val cell = (maxWidth - gap * (cols - 1)) / cols
        val rows = ceil(group.photos.size / cols.toFloat()).toInt()
        val gridHeight = cell * rows.toFloat() + gap * (rows - 1)

        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight + 2.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            userScrollEnabled = false
        ) {
            items(group.photos.size, key = { group.photos[it].id }) { idx ->
                val photo = group.photos[idx]
                GridCell(
                    photo = photo,
                    enterDelayMillis = (idx % cols) * 25L + (idx / cols) * 35L,
                    registerRect = { id, rect -> rects[id] = rect },
                    onClick = {
                        onOpenViewer(
                            ViewerRequest(
                                photos = group.photos,
                                startIndex = idx,
                                origin = rects[photo.id],
                                contextTitle = Fmt.photoDate(group.dateMs)
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun GridCell(
    photo: Photo,
    enterDelayMillis: Long,
    registerRect: (Long, Rect) -> Unit,
    onClick: () -> Unit,
) {
    var shown by remember(photo.id) { mutableStateOf(false) }
    LaunchedEffect(photo.id) {
        delay(enterDelayMillis)
        shown = true
    }
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = MuseMotion.soft(),
        label = "cellEntrance"
    )

    Pressable(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                alpha = 0.65f + 0.35f * entrance
                scaleX = 0.95f + 0.05f * entrance
                scaleY = 0.95f + 0.05f * entrance
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .onGloballyPositioned { coords ->
                    registerRect(photo.id, coords.boundsInRoot())
                }
        ) {
            PhotoImage(uri = photo.uri, modifier = Modifier.fillMaxSize(), thumbnail = true)
        }
    }
}
