package com.muse.app.ui.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.data.model.CropMode
import com.muse.app.data.model.PostcardRecord
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.theme.MuseColors

const val BASE_CARD_DP = 320

val LocalStaticPhoto = staticCompositionLocalOf<ImageBitmap?> { null }

data class TemplateSpec(
    val id: String,
    val displayName: String,
    val padDp: Float,
    val titleSp: Float,
    val titleFont: FontFamily,
    val titleWeight: FontWeight,
    val titleTrackingSp: Float,
    val metaSp: Float,
    val centered: Boolean = false,
    val rule: Boolean = false,
    val frame: Boolean = false,
    val stamp: Boolean = false,
    val ghostDate: Boolean = false,
    val topMetaBlock: Boolean = false,
    val bigPlace: Boolean = false,
)

object Templates {
    val all: List<TemplateSpec> = listOf(
        TemplateSpec("classic", "CLASSIC", 18f, 15f, FontFamily.SansSerif, FontWeight.W500, 1.5f, 9f),
        TemplateSpec("editorial", "EDITORIAL", 18f, 20f, FontFamily.Serif, FontWeight.W500, 0.5f, 9f, rule = true),
        TemplateSpec("museum", "MUSEUM", 20f, 13f, FontFamily.Serif, FontWeight.W400, 3f, 8.5f, centered = true, rule = true),
        TemplateSpec("travel", "TRAVEL", 18f, 24f, FontFamily.Serif, FontWeight.W500, 1f, 9.5f, bigPlace = true),
        TemplateSpec("minimal", "MINIMAL", 26f, 10f, FontFamily.SansSerif, FontWeight.Normal, 3f, 9f, centered = true),
        TemplateSpec("archive", "ARCHIVE", 18f, 11f, FontFamily.SansSerif, FontWeight.W500, 1f, 9f, topMetaBlock = true),
        TemplateSpec("date", "DATE", 16f, 11f, FontFamily.Monospace, FontWeight.W500, 1f, 9f, ghostDate = true),
        TemplateSpec("postcard", "POSTCARD", 14f, 11f, FontFamily.Monospace, FontWeight.W500, 2f, 9f, frame = true, stamp = true)
    )

    fun byId(id: String): TemplateSpec = all.find { it.id == id } ?: all.first()
}

data class DraftView(
    val uri: String,
    val photoAspect: Float,
    val templateId: String,
    val cropMode: CropMode,
    val offsetX: Float,
    val offsetY: Float,
    val title: String,
    val dateLabel: String,
    val placeLabel: String,
    val numberLabel: String,
    val showDate: Boolean,
    val showLocation: Boolean,
    val showNumber: Boolean,
    val paperArgb: Int,
    val inkArgb: Int,
    val onPan: ((Float, Float) -> Unit)? = null,
) {
    val spec: TemplateSpec get() = Templates.byId(templateId)
}

fun draftViewOf(record: PostcardRecord): DraftView = DraftView(
    uri = "content://media/external/images/media/${record.mediaId}",
    photoAspect = record.photoAspect,
    templateId = record.templateId,
    cropMode = record.cropMode,
    offsetX = record.offsetX,
    offsetY = record.offsetY,
    title = record.title,
    dateLabel = record.dateLabel,
    placeLabel = record.placeLabel,
    numberLabel = record.numberLabel,
    showDate = record.showDate,
    showLocation = record.showLocation,
    showNumber = record.showNumber,
    paperArgb = record.paperArgb,
    inkArgb = record.inkArgb
)

fun resolvedTitle(view: DraftView): String =
    view.title.ifBlank { view.placeLabel.ifBlank { "UNTITLED" } }.uppercase()

private fun metaLine(view: DraftView): String {
    val parts = mutableListOf<String>()
    if (view.showDate && view.dateLabel.isNotBlank()) parts += view.dateLabel
    if (view.showLocation && view.placeLabel.isNotBlank()) parts += view.placeLabel
    return parts.joinToString("  ·  ")
}

