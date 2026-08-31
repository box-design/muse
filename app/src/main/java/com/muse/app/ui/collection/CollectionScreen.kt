package com.muse.app.ui.collection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.create.ScaledPostcard
import com.muse.app.ui.create.draftViewOf
import com.muse.app.ui.theme.MuseColors
import com.muse.app.util.Colors
import com.muse.app.util.Fmt

@Composable
fun CollectionScreen(
    application: MuseApplication,
    onGoExplore: () -> Unit,
    onOpenZine: (Long) -> Unit = {},
    onEditZine: (Long) -> Unit = {},
) {
    StatusBarAppearance(lightIcons = true)
    val vm: CollectionViewModel = viewModel {
        CollectionViewModel(application.container.archive, application.container.creations)
    }
    val items by vm.items.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<ArtifactUi?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MuseColors.Void)
            .statusBarsPadding()
    ) {
        FilterRow(current = filter, onSelect = { vm.filter.value = it })

        if (filter == ArchiveFilter.ZINES) {
            ZineShelf(
                items = items,
                onOpen = onOpenZine,
                onEdit = onEditZine,
                onRemove = { removing = it },
                modifier = Modifier.weight(1f)
            )
        } else if (items.isEmpty()) {
            EmptyCollection(onGoExplore)
        } else {
            val lastIndex = items.size - 1
            val pagerState = rememberPagerState(pageCount = { items.size })
            LaunchedEffect(filter) { pagerState.scrollToPage(0) }
            var page by remember { mutableIntStateOf(0) }
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page = it }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 24.dp
                ) { index ->
                    ArtifactPage(items[index], onLongPress = { removing = items[index] })
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val visibleCount = minOf(items.size, 40)
                    for (i in 0 until visibleCount) {
                        val active = i == page.coerceAtMost(visibleCount - 1)
                        Box(
                            Modifier
                                .size(if (active) 5.dp else 3.dp)
                                .background(MuseColors.White.copy(alpha = if (active) 0.9f else 0.25f))
                        )
                    }
                }
            }

            val safePage = page.coerceIn(0, lastIndex)
            val currentItem = items.getOrNull(safePage)
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    val spec = spring<IntOffset>(dampingRatio = 0.7f, stiffness = 250f)
                    val oldIndex = initialState?.let { items.indexOf(it) } ?: -1
                    val newIndex = targetState?.let { items.indexOf(it) } ?: -1
                    val forward = newIndex > oldIndex
                    if (forward) {
                        (slideInVertically(spec) { it / 2 } + fadeIn(tween(120))) togetherWith
                            (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(110)))
                    } else {
                        (slideInVertically(spec) { -it / 2 } + fadeIn(tween(120))) togetherWith
                            (slideOutVertically(tween(160)) { it / 2 } + fadeOut(tween(110)))
                    }
                },
                label = "archiveMeta"
            ) { item ->
                if (item == null) return@AnimatedContent
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        Modifier
                            .widthIn(max = 640.dp)
                            .padding(start = 30.dp, end = 30.dp, top = 20.dp, bottom = 94.dp)
                    ) {
                        MetaText(
                            Fmt.archiveNo(item.entry.number),
                            color = MuseColors.White,
                            size = 12.sp,
                            letterSpacing = 3.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        MetaText(
                            "${item.entry.title.ifBlank { "UNTITLED" }} · ${item.entry.subtitle}",
                            color = MuseColors.Gray2,
                            size = 10.sp
                        )
                    }
                }
            }
        }
    }

    val target = removing
    if (target != null) {
        if (target.content is ArtifactContent.ZineArt) {
            ZineActionsDialog(
                item = target,
                onDismiss = { removing = null },
                onEdit = {
                    onEditZine((target.content as ArtifactContent.ZineArt).record.id)
                    removing = null
                },
                onRemove = {
                    vm.remove(target.entry.id)
                    removing = null
                }
            )
        } else {
            RemoveDialog(
                onDismiss = { removing = null },
                onRemove = {
                    vm.remove(target.entry.id)
                    removing = null
                }
            )
        }
    }
}

