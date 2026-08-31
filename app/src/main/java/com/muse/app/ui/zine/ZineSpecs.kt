package com.muse.app.ui.zine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.data.model.ZineCoverStyle
import com.muse.app.data.model.ZinePageLayout
import com.muse.app.data.model.ZinePaper
import com.muse.app.data.model.ZineRecord
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.theme.MuseColors
import com.muse.app.util.Fmt
import kotlin.random.Random

const val BASE_ZINE_DP = 360
const val ZINE_ASPECT = 0.75f

data class PaperSpec(
    val id: String,
    val bg: Color,
    val grainAlpha: Float,
    val dotCount: Int,
    val vignette: Boolean,
    val saturation: Float,
    val label: String,
)

object ZinePapers {
    fun spec(paper: ZinePaper): PaperSpec = when (paper) {
        ZinePaper.MATTE -> PaperSpec(
            id = "MATTE", bg = Color(0xFFF6F3EC),
            grainAlpha = 0.05f, dotCount = 260, vignette = false, saturation = 1f,
            label = "哑光 MATTE",
        )
        ZinePaper.GLOSS -> PaperSpec(
            id = "GLOSS", bg = Color(0xFFFFFFFF),
            grainAlpha = 0f, dotCount = 0, vignette = true, saturation = 1f,
            label = "光泽 GLOSS",
        )
        ZinePaper.NEWSPRINT -> PaperSpec(
            id = "NEWSPRINT", bg = Color(0xFFE9E5DA),
            grainAlpha = 0.11f, dotCount = 520, vignette = false, saturation = 0.76f,
            label = "新闻纸 NEWSPRINT",
        )
    }
}

fun zineUri(mediaId: Long): String = "content://media/external/images/media/$mediaId"

fun zineRange(dates: Map<Long, Long>): String {
    val values = dates.values.toSortedSet()
    if (values.isEmpty()) return ""
    return if (values.size == 1) {
        Fmt.photoDate(values.first())
    } else {
        Fmt.coverRange(values.first(), values.last())
    }
}

fun resolvedZineLayout(record: ZineRecord, pageIndex: Int): ZinePageLayout =
    record.pages.getOrNull(pageIndex)?.layout ?: record.defaultLayout

fun zineTitle(record: ZineRecord): String = record.title.ifBlank { "UNTITLED" }.uppercase()

private fun satFilter(spec: PaperSpec): ColorFilter? =
    if (spec.saturation < 1f) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(spec.saturation) })
    } else null

@Composable
fun ScaledZinePage(
    scale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val current = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(current.density * scale, current.fontScale)
    ) {
        Box(modifier.width(BASE_ZINE_DP.dp).aspectRatio(ZINE_ASPECT)) { content() }
    }
}

@Composable
fun rememberZineScale(maxWidthPx: Int, maxHeightPx: Int): Float {
    val density = LocalDensity.current
    if (maxWidthPx <= 0 || maxHeightPx <= 0) return 1f
    val baseW = with(density) { BASE_ZINE_DP.dp.toPx() }
    val baseH = baseW / ZINE_ASPECT
    return minOf(maxWidthPx / baseW, maxHeightPx / baseH)
}

@Composable
fun ZineSheet(
    paper: ZinePaper,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val spec = ZinePapers.spec(paper)
    Box(modifier.aspectRatio(ZINE_ASPECT).background(spec.bg)) {
        content()
        PaperTexture(spec, Modifier.matchParentSize())
    }
}

@Composable
internal fun PaperTexture(spec: PaperSpec, modifier: Modifier = Modifier) {
    if (spec.grainAlpha <= 0f && !spec.vignette) return
    val dots = remember(spec.id) {
        List(spec.dotCount) {
            Random(spec.id.hashCode() + it).nextFloat() to Random(it * 31 + 7).nextFloat()
        }
    }
    Canvas(modifier) {
        if (spec.grainAlpha > 0f) {
            val dot = 1.1.dp.toPx()
            dots.forEach { (fx, fy) ->
                drawRect(
                    color = Color.Black.copy(alpha = spec.grainAlpha),
                    topLeft = Offset(fx * size.width, fy * size.height),
                    size = androidx.compose.ui.geometry.Size(dot, dot)
                )
            }
        }
        if (spec.vignette) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.07f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.75f
                )
            )
        }
    }
}