@Composable
fun ScaledPostcard(
    view: DraftView,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val current = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(current.density * scale, current.fontScale)
    ) {
        PostcardCanvas(view, modifier.width(BASE_CARD_DP.dp))
    }
}

@Composable
fun rememberCardScale(availableWidthPx: Int): Float {
    val density = LocalDensity.current
    return if (availableWidthPx <= 0) {
        1f
    } else {
        availableWidthPx.toFloat() / with(density) { BASE_CARD_DP.dp.toPx() }
    }
}

@Composable
fun PostcardCanvas(view: DraftView, modifier: Modifier = Modifier) {
    val paper = Color(view.paperArgb)
    val ink = Color(view.inkArgb)
    val spec = view.spec
    val pad = spec.padDp.dp

    Box(modifier.aspectRatio(0.75f).background(paper)) {
        when (spec.id) {
            "editorial" -> EditorialLayout(view, ink, pad)
            "museum" -> MuseumLayout(view, ink, pad)
            "travel" -> TravelLayout(view, ink, pad)
            "minimal" -> MinimalLayout(view, ink, pad)
            "archive" -> ArchiveLayout(view, ink, pad)
            "date" -> DateGhostLayout(view, ink)
            "postcard" -> PostcardLayout(view, ink)
            else -> ClassicLayout(view, ink, pad)
        }
    }
}

@Composable
private fun ClassicLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(Modifier.fillMaxSize().padding(pad)) {
        PanSlot(view, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(15.dp))
        Text(
            text = resolvedTitle(view),
            style = TextStyle(
                fontSize = view.spec.titleSp.sp,
                fontWeight = view.spec.titleWeight,
                fontFamily = view.spec.titleFont,
                letterSpacing = view.spec.titleTrackingSp.sp,
                color = ink
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = metaLine(view),
                style = mono(view.spec.metaSp, ink.copy(alpha = 0.72f), 1.2f)
            )
            if (view.showNumber) {
                Text(
                    text = view.numberLabel,
                    style = mono(view.spec.metaSp, ink.copy(alpha = 0.72f), 1.2f)
                )
            }
        }
    }
}