@Composable
private fun FilterRow(current: ArchiveFilter, onSelect: (ArchiveFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ArchiveFilter.values().forEach { f ->
            val active = f == current
            Pressable(onClick = { onSelect(f) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = f.label,
                        style = TextStyle(
                            fontSize = 10.sp,
                            letterSpacing = 2.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (active) MuseColors.White else MuseColors.Gray3
                        )
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .height(1.dp)
                            .width(18.dp)
                            .background(MuseColors.White.copy(alpha = if (active) 1f else 0f))
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCollection(onGoExplore: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "00",
            style = TextStyle(
                fontSize = 104.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-4).sp,
                color = MuseColors.White
            )
        )
        Spacer(Modifier.height(6.dp))
        MetaText("COLLECTED", color = MuseColors.Gray2, letterSpacing = 6.sp, align = TextAlign.Center)
        Spacer(Modifier.height(34.dp))
        MetaText(
            "DOUBLE-TAP A PHOTOGRAPH IN THE EXHIBITION TO BEGIN YOUR ARCHIVE",
            color = MuseColors.Gray3,
            align = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(30.dp))
        Pressable(onClick = onGoExplore) {
            MetaText("OPEN EXHIBITIONS", color = MuseColors.White, size = 11.sp, letterSpacing = 3.sp)
        }
    }
}

@Composable
private fun ArtifactPage(item: ArtifactUi, onLongPress: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .pointerInput(item.entry.id) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (val c = item.content) {
                is ArtifactContent.PhotoArt -> PhotoImage(
                    uri = "content://media/external/images/media/${c.mediaId}",
                    modifier = Modifier
                        .widthIn(max = 620.dp)
                        .fillMaxWidth(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    thumbnail = false
                )
                is ArtifactContent.PostcardArt -> BoxWithConstraints(
                    contentAlignment = Alignment.Center
                ) {
                    val scale = with(LocalDensity.current) {
                        val wScale =
                            constraints.maxWidth.toFloat() / com.muse.app.ui.create.BASE_CARD_DP.dp.roundToPx()
                        val hScale = constraints.maxHeight.toFloat() /
                            (com.muse.app.ui.create.BASE_CARD_DP.dp / 0.75f).roundToPx()
                        minOf(wScale, hScale, 300f / com.muse.app.ui.create.BASE_CARD_DP)
                    }
                    ScaledPostcard(
                        view = draftViewOf(c.record),
                        scale = scale
                    )
                }
                is ArtifactContent.PaletteArt -> Box(Modifier.widthIn(max = 620.dp)) {
                    PaletteArtwork(c.record)
                }
                is ArtifactContent.ZineArt -> BoxWithConstraints(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val scale = com.muse.app.ui.zine.rememberZineScale(
                        maxWidthPx = constraints.maxWidth,
                        maxHeightPx = constraints.maxHeight
                    )
                    Box(
                        Modifier.shadow(
                            elevation = 8.dp,
                            spotColor = Color.Black,
                            ambientColor = Color.Black
                        )
                    ) {
                        com.muse.app.ui.zine.ScaledZinePage(scale = scale) {
                            com.muse.app.ui.zine.ZineSheet(paper = c.record.paper) {
                                com.muse.app.ui.zine.ZineCoverContent(c.record, emptyMap())
                            }
                        }
                    }
                }
                null -> MissingArtwork()
            }
        }
    }
}

@Composable
private fun PaletteArtwork(record: com.muse.app.data.model.PaletteRecord) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            record.colors.forEach { argb ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(argb))
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = record.name.uppercase(),
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.W500,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                color = MuseColors.White
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            record.colors.take(6).forEach { argb ->
                MetaText(Colors.hex(argb), color = MuseColors.Gray3, size = 8.5.sp)
            }
        }
    }
}

