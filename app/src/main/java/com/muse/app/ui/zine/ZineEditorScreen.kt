package com.muse.app.ui.zine

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.model.ZineCoverStyle
import com.muse.app.data.model.ZinePageLayout
import com.muse.app.data.model.ZinePaper
import com.muse.app.data.model.ZineRecord
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.ui.components.Hairline
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors

private enum class ZineTool { ORDER, COVER, PAPER, STYLE, LAYOUT }

@Composable
fun ZineEditorScreen(
    application: MuseApplication,
    zineId: Long?,
    onClose: () -> Unit,
    onBound: (Long) -> Unit,
) {
    StatusBarAppearance(lightIcons = false)
    val vm: ZineEditorViewModel = viewModel(key = "zine_editor_${zineId ?: 0}") {
        ZineEditorViewModel(
            media = application.container.media,
            creations = application.container.creations,
            archiveRepository = application.container.archive,
            zineId = zineId
        )
    }
    val ready by vm.ready.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val dates by vm.dates.collectAsStateWithLifecycle()

    LaunchedEffect(ready) {
        if (!ready) {
            kotlinx.coroutines.delay(500)
            if (!vm.ready.value) onClose()
        }
    }

    var tool by remember { mutableStateOf<ZineTool?>(null) }
    var selectedPage by remember { mutableStateOf<Int?>(null) }

    val record = remember(draft) { draftRecord(draft) }
    val canBind = vm.canBind(draft)

    Box(Modifier.fillMaxSize().background(MuseColors.Paper)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pressable(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vector_close),
                        contentDescription = null,
                        tint = MuseColors.Ink.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp).padding(9.dp)
                    )
                }
                Text(
                    text = "ZINE",
                    style = TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MuseColors.Ink
                    )
                )
                Pressable(onClick = { if (canBind) vm.publish(onBound) }) {
                    Text(
                        text = "BIND",
                        style = TextStyle(
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = MuseColors.Ink.copy(alpha = if (canBind) 1f else 0.35f)
                        )
                    )
                }
            }

            if (isAtLeastMediumWidth()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    EditorStage(
                        record = record,
                        dates = dates,
                        tool = tool,
                        selectedPage = selectedPage,
                        pageCount = record.pageCount,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MuseColors.HairLight))
                    Column(
                        Modifier.width(340.dp).fillMaxHeight().navigationBarsPadding()
                    ) {
                        Hairline(MuseColors.HairLight)
                        ZineTabs(selected = tool, onSelect = { t -> tool = if (t == tool) null else t })
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = tool != null,
                                enter = expandVertically(spring(0.75f, 300f)) + fadeIn(tween(180)),
                                exit = shrinkVertically(spring(0.75f, 340f)) + fadeOut(tween(180)),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight()
                                        .verticalScroll(rememberScrollState())
                                        .background(MuseColors.Paper)
                                ) {
                                    Hairline(MuseColors.HairLight)
                                    when (tool ?: ZineTool.ORDER) {
                                        ZineTool.ORDER -> OrderPanel(
                                            vm = vm, draft = draft, record = record,
                                            selectedPage = selectedPage,
                                            onSelectPage = { selectedPage = it },
                                            gridHeight = 340.dp
                                        )
                                        ZineTool.COVER -> CoverPanel(vm, record)
                                        ZineTool.PAPER -> PaperPanel(vm, draft.paper)
                                        ZineTool.STYLE -> StylePanel(vm, draft.coverStyle)
                                        ZineTool.LAYOUT -> LayoutPanel(vm, draft, record, selectedPage)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                EditorStage(
                    record = record,
                    dates = dates,
                    tool = tool,
                    selectedPage = selectedPage,
                    pageCount = record.pageCount,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = tool != null,
                    enter = expandVertically(spring(0.75f, 300f)) + fadeIn(tween(180)),
                    exit = shrinkVertically(spring(0.75f, 340f)) + fadeOut(tween(180))
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(MuseColors.Paper)
                    ) {
                        Hairline(MuseColors.HairLight)
                        when (tool ?: ZineTool.ORDER) {
                            ZineTool.ORDER -> OrderPanel(
                                vm = vm, draft = draft, record = record,
                                selectedPage = selectedPage,
                                onSelectPage = { selectedPage = it },
                                gridHeight = 150.dp
                            )
                            ZineTool.COVER -> CoverPanel(vm, record)
                            ZineTool.PAPER -> PaperPanel(vm, draft.paper)
                            ZineTool.STYLE -> StylePanel(vm, draft.coverStyle)
                            ZineTool.LAYOUT -> LayoutPanel(vm, draft, record, selectedPage)
                        }
                    }
                }

                ZineTabs(
                    selected = tool,
                    onSelect = { t -> tool = if (t == tool) null else t },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
private fun EditorStage(
    record: ZineRecord,
    dates: Map<Long, Long>,
    tool: ZineTool?,
    selectedPage: Int?,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    val previewTarget = remember(tool, selectedPage, pageCount) {
        when {
            tool == ZineTool.STYLE || tool == ZineTool.COVER || tool == ZineTool.PAPER -> -1
            selectedPage != null && selectedPage in 0 until pageCount -> selectedPage
            else -> -1
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val scale = with(density) {
                rememberZineScale(
                    maxWidthPx = constraints.maxWidth - 44.dp.roundToPx(),
                    maxHeightPx = constraints.maxHeight - 28.dp.roundToPx()
                )
            }
            AnimatedContent(
                targetState = previewTarget,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                },
                label = "zineStage"
            ) { target ->
                ScaledZinePage(
                    scale = scale,
                    modifier = Modifier.shadow(
                        elevation = 14.dp,
                        ambientColor = Color.Black.copy(alpha = 0.28f),
                        spotColor = Color.Black.copy(alpha = 0.32f)
                    )
                ) {
                    ZineSheet(paper = record.paper) {
                        if (target < 0) {
                            ZineCoverContent(record, dates)
                        } else {
                            ZineInnerPageContent(record, target, dates)
                        }
                    }
                }
            }
        }

        MetaText(
            text = if (previewTarget < 0) "COVER PREVIEW" else "PAGE ${folioLabel(previewTarget, pageCount)}",
            color = MuseColors.Gray2.copy(alpha = 0.8f),
            size = 8.5.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun ZineTabs(
    selected: ZineTool?,
    onSelect: (ZineTool?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(MuseColors.Paper),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ZineTool.values().forEach { t ->
            val isSelected = selected == t
            Pressable(onClick = { onSelect(t) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(30.dp), contentAlignment = Alignment.BottomCenter) {
                        Text(
                            text = t.name,
                            style = TextStyle(
                                fontSize = 10.sp,
                                letterSpacing = 1.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) MuseColors.Ink else MuseColors.Gray2
                            )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .background(if (isSelected) MuseColors.Ink else Color.Transparent)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderPanel(
    vm: ZineEditorViewModel,
    draft: com.muse.app.ui.zine.ZineDraft,
    record: ZineRecord,
    selectedPage: Int?,
    onSelectPage: (Int?) -> Unit,
    gridHeight: Dp,
) {
    val reorder = rememberZineReorder { from, to ->
        vm.movePage(from, to)
        if (selectedPage != null) onSelectPage(null)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = draft.title,
                onValueChange = { v -> vm.edit { it.copy(title = v) } },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.W500,
                    color = MuseColors.Ink
                ),
                cursorBrush = SolidColor(MuseColors.Ink),
                singleLine = true,
                decorationBox = { inner ->
                    Column {
                        inner()
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.height(1.dp).fillMaxWidth().background(MuseColors.HairLight))
                    }
                },
                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
            )
            Spacer(Modifier.width(14.dp))
            MetaText("${draft.pages.size} PAGES", color = MuseColors.Gray2, size = 9.sp)
        }
        Spacer(Modifier.height(4.dp))
        MetaText("LONG-PRESS TO REORDER · TAP TO SELECT A PAGE", color = MuseColors.Gray2.copy(alpha = 0.7f), size = 8.sp)
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(64.dp),
            state = reorder.listState,
            userScrollEnabled = !reorder.isDragging,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(count = record.pages.size, key = { record.pages[it].mediaId }) { index ->
                val mediaId = record.pages[index].mediaId
                val isCover = mediaId == record.coverMediaId
                val isSelected = selectedPage == index
                Box(modifier = reorder.itemModifier(index).aspectRatio(ZINE_ASPECT)) {
                    Pressable(onClick = { onSelectPage(if (isSelected) null else index) }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .border(
                                    1.dp,
                                    when {
                                        isSelected -> MuseColors.Ink
                                        isCover -> MuseColors.Ink.copy(alpha = 0.45f)
                                        else -> MuseColors.HairLight
                                    }
                                )
                        ) {
                            PhotoThumb(mediaId, record.paper, Modifier.fillMaxSize())
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                style = TextStyle(
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MuseColors.White
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            if (isCover) {
                                MetaText(
                                    "COVER",
                                    color = MuseColors.White,
                                    size = 6.5.sp,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val page = selectedPage
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page != null) {
                Pressable(onClick = { vm.removePage(page); onSelectPage(null) }) {
                    MetaText(
                        "REMOVE PAGE ${(page + 1).toString().padStart(2, '0')}",
                        color = MuseColors.Ink.copy(alpha = if (record.pageCount > ZINE_MIN_PAGES) 0.85f else 0.3f),
                        size = 9.sp
                    )
                }
            } else {
                MetaText(" ", size = 9.sp)
            }
            if (page != null) {
                MetaText("PAGE ${folioLabel(page, record.pageCount)} SELECTED", color = MuseColors.Gray2, size = 8.sp)
            }
        }
    }
}

@Composable
private fun PhotoThumb(mediaId: Long, paper: ZinePaper, modifier: Modifier = Modifier) {
    val spec = ZinePapers.spec(paper)
    Box(modifier.background(MuseColors.PaperGrey)) {
        com.muse.app.ui.components.PhotoImage(
            uri = zineUri(mediaId),
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            thumbnail = true,
            crossfade = 0,
            colorFilter = if (spec.saturation < 1f) {
                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                    androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(spec.saturation) }
                )
            } else null
        )
    }
}

@Composable
private fun CoverPanel(vm: ZineEditorViewModel, record: ZineRecord) {
    Column(Modifier.fillMaxWidth()) {
        MetaText("TAP TO CHOOSE THE COVER", color = MuseColors.Gray2.copy(alpha = 0.7f), size = 8.sp, modifier = Modifier.padding(start = 24.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(count = record.pages.size, key = { record.pages[it].mediaId }) { index ->
                val mediaId = record.pages[index].mediaId
                val isCover = mediaId == record.coverMediaId
                val candidate = remember(record, mediaId) { record.copy(coverMediaId = mediaId) }
                Pressable(onClick = { vm.setCover(mediaId) }, scaleDown = 0.95f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(72.dp)
                                .border(
                                    1.dp,
                                    if (isCover) MuseColors.Ink else MuseColors.HairLight
                                )
                        ) {
                            ScaledZinePage(scale = 72f / BASE_ZINE_DP) {
                                ZineSheet(paper = record.paper) {
                                    ZineCoverContent(candidate, emptyMap())
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        MetaText(
                            "${index + 1}",
                            color = if (isCover) MuseColors.Ink else MuseColors.Gray2,
                            size = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaperPanel(vm: ZineEditorViewModel, current: ZinePaper) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ZinePaper.values().forEach { paper ->
            val spec = ZinePapers.spec(paper)
            val selected = paper == current
            Pressable(onClick = { vm.setPaper(paper) }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                            .border(
                                1.dp,
                                if (selected) MuseColors.Ink else MuseColors.HairLight
                            )
                            .background(spec.bg)
                    ) {
                        PaperTexture(spec, Modifier.matchParentSize())
                    }
                    Spacer(Modifier.height(7.dp))
                    MetaText(
                        spec.label,
                        color = if (selected) MuseColors.Ink else MuseColors.Gray2,
                        size = 8.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StylePanel(vm: ZineEditorViewModel, current: ZineCoverStyle) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        listOf(
            ZineCoverStyle.FULL_BLEED to "FULL BLEED",
            ZineCoverStyle.FRAME to "FRAME",
            ZineCoverStyle.EDITORIAL to "EDITORIAL"
        ).forEach { (style, label) ->
            val selected = style == current
            Pressable(onClick = { vm.setCoverStyle(style) }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StyleWireframe(style, selected)
                    Spacer(Modifier.height(7.dp))
                    MetaText(label, color = if (selected) MuseColors.Ink else MuseColors.Gray2, size = 8.5.sp)
                }
            }
        }
    }
}

@Composable
private fun StyleWireframe(style: ZineCoverStyle, selected: Boolean) {
    val stroke = if (selected) MuseColors.Ink else MuseColors.Gray3.copy(alpha = 0.8f)
    val fill = if (selected) MuseColors.Ink else MuseColors.Gray3.copy(alpha = 0.55f)
    androidx.compose.foundation.Canvas(
        Modifier
            .width(56.dp)
            .height(74.dp)
    ) {
        val w = size.width
        val h = size.height
        when (style) {
            ZineCoverStyle.FULL_BLEED -> {
                drawRect(color = fill.copy(alpha = 0.25f))
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.78f), size = androidx.compose.ui.geometry.Size(w, h * 0.22f))
            }
            ZineCoverStyle.FRAME -> {
                drawRect(color = stroke, style = Stroke(width = 1.dp.toPx()))
                drawRect(
                    color = fill.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.14f),
                    size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.5f)
                )
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.76f), size = androidx.compose.ui.geometry.Size(w * 0.4f, 2.dp.toPx()))
            }
            ZineCoverStyle.EDITORIAL -> {
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.08f), size = androidx.compose.ui.geometry.Size(w * 0.55f, 2.5f.dp.toPx()))
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.17f), size = androidx.compose.ui.geometry.Size(w * 0.35f, 2.5f.dp.toPx()))
                drawRect(
                    color = fill.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.34f),
                    size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.52f)
                )
            }
        }
    }
}

@Composable
private fun LayoutPanel(
    vm: ZineEditorViewModel,
    draft: com.muse.app.ui.zine.ZineDraft,
    record: ZineRecord,
    selectedPage: Int?,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
        MetaText("BOOK DEFAULT", color = MuseColors.Gray2.copy(alpha = 0.7f), size = 8.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                ZinePageLayout.GALLERY to "GALLERY",
                ZinePageLayout.MUSEUM to "MUSEUM",
                ZinePageLayout.JOURNAL to "JOURNAL"
            ).forEach { (layout, label) ->
                val selected = layout == draft.defaultLayout
                Pressable(onClick = { vm.setDefaultLayout(layout) }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LayoutWireframe(layout, selected)
                        Spacer(Modifier.height(6.dp))
                        MetaText(label, color = if (selected) MuseColors.Ink else MuseColors.Gray2, size = 8.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Hairline(MuseColors.HairLight)
        Spacer(Modifier.height(14.dp))

        val page = selectedPage
        MetaText(
            if (page != null) "THIS PAGE — ${folioLabel(page, record.pageCount)}" else "THIS PAGE",
            color = MuseColors.Gray2.copy(alpha = 0.7f),
            size = 8.sp
        )
        Spacer(Modifier.height(12.dp))
        if (page == null) {
            MetaText("SELECT A PAGE IN ORDER FIRST", color = MuseColors.Gray3, size = 8.5.sp)
        } else {
            val mediaId = record.pages.getOrNull(page)?.mediaId
            val override = mediaId?.let { draft.layouts[it] }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipOption("FOLLOW BOOK", override == null) { vm.setPageLayout(mediaId!!, null) }
                ChipOption("GALLERY", override == ZinePageLayout.GALLERY) { vm.setPageLayout(mediaId!!, ZinePageLayout.GALLERY) }
                ChipOption("MUSEUM", override == ZinePageLayout.MUSEUM) { vm.setPageLayout(mediaId!!, ZinePageLayout.MUSEUM) }
                ChipOption("JOURNAL", override == ZinePageLayout.JOURNAL) { vm.setPageLayout(mediaId!!, ZinePageLayout.JOURNAL) }
            }
        }
    }
}

@Composable
private fun ChipOption(label: String, active: Boolean, onClick: () -> Unit) {
    Pressable(onClick = onClick) {
        Box(
            Modifier
                .border(1.dp, if (active) MuseColors.Ink else MuseColors.HairLight)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            MetaText(label, color = if (active) MuseColors.Ink else MuseColors.Gray2, size = 8.sp)
        }
    }
}

@Composable
private fun LayoutWireframe(layout: ZinePageLayout, selected: Boolean) {
    val stroke = if (selected) MuseColors.Ink else MuseColors.Gray3.copy(alpha = 0.8f)
    val fill = if (selected) MuseColors.Ink else MuseColors.Gray3.copy(alpha = 0.55f)
    androidx.compose.foundation.Canvas(
        Modifier
            .width(56.dp)
            .height(74.dp)
    ) {
        val w = size.width
        val h = size.height
        when (layout) {
            ZinePageLayout.GALLERY -> {
                drawRect(color = fill.copy(alpha = 0.25f), size = size)
                drawRect(color = stroke, style = Stroke(width = 1.dp.toPx()))
            }
            ZinePageLayout.MUSEUM -> {
                drawRect(
                    color = fill.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.1f),
                    size = androidx.compose.ui.geometry.Size(w * 0.72f, h * 0.56f)
                )
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.78f), size = androidx.compose.ui.geometry.Size(w * 0.4f, 2f.dp.toPx()))
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.87f), size = androidx.compose.ui.geometry.Size(w * 0.24f, 2f.dp.toPx()))
            }
            ZinePageLayout.JOURNAL -> {
                drawRect(
                    color = fill.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(w, h * 0.58f)
                )
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.68f), size = androidx.compose.ui.geometry.Size(w * 0.8f, 2f.dp.toPx()))
                drawRect(color = fill, topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.78f), size = androidx.compose.ui.geometry.Size(w * 0.5f, 2f.dp.toPx()))
            }
        }
    }
}

