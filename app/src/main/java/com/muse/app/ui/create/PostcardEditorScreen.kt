package com.muse.app.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.model.CropMode
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.ui.components.Hairline
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import kotlinx.coroutines.launch

private enum class Tool { TEMPLATE, LAYOUT, TEXT, INFO, COLOR }

@Composable
fun PostcardEditorScreen(
    application: MuseApplication,
    photoId: Long?,
    place: String?,
    postcardId: Long?,
    onClose: () -> Unit,
) {
    StatusBarAppearance(lightIcons = false)
    val vm: EditorViewModel = viewModel(key = "editor_${photoId ?: 0}_${postcardId ?: 0}") {
        EditorViewModel(
            media = application.container.media,
            creations = application.container.creations,
            archiveRepository = application.container.archive,
            photoId = photoId,
            place = place,
            postcardId = postcardId
        )
    }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val nextNumber by vm.nextNumber.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val palettes by application.container.creations.palettes.collectAsStateWithLifecycle()

    var tool by remember { mutableStateOf<Tool?>(null) }
    var showVariants by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
        }
    }

    val numberLabel = com.muse.app.util.Fmt.archiveNo(nextNumber)
    val view = remember(draft, nextNumber) {
        buildView(draft, numberLabel).copy(
            onPan = { dx, dy ->
                vm.edit { d -> d.copy(offsetX = d.offsetX + dx, offsetY = d.offsetY + dy) }
            }
        )
    }
    val ink = MuseColors.Ink

    val scope = rememberCoroutineScope()
    var awaitingPermission by remember { mutableStateOf(false) }

    val activityContext = LocalContext.current

    fun performDownload() {
        scope.launch {
            vm.message.value = "EXPORTING..."
            val result = PostcardExporter.export(
                context = activityContext,
                view = view.copy(onPan = null),
                fileNameBase = resolvedTitle(view)
            )
            vm.message.value = result.fold(
                onSuccess = { "SAVED TO GALLERY · PNG" },
                onFailure = { e ->
                    if (e is PostcardExporter.Failure) e.messageLabel else "EXPORT FAILED"
                }
            )
        }
    }

    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && awaitingPermission) {
            awaitingPermission = false
            performDownload()
        } else if (!granted) {
            vm.message.value = "STORAGE PERMISSION NEEDED"
        }
    }

    fun requestDownload() {
        if (Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(application, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            performDownload()
        } else {
            awaitingPermission = true
            writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

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
                        tint = ink.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp).padding(9.dp)
                    )
                }
                Text(
                    text = "POSTCARD",
                    style = TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ink
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Pressable(onClick = { vm.save() }) {
                        MetaText("SAVE", color = ink, size = 10.sp, letterSpacing = 2.sp, weight = FontWeight.Medium)
                    }
                    Pressable(onClick = { vm.collect() }) {
                        MetaText("COLLECT", color = ink.copy(alpha = 0.65f), size = 10.sp, letterSpacing = 2.sp)
                    }
                    Pressable(onClick = { requestDownload() }) {
                        MetaText("DOWNLOAD", color = ink.copy(alpha = 0.65f), size = 10.sp, letterSpacing = 2.sp)
                    }
                }
            }

            if (isAtLeastMediumWidth()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    CanvasStage(
                        view = view,
                        message = message,
                        onShowVariants = { showVariants = true },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    Box(Modifier.width(1.dp).fillMaxHeight().background(MuseColors.HairLight))

                    Column(
                        Modifier.width(340.dp).fillMaxHeight().navigationBarsPadding()
                    ) {
                        Hairline(MuseColors.HairLight)
                        ToolTabs(
                            selected = tool,
                            onSelect = { t -> tool = if (t == tool) null else t },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = tool != null,
                                enter = expandVertically(spring(0.75f, 300f)) + fadeIn(tweenPaper()),
                                exit = shrinkVertically(spring(0.75f, 340f)) + fadeOut(tweenPaper()),
                                modifier = Modifier.align(Alignment.TopCenter)
                            ) {
                                ToolPanel(
                                    tool = tool ?: Tool.TEMPLATE,
                                    draft = draft,
                                    numberLabelForPanel = numberLabel,
                                    palettes = palettes,
                                    onEdit = { vm.edit(it) },
                                    onApplyPalette = { vm.applyPalette(it) },
                                    panelModifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            } else {
                CanvasStage(
                    view = view,
                    message = message,
                    onShowVariants = { showVariants = true },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = tool != null,
                    enter = expandVertically(spring(0.75f, 300f)) + fadeIn(tweenPaper()),
                    exit = shrinkVertically(spring(0.75f, 340f)) + fadeOut(tweenPaper())
                ) {
                    ToolPanel(
                        tool = tool ?: Tool.TEMPLATE,
                        draft = draft,
                        numberLabelForPanel = numberLabel,
                        palettes = palettes,
                        onEdit = { vm.edit(it) },
                        onApplyPalette = { vm.applyPalette(it) },
                        panelModifier = Modifier.height(236.dp)
                    )
                }

                ToolTabs(
                    selected = tool,
                    onSelect = { t -> tool = if (t == tool) null else t },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    }

    if (showVariants) {
        VariantsOverlay(
            base = draft,
            onPick = { picked ->
                vm.edit { picked }
                showVariants = false
            },
            onClose = { showVariants = false },
            numberLabel = numberLabel
        )
    }
}

private fun tweenPaper() = androidx.compose.animation.core.tween<Float>(180)

@Composable
private fun CanvasStage(
    view: DraftView,
    message: String?,
    onShowVariants: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = MuseColors.Ink
    Box(modifier, contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val scale = with(LocalDensity.current) {
                val maxWidthPx = constraints.maxWidth - 44.dp.roundToPx()
                val maxHeightPx = constraints.maxHeight - 28.dp.roundToPx()
                val baseHeight = (BASE_CARD_DP.dp / 0.75f).roundToPx()
                val wScale = if (maxWidthPx <= 0) 1f else maxWidthPx.toFloat() / BASE_CARD_DP.dp.roundToPx()
                val hScale = if (maxHeightPx <= 0) 1f else maxHeightPx.toFloat() / baseHeight
                minOf(wScale, hScale)
            }
            ScaledPostcard(view = view, scale = scale)
        }

        Pressable(
            onClick = onShowVariants,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
        ) {
            Text(
                text = "GENERATE VARIANTS",
                style = TextStyle(
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ink.copy(alpha = 0.75f)
                )
            )
        }

        MessageToast(
            message = message,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        )
    }
}

@Composable
private fun ToolTabs(
    selected: Tool?,
    onSelect: (Tool?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().background(MuseColors.Paper),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tool.values().forEach { t ->
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
private fun MessageToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tweenPaper()) + expandVertically(spring(0.75f, 300f)),
        exit = fadeOut(tweenPaper()) + shrinkVertically(spring(0.75f, 340f)),
        modifier = modifier
    ) {
        Box(
            Modifier
                .background(MuseColors.Ink)
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            MetaText(message ?: "", color = MuseColors.Paper, size = 9.5.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun ToolPanel(
    tool: Tool,
    draft: Draft,
    numberLabelForPanel: String,
    palettes: List<com.muse.app.data.model.PaletteRecord>,
    onEdit: ((Draft) -> Draft) -> Unit,
    onApplyPalette: (com.muse.app.data.model.PaletteRecord) -> Unit,
    panelModifier: Modifier = Modifier,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(panelModifier)
            .background(MuseColors.Paper)
    ) {
        Hairline(MuseColors.HairLight)
        when (tool) {
            Tool.TEMPLATE -> TemplatePanel(draft, onEdit)
            Tool.LAYOUT -> LayoutPanel(draft, onEdit)
            Tool.TEXT -> TextPanel(draft, onEdit)
            Tool.INFO -> InfoPanel(draft, numberLabelForPanel, onEdit)
            Tool.COLOR -> ColorPanel(draft, palettes, onEdit, onApplyPalette)
        }
    }
}

@Composable
private fun TemplatePanel(draft: Draft, onEdit: ((Draft) -> Draft) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(count = Templates.all.size, key = { Templates.all[it].id }) { index ->
            val spec = Templates.all[index]
            val selected = spec.id == draft.templateId
            Pressable(onClick = { onEdit { it.copy(templateId = spec.id, offsetX = 0f, offsetY = 0f) } }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TemplateWireframe(spec = spec, selected = selected)
                    Spacer(Modifier.height(7.dp))
                    MetaText(
                        spec.displayName,
                        color = if (selected) MuseColors.Ink else MuseColors.Gray2,
                        size = 8.5.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateWireframe(spec: TemplateSpec, selected: Boolean) {
    val stroke = if (selected) MuseColors.Ink else MuseColors.Gray3.copy(alpha = 0.8f)
    androidx.compose.foundation.Canvas(
        Modifier
            .width(52.dp)
            .height(70.dp)
            .then(if (selected) Modifier.border(1.dp, MuseColors.Ink) else Modifier)
    ) {
        val w = size.width
        val h = size.height
        val p = 5.dp.toPx()
        drawRect(color = stroke.copy(alpha = 0.55f), style = Stroke(0.9.dp.toPx()))
        var top = p + 3.dp.toPx()
        if (spec.topMetaBlock) {
            drawLine(stroke, Offset(p + 2f, top + 2f), Offset(p + 2f + w * 0.22f, top + 2f), strokeWidth = 1.4f)
            top += 9.dp.toPx()
        }
        val bottomReserve = p + 13.dp.toPx()
        val imgH = (h - top - bottomReserve) * (if (spec.ghostDate || spec.bigPlace) 0.72f else 0.62f)
        drawRect(
            color = stroke,
            topLeft = Offset(p + 2f, top),
            size = androidx.compose.ui.geometry.Size(w - (p + 2f) * 2, imgH),
            style = Stroke(1.1f)
        )
        var ty = top + imgH + 6.dp.toPx()
        val line1w = (w - (p + 2f) * 2) * 0.55f
        val x1 = if (spec.centered) (w - line1w) / 2f else p + 2f
        drawLine(stroke, Offset(x1, ty), Offset(x1 + line1w, ty), strokeWidth = 1.6f)
        ty += 5.dp.toPx()
        val line2w = (w - (p + 2f) * 2) * 0.35f
        val x2 = if (spec.centered) (w - line2w) / 2f else p + 2f
        drawLine(stroke.copy(alpha = 0.65f), Offset(x2, ty), Offset(x2 + line2w, ty), strokeWidth = 1.1f)
        if (spec.stamp) {
            drawRect(
                color = stroke,
                topLeft = Offset(w - p - 12.dp.toPx(), p + 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(9.dp.toPx(), 9.dp.toPx()),
                style = Stroke(1f)
            )
        }
        if (spec.frame) {
            drawRect(
                color = stroke.copy(alpha = 0.7f),
                topLeft = Offset(p + 5f, p + 5f),
                size = androidx.compose.ui.geometry.Size(w - (p + 5f) * 2, h - (p + 5f) * 2),
                style = Stroke(0.7f)
            )
        }
    }
}

@Composable
private fun LayoutPanel(draft: Draft, onEdit: ((Draft) -> Draft) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                CropMode.FIT to "FIT",
                CropMode.FILL to "FILL",
                CropMode.CROP to "CROP"
            ).forEach { (mode, label) ->
                val selected = draft.cropMode == mode
                Pressable(onClick = { onEdit { it.copy(cropMode = mode, offsetX = 0f, offsetY = 0f) } }) {
                    Box(
                        Modifier
                            .border(
                                1.dp,
                                if (selected) MuseColors.Ink else MuseColors.HairLight
                            )
                            .padding(horizontal = 20.dp, vertical = 11.dp)
                    ) {
                        MetaText(label, color = if (selected) MuseColors.Ink else MuseColors.Gray2, size = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        MetaText(
            if (draft.cropMode == CropMode.CROP)
                "DRAG THE PHOTOGRAPH ON THE CARD TO REPOSITION"
            else
                "FIT KEEPS THE FULL FRAME · FILL COVERS THE CANVAS",
            color = MuseColors.Gray2,
            size = 9.sp
        )
    }
}

@Composable
private fun TextPanel(draft: Draft, onEdit: ((Draft) -> Draft) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp)) {
        LineField("TITLE", draft.title, ink = MuseColors.Ink) { v ->
            onEdit { it.copy(title = v) }
        }
        Spacer(Modifier.height(16.dp))
        LineField("PLACE", draft.placeLabel, ink = MuseColors.Ink) { v ->
            onEdit { it.copy(placeLabel = v) }
        }
    }
}

@Composable
private fun InfoPanel(
    draft: Draft,
    nextNumberLabel: String,
    onEdit: ((Draft) -> Draft) -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 14.dp)) {
        LineField("PLACE", draft.placeLabel, MuseColors.Ink) { v ->
            onEdit { it.copy(placeLabel = v) }
        }
        Spacer(Modifier.height(14.dp))
        ToggleLine("SHOW DATE", draft.showDate) { checked ->
            onEdit { it.copy(showDate = checked) }
        }
        ToggleLine("SHOW LOCATION", draft.showLocation) { checked ->
            onEdit { it.copy(showLocation = checked) }
        }
        ToggleLine("SHOW ARCHIVE NUMBER", draft.showNumber) { checked ->
            onEdit { it.copy(showNumber = checked) }
        }
        Spacer(Modifier.height(10.dp))
        MetaText("NEXT NUMBER — $nextNumberLabel", color = MuseColors.Gray2, size = 9.sp)
    }
}

@Composable
private fun ColorPanel(
    draft: Draft,
    palettes: List<com.muse.app.data.model.PaletteRecord>,
    onEdit: ((Draft) -> Draft) -> Unit,
    onApplyPalette: (com.muse.app.data.model.PaletteRecord) -> Unit,
) {
    val papers = listOf(
        "WARM" to 0xFFF6F3EC.toInt(),
        "PURE" to 0xFFFFFFFF.toInt(),
        "GREY" to 0xFFE9E5DC.toInt(),
        "INK" to 0xFF161512.toInt()
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        MetaText("PAPER", color = MuseColors.Gray2, size = 9.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            papers.forEach { (name, argb) ->
                val selected = draft.paperArgb == argb
                Pressable(onClick = {
                    onEdit { it.copy(paperArgb = argb, inkArgb = com.muse.app.util.Colors.readableInk(argb)) }
                }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(Color(argb))
                                .border(
                                    1.dp,
                                    if (selected) MuseColors.Ink else MuseColors.HairLight
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        MetaText(
                            name,
                            color = if (selected) MuseColors.Ink else MuseColors.Gray3,
                            size = 8.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        MetaText("APPLY PALETTE", color = MuseColors.Gray2, size = 9.sp)
        Spacer(Modifier.height(10.dp))
        if (palettes.isEmpty()) {
            MetaText("NO SAVED PALETTES YET", color = MuseColors.Gray3.copy(alpha = 0.7f), size = 9.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(count = palettes.size, key = { palettes[it].id }) { index ->
                    val record = palettes[index]
                    Pressable(onClick = { onApplyPalette(record) }) {
                        Row(
                            Modifier
                                .width(96.dp)
                                .height(40.dp)
                                .border(1.dp, MuseColors.Ink.copy(alpha = 0.15f))
                        ) {
                            record.colors.forEach { argb ->
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color(argb)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetaText(label, color = MuseColors.Gray2, size = 10.sp)
        Pressable(onClick = { onChange(!checked) }) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(if (checked) MuseColors.Ink else Color.Transparent)
                    .border(1.dp, MuseColors.Ink)
            )
        }
    }
}

@Composable
private fun LineField(
    label: String,
    value: String,
    ink: Color,
    onChange: (String) -> Unit,
) {
    Column {
        MetaText(label, color = MuseColors.Gray2, size = 9.sp)
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.take(24)) },
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                color = ink
            ),
            cursorBrush = SolidColor(ink),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
        )
        Hairline(MuseColors.HairLight)
    }
}