@Composable
private fun MissingArtwork() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MetaText("ARTIFACT MISSING", color = MuseColors.Gray3, size = 11.sp)
        Spacer(Modifier.height(10.dp))
        MetaText(
            "THE SOURCE WAS REMOVED FROM THIS DEVICE",
            color = MuseColors.Gray3.copy(alpha = 0.6f),
            size = 9.sp
        )
    }
}

@Composable
private fun RemoveDialog(onDismiss: () -> Unit, onRemove: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MuseColors.ScrimVoid)
                .padding(horizontal = 30.dp, vertical = 28.dp)
        ) {
            MetaText("REMOVE THIS ARTIFACT FROM THE ARCHIVE?", color = MuseColors.White, size = 11.sp)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Pressable(onClick = onDismiss) {
                    MetaText("KEEP", color = MuseColors.Gray2, size = 11.sp)
                }
                Pressable(onClick = onRemove) {
                    MetaText("REMOVE", color = MuseColors.White, size = 11.sp, weight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ZineShelf(
    items: List<ArtifactUi>,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onRemove: (ArtifactUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        EmptyZines()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 26.dp, end = 26.dp, top = 10.dp, bottom = 120.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(items, key = { "zine${it.entry.id}" }) { item ->
            val record = (item.content as? ArtifactContent.ZineArt)?.record
            if (record != null) {
                ZineShelfCell(item = item, record = record, onOpen = onOpen, onLongPress = { onRemove(item) })
            }
        }
    }
}

@Composable
private fun ZineShelfCell(
    item: ArtifactUi,
    record: com.muse.app.data.model.ZineRecord,
    onOpen: (Long) -> Unit,
    onLongPress: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .pointerInput(item.entry.id) {
                    detectTapGestures(
                        onTap = { onOpen(record.id) },
                        onLongPress = { onLongPress() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val scale =
                constraints.maxWidth.toFloat() / com.muse.app.ui.zine.BASE_ZINE_DP
            Box(Modifier.shadow(elevation = 8.dp, spotColor = Color.Black, ambientColor = Color.Black)) {
                com.muse.app.ui.zine.ScaledZinePage(scale = scale) {
                    com.muse.app.ui.zine.ZineSheet(paper = record.paper) {
                        com.muse.app.ui.zine.ZineCoverContent(record, emptyMap())
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MetaText(Fmt.archiveNo(item.entry.number), color = MuseColors.Gray3, size = 8.5.sp)
        Spacer(Modifier.height(4.dp))
        MetaText(
            "${item.entry.title.ifBlank { "UNTITLED" }} · ${record.pageCount} PLATES",
            color = MuseColors.Gray1.copy(alpha = 0.85f),
            size = 9.sp,
            align = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyZines() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "00",
            style = TextStyle(
                fontSize = 104.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-4).sp,
                color = MuseColors.White
            )
        )
        Spacer(Modifier.height(6.dp))
        MetaText("ZINES", color = MuseColors.Gray2, letterSpacing = 6.sp, align = TextAlign.Center)
        Spacer(Modifier.height(34.dp))
        MetaText(
            "PICK 8–20 PHOTOGRAPHS AND BIND THEM INTO A BOOK IN THE CREATE STUDIO",
            color = MuseColors.Gray3,
            align = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
private fun ZineActionsDialog(
    item: ArtifactUi,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(MuseColors.ScrimVoid)
                .padding(horizontal = 30.dp, vertical = 28.dp)
        ) {
            MetaText(
                "THIS ZINE — ${item.entry.title.ifBlank { "UNTITLED" }}",
                color = MuseColors.White,
                size = 11.sp
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                Pressable(onClick = onDismiss) {
                    MetaText("KEEP", color = MuseColors.Gray2, size = 11.sp)
                }
                Pressable(onClick = onEdit) {
                    MetaText("EDIT", color = MuseColors.Gray2, size = 11.sp)
                }
                Pressable(onClick = onRemove) {
                    MetaText("REMOVE", color = MuseColors.White, size = 11.sp, weight = FontWeight.Medium)
                }
            }
        }
    }
}