@Composable
private fun PlateSlot(
    mediaId: Long,
    paper: ZinePaper,
    modifier: Modifier = Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale = androidx.compose.ui.layout.ContentScale.Crop,
) {
    val spec = ZinePapers.spec(paper)
    Box(modifier.background(MuseColors.PaperGrey)) {
        PhotoImage(
            uri = zineUri(mediaId),
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            thumbnail = false,
            crossfade = 0,
            colorFilter = satFilter(spec)
        )
    }
}

private fun serif(size: Float, color: Color, tracking: Float = 0.5f, weight: FontWeight = FontWeight.W500) = TextStyle(
    fontSize = size.sp,
    fontWeight = weight,
    fontFamily = FontFamily.Serif,
    letterSpacing = tracking.sp,
    color = color
)

private fun mono(size: Float, color: Color, tracking: Float = 1.6f) = TextStyle(
    fontSize = size.sp,
    letterSpacing = tracking.sp,
    fontFamily = FontFamily.Monospace,
    color = color
)

@Composable
fun ZineCoverContent(record: ZineRecord, dates: Map<Long, Long>) {
    val ink = MuseColors.Ink
    val range = zineRange(dates)
    when (record.coverStyle) {
        ZineCoverStyle.FULL_BLEED -> FullBleedCover(record, range)
        ZineCoverStyle.FRAME -> FramedCover(record, range, ink)
        ZineCoverStyle.EDITORIAL -> EditorialCover(record, range, ink)
    }
}

@Composable
private fun FullBleedCover(record: ZineRecord, range: String) {
    Box(Modifier.fillMaxSize()) {
        PlateSlot(record.coverMediaId, record.paper, Modifier.fillMaxSize())
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 16.dp)
        ) {
            MetaText("MUSE", color = Color.White.copy(alpha = 0.72f), size = 8.sp, letterSpacing = 4.sp)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MuseColors.Ink.copy(alpha = 0.92f))
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
        ) {
            Text(
                text = zineTitle(record),
                style = serif(17f, MuseColors.White, 1.2f),
                maxLines = 1
            )
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetaText(range, color = MuseColors.Gray1.copy(alpha = 0.85f), size = 8.sp)
                MetaText("${record.pageCount} PLATES", color = MuseColors.Gray2, size = 8.sp)
            }
        }
    }
}

@Composable
private fun FramedCover(record: ZineRecord, range: String, ink: Color) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, ink.copy(alpha = 0.35f))
                .padding(12.dp)
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                PlateSlot(
                    record.coverMediaId, record.paper,
                    Modifier.fillMaxWidth().weight(1f),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                Spacer(Modifier.height(13.dp))
                Text(text = zineTitle(record), style = serif(15f, ink, 1.5f), maxLines = 1)
                Spacer(Modifier.height(6.dp))
                MetaText(range, color = ink.copy(alpha = 0.55f), size = 8.5.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        MetaText("MUSE EDITION", color = ink.copy(alpha = 0.45f), size = 7.5.sp, letterSpacing = 3.sp)
    }
}

@Composable
private fun EditorialCover(record: ZineRecord, range: String, ink: Color) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp)
    ) {
        Text(text = zineTitle(record), style = serif(24f, ink, 0.5f), maxLines = 2)
        Spacer(Modifier.height(9.dp))
        Box(Modifier.width(44.dp).height(1.dp).background(ink.copy(alpha = 0.7f)))
        Spacer(Modifier.height(14.dp))
        PlateSlot(record.coverMediaId, record.paper, Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetaText(range, color = ink.copy(alpha = 0.55f), size = 8.sp)
            MetaText("MUSE", color = ink.copy(alpha = 0.45f), size = 8.sp, letterSpacing = 4.sp)
        }
    }
}