@Composable
private fun EditorialLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(Modifier.fillMaxSize()) {
        BoxWithWeight(0.62f) { PanSlot(view, Modifier.fillMaxSize()) }
        Column(Modifier.weight(0.38f).fillMaxWidth().padding(pad)) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = resolvedTitle(view),
                style = TextStyle(
                    fontSize = view.spec.titleSp.sp,
                    fontWeight = view.spec.titleWeight,
                    fontFamily = view.spec.titleFont,
                    letterSpacing = view.spec.titleTrackingSp.sp,
                    color = ink
                ),
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            HairlineInline(ink.copy(alpha = 0.55f))
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = metaLine(view).ifBlank { "MUSE EDITION" },
                    style = mono(view.spec.metaSp, ink.copy(alpha = 0.72f), 1.4f)
                )
                if (view.showNumber) {
                    Text(
                        text = view.numberLabel,
                        style = mono(view.spec.metaSp, ink.copy(alpha = 0.72f), 1.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MuseumLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(
        Modifier.fillMaxSize().padding(pad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithWeight(0.58f) { PanSlot(view, Modifier.fillMaxSize()) }
        Spacer(Modifier.height(14.dp))
        Text(
            text = resolvedTitle(view),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = view.spec.titleSp.sp,
                fontWeight = view.spec.titleWeight,
                fontFamily = view.spec.titleFont,
                letterSpacing = view.spec.titleTrackingSp.sp,
                color = ink
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(9.dp))
        Box(Modifier.size(width = 34.dp, height = 1.dp).background(ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(8.dp))
        if (view.showDate && view.dateLabel.isNotBlank()) {
            Text(text = view.dateLabel, style = mono(view.spec.metaSp, ink, 1.6f))
            Spacer(Modifier.height(3.dp))
        }
        if (view.showLocation && view.placeLabel.isNotBlank()) {
            Text(
                text = view.placeLabel,
                style = mono(view.spec.metaSp, ink.copy(alpha = 0.65f), 1.6f)
            )
            Spacer(Modifier.height(3.dp))
        }
        if (view.showNumber && view.numberLabel.isNotBlank()) {
            Text(
                text = view.numberLabel,
                style = mono(view.spec.metaSp - 0.5f, ink.copy(alpha = 0.5f), 1.6f)
            )
        }
    }
}

@Composable
private fun TravelLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(Modifier.fillMaxSize()) {
        BoxWithWeight(0.56f) { PanSlot(view, Modifier.fillMaxSize()) }
        Column(Modifier.weight(0.44f).fillMaxWidth().padding(pad)) {
            Text(
                text = view.placeLabel.ifBlank { resolvedTitle(view) }.uppercase(),
                style = TextStyle(
                    fontSize = view.spec.titleSp.sp,
                    fontWeight = view.spec.titleWeight,
                    fontFamily = view.spec.titleFont,
                    letterSpacing = view.spec.titleTrackingSp.sp,
                    color = ink
                ),
                maxLines = 1
            )
            Spacer(Modifier.height(7.dp))
            if (view.showDate) {
                Text(text = view.dateLabel, style = mono(view.spec.metaSp, ink.copy(alpha = 0.75f), 1.6f))
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (view.showLocation) view.placeLabel.uppercase() else "",
                    style = mono(view.spec.metaSp, ink.copy(alpha = 0.5f), 1.6f)
                )
                if (view.showNumber) {
                    Text(
                        text = view.numberLabel,
                        style = mono(view.spec.metaSp, ink.copy(alpha = 0.5f), 1.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MinimalLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(
        Modifier.fillMaxSize().padding(pad),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PanSlot(view, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(22.dp))
        Text(
            text = resolvedTitle(view),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = view.spec.titleSp.sp,
                fontWeight = view.spec.titleWeight,
                fontFamily = view.spec.titleFont,
                letterSpacing = view.spec.titleTrackingSp.sp,
                color = ink
            ),
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = metaLine(view),
            textAlign = TextAlign.Center,
            style = mono(view.spec.metaSp, ink.copy(alpha = 0.55f), 1.8f),
            maxLines = 1
        )
    }
}

@Composable
private fun ArchiveLayout(view: DraftView, ink: Color, pad: Dp) {
    Column(Modifier.fillMaxSize().padding(pad)) {
        Text(text = view.numberLabel, style = mono(view.spec.metaSp + 1f, ink, 2.4f))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (view.showDate) view.dateLabel else "",
                style = mono(view.spec.metaSp, ink.copy(alpha = 0.7f), 1.6f)
            )
            Text(
                text = if (view.showLocation) view.placeLabel.uppercase() else "",
                style = mono(view.spec.metaSp, ink.copy(alpha = 0.7f), 1.6f)
            )
        }
        Spacer(Modifier.height(10.dp))
        HairlineInline(ink.copy(alpha = 0.5f))
        Spacer(Modifier.height(14.dp))
        PanSlot(view, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(10.dp))
        Text(
            text = resolvedTitle(view),
            style = mono(view.spec.metaSp, ink.copy(alpha = 0.85f), 1.8f),
            maxLines = 1
        )
    }
}

@Composable
private fun DateGhostLayout(view: DraftView, ink: Color) {
    Box(Modifier.fillMaxSize()) {
        PanSlot(view, Modifier.fillMaxSize())
        Text(
            text = if (view.showDate) view.dateLabel.replace(".", " . ") else "",
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.82f)
            ),
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(52.dp)
                .background(Color(view.paperArgb)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = resolvedTitle(view),
                modifier = Modifier.padding(start = 16.dp),
                style = mono(view.spec.metaSp + 1f, ink, 1.8f),
                maxLines = 1
            )
            if (view.showNumber) {
                Text(
                    text = view.numberLabel,
                    modifier = Modifier.padding(end = 16.dp),
                    style = mono(view.spec.metaSp, ink.copy(alpha = 0.65f), 1.8f)
                )
            }
        }
    }
}

@Composable
private fun PostcardLayout(view: DraftView, ink: Color) {
    Box(Modifier.fillMaxSize().padding(12.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .border(1.dp, ink.copy(alpha = 0.85f))
                .padding(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = resolvedTitle(view),
                        style = mono(view.spec.titleSp, ink, view.spec.titleTrackingSp),
                        maxLines = 2
                    )
                }
                if (view.spec.stamp) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .border(1.dp, ink.copy(alpha = 0.8f))
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PanSlot(view, Modifier.fillMaxWidth().weight(1f))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.6f)))
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (view.showDate) view.dateLabel else "",
                    style = mono(view.spec.metaSp, ink.copy(alpha = 0.75f), 1.4f)
                )
                if (view.showLocation) {
                    Text(
                        text = view.placeLabel.uppercase(),
                        style = mono(view.spec.metaSp, ink.copy(alpha = 0.75f), 1.4f)
                    )
                }
                if (view.showNumber) {
                    Text(
                        text = view.numberLabel,
                        style = mono(view.spec.metaSp, ink.copy(alpha = 0.75f), 1.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BoxWithWeight(weightFraction: Float, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().weight(weightFraction)) { content() }
}

@Composable
private fun HairlineInline(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}


@Composable
private fun mono(sizeSp: Float, color: Color, tracking: Float): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    letterSpacing = tracking.sp,
    fontFamily = FontFamily.Monospace,
    color = color
)

@Composable
private fun PanSlot(view: DraftView, modifier: Modifier, onPan: ((Float, Float) -> Unit)? = null) {
    val static = LocalStaticPhoto.current
    if (static != null) {
        StaticPanSlot(view, static, modifier)
        return
    }
    when (view.cropMode) {
        CropMode.FIT -> PhotoImage(
            uri = view.uri,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            thumbnail = false,
            crossfade = 0
        )
        else -> {
            var slotSize by remember { mutableStateOf(IntSize.Zero) }
            val over = overflows(slotSize.width.toFloat(), slotSize.height.toFloat(), view.photoAspect)
            val draggable = view.cropMode == CropMode.CROP
            PhotoImage(
                uri = view.uri,
                modifier = modifier
                    .onSizeChanged { slotSize = it }
                    .graphicsLayer {
                        translationX = view.offsetX * over.first
                        translationY = view.offsetY * over.second
                    }
                    .then(
                        if (draggable) {
                            Modifier.pointerInput(slotSize, view.photoAspect) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    val fx = if (over.first > 1f) amount.x / over.first else 0f
                                    val fy = if (over.second > 1f) amount.y / over.second else 0f
                                    view.onPan?.invoke(fx, fy)
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.Crop,
                thumbnail = false,
                crossfade = 0
            )
        }
    }
}

@Composable
private fun StaticPanSlot(view: DraftView, photo: ImageBitmap, modifier: Modifier) {
    when (view.cropMode) {
        CropMode.FIT -> Image(
            bitmap = photo,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
        else -> {
            var slotSize by remember { mutableStateOf(IntSize.Zero) }
            val over = overflows(slotSize.width.toFloat(), slotSize.height.toFloat(), view.photoAspect)
            Image(
                bitmap = photo,
                contentDescription = null,
                modifier = modifier
                    .onSizeChanged { slotSize = it }
                    .graphicsLayer {
                        translationX = view.offsetX * over.first
                        translationY = view.offsetY * over.second
                    },
                contentScale = ContentScale.Crop
            )
        }
    }
}

internal fun overflows(w: Float, h: Float, aspect: Float): Pair<Float, Float> {
    if (w <= 0f || h <= 0f) return 0f to 0f
    val iw = aspect * 1000f
    val ih = 1000f
    val s = maxOf(w / iw, h / ih)
    return ((iw * s - w) / 2f).coerceAtLeast(0f) to ((ih * s - h) / 2f).coerceAtLeast(0f)
}