@Composable
fun ZineInnerPageContent(record: ZineRecord, pageIndex: Int, dates: Map<Long, Long>) {
    val page = record.pages.getOrNull(pageIndex)
    if (page == null || pageIndex < 0) {
        MissingPlate(Modifier.fillMaxSize())
        return
    }
    val ink = MuseColors.Ink
    val folio = folioLabel(pageIndex, record.pageCount)
    val dateLabel = dates[page.mediaId]?.let { Fmt.photoDate(it) } ?: ""
    when (resolvedZineLayout(record, pageIndex)) {
        ZinePageLayout.GALLERY -> GalleryPage(record, pageIndex, folio)
        ZinePageLayout.MUSEUM -> MuseumPage(record, pageIndex, folio, dateLabel, ink)
        ZinePageLayout.JOURNAL -> JournalPage(record, pageIndex, folio, dateLabel, ink)
    }
}

@Composable
private fun MissingPlate(modifier: Modifier = Modifier) {
    Box(modifier.background(MuseColors.PaperGrey), contentAlignment = Alignment.Center) {
        MetaText("PHOTOGRAPH MISSING", color = MuseColors.Gray2, size = 9.sp)
    }
}

internal fun folioLabel(index: Int, count: Int): String =
    "${(index + 1).toString().padStart(2, '0')} — ${count.toString().padStart(2, '0')}"

@Composable
private fun GalleryPage(record: ZineRecord, pageIndex: Int, folio: String) {
    Box(Modifier.fillMaxSize()) {
        PlateSlot(
            record.pages[pageIndex].mediaId, record.paper, Modifier.fillMaxSize()
        )
        Text(
            text = folio,
            style = mono(7.5f, Color.White.copy(alpha = 0.78f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun MuseumPage(
    record: ZineRecord,
    pageIndex: Int,
    folio: String,
    dateLabel: String,
    ink: Color,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetaText(zineTitle(record), color = ink.copy(alpha = 0.42f), size = 7.sp)
            MetaText("PLATE ${pageIndex + 1}", color = ink.copy(alpha = 0.42f), size = 7.sp)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, ink.copy(alpha = 0.28f))
                .padding(10.dp)
        ) {
            PlateSlot(
                record.pages[pageIndex].mediaId, record.paper, Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }
        Spacer(Modifier.height(16.dp))
        if (dateLabel.isNotBlank()) {
            Text(text = dateLabel, style = mono(9f, ink))
            Spacer(Modifier.height(6.dp))
        }
        Box(Modifier.width(28.dp).height(1.dp).background(ink.copy(alpha = 0.5f)))
        Spacer(Modifier.height(8.dp))
        MetaText(folio, color = ink.copy(alpha = 0.4f), size = 7.5.sp)
    }
}

@Composable
private fun JournalPage(
    record: ZineRecord,
    pageIndex: Int,
    folio: String,
    dateLabel: String,
    ink: Color,
) {
    Column(Modifier.fillMaxSize()) {
        BoxWithWeight(0.56f) {
            PlateSlot(record.pages[pageIndex].mediaId, record.paper, Modifier.fillMaxSize())
        }
        Column(
            Modifier
                .weight(0.44f)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 20.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.55f)))
            Spacer(Modifier.height(14.dp))
            if (dateLabel.isNotBlank()) {
                Text(text = dateLabel, style = serif(14f, ink, 1f), maxLines = 1)
                Spacer(Modifier.height(7.dp))
            }
            MetaText("PLATE ${pageIndex + 1} · ${record.pageCount}", color = ink.copy(alpha = 0.5f), size = 8.sp)
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetaText(zineTitle(record), color = ink.copy(alpha = 0.4f), size = 7.5.sp)
                MetaText(folio, color = ink.copy(alpha = 0.4f), size = 7.5.sp)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BoxWithWeight(
    weightFraction: Float,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth().weight(weightFraction)) { content() }
}

@Composable
fun ZineColophonContent(record: ZineRecord, boundAt: Long) {
    val ink = MuseColors.Ink
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.width(30.dp).height(1.dp).background(ink.copy(alpha = 0.5f)))
        Spacer(Modifier.height(18.dp))
        Text(text = zineTitle(record), style = serif(16f, ink, 1.5f), maxLines = 1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        MetaText("MUSE · PRIVATE EDITION", color = ink.copy(alpha = 0.5f), size = 8.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        MetaText("BOUND ${Fmt.photoDate(boundAt)}", color = ink.copy(alpha = 0.35f), size = 8.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(30.dp).height(1.dp).background(ink.copy(alpha = 0.5f)))
    }
}
