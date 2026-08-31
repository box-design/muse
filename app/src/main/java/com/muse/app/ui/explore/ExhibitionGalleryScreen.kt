package com.muse.app.ui.explore

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.data.Curator
import com.muse.app.data.model.Exhibition
import com.muse.app.data.model.Photo
import com.muse.app.ui.components.Hairline
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import com.muse.app.ui.viewer.ViewerRequest
import com.muse.app.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val curator: Curator,
    private val key: String,
    private val store: com.muse.app.data.MuseStore,
) : ViewModel() {

    private val _exhibition = MutableStateFlow<Exhibition?>(null)
    val exhibition: kotlinx.coroutines.flow.StateFlow<Exhibition?> = _exhibition

    init {
        viewModelScope.launch {
            _exhibition.value = runCatching { curator.ensureLoaded(key) }.getOrNull()
        }
    }

    fun rename(raw: String) {
        viewModelScope.launch {
            curator.rename(key, raw)
            _exhibition.value = runCatching { curator.ensureLoaded(key) }.getOrNull()
        }
    }
}

private sealed interface Block {
    data class Divider(val photo: Photo) : Block
    data class Hero(val photo: Photo) : Block
    data class PairBlock(val a: Photo, val b: Photo) : Block
    data class Wide(val photo: Photo) : Block
    data class Small(val photo: Photo) : Block
    data class Tall(val photo: Photo) : Block
}

private fun buildBlocks(photos: List<Photo>): List<Block> {
    val blocks = mutableListOf<Block>()
    if (photos.isEmpty()) return blocks
    val rhythm = intArrayOf(0, 1, 2, 3, 4)
    var r = 0
    var i = 0
    var lastDay = -1

    fun dividerIfNeeded(p: Photo) {
        val day = Fmt.dayKey(p.dateMs)
        if (lastDay != -1 && day != lastDay) blocks += Block.Divider(p)
        lastDay = day
    }

    while (i < photos.size) {
        when (rhythm[r % rhythm.size]) {
            0 -> {
                dividerIfNeeded(photos[i]); blocks += Block.Hero(photos[i]); i += 1
            }
            1 -> {
                dividerIfNeeded(photos[i])
                if (i + 1 < photos.size) {
                    blocks += Block.PairBlock(photos[i], photos[i + 1]); i += 2
                } else {
                    blocks += Block.Hero(photos[i]); i += 1
                }
            }
            2 -> {
                blocks += Block.Divider(photos[i]); lastDay = Fmt.dayKey(photos[i].dateMs)
                blocks += Block.Wide(photos[i]); i += 1
            }
            3 -> {
                dividerIfNeeded(photos[i]); blocks += Block.Small(photos[i]); i += 1
            }
            else -> {
                dividerIfNeeded(photos[i]); blocks += Block.Tall(photos[i]); i += 1
            }
        }
        r++
    }
    return blocks
}

@Composable
fun ExhibitionGalleryScreen(
    application: MuseApplication,
    key: String,
    onClose: () -> Unit,
    onOpenViewer: (ViewerRequest) -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: GalleryViewModel = viewModel(key = "gallery_$key") {
        GalleryViewModel(application.container.curator, key, application.container.store)
    }
    val exhibition by vm.exhibition.collectAsStateWithLifecycle()
    val ex = exhibition ?: return

    val blocks = remember(ex.photos) { buildBlocks(ex.photos) }
    val rects = remember(ex.key) { mutableMapOf<Long, Rect>() }
    var renaming by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MuseColors.Void)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sidePad = ((maxWidth - 680.dp) / 2).coerceAtLeast(0.dp)
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = sidePad, end = sidePad, bottom = 60.dp)
            ) {
                item(key = "header") { GalleryHeader(ex, onRename = { renaming = true }) }

                items(blocks.size, key = { "b$it" }) { idx ->
                    val block = blocks[idx]
                    GalleryBlock(
                        block = block,
                        place = ex.place,
                        onPhoto = { photo ->
                            val index = ex.photos.indexOfFirst { it.id == photo.id }.coerceAtLeast(0)
                            onOpenViewer(
                                ViewerRequest(
                                    photos = ex.photos,
                                    startIndex = index,
                                    origin = rects[photo.id],
                                    contextTitle = ex.name
                                )
                            )
                        },
                        registerRect = { photo, rect -> rects[photo.id] = rect }
                    )
                }

                item(key = "footer") { GalleryFooter(count = ex.photos.size) }
            }
        }

        Pressable(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(14.dp)) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(com.muse.app.R.drawable.ic_vector_arrow_back),
                    contentDescription = null,
                    tint = MuseColors.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (renaming) {
        RenameDialog(initial = ex.name, onDismiss = { renaming = false }, onSave = {
            vm.rename(it)
            renaming = false
        })
    }
}

@Composable
private fun GalleryHeader(ex: Exhibition, onRename: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, start = 28.dp, end = 28.dp)) {
        Text(
            text = ex.name.uppercase(),
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.W500,
                fontFamily = FontFamily.Serif,
                letterSpacing = 2.sp,
                color = MuseColors.White
            ),
            maxLines = 2
        )
        Spacer(Modifier.height(12.dp))
        MetaText(Fmt.coverRange(ex.startMs, ex.endMs), color = MuseColors.Gray1, size = 11.sp)
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetaText(Fmt.artifacts(ex.photos.size), color = MuseColors.Gray3, size = 11.sp)
            Pressable(onClick = onRename) {
                MetaText("RENAME", color = MuseColors.Gray3.copy(alpha = 0.9f), size = 11.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Hairline(MuseColors.HairDark)
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun GalleryFooter(count: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
        Spacer(Modifier.height(26.dp))
        Hairline(MuseColors.HairDark)
        Spacer(Modifier.height(18.dp))
        MetaText("MUSE — ${Fmt.artifacts(count)}", color = MuseColors.Gray3, size = 10.sp)
        Spacer(Modifier.height(6.dp))
        MetaText("CURATED FROM YOUR PHOTOGRAPHS", color = MuseColors.Gray3.copy(alpha = 0.7f), size = 10.sp)
        navigationBarsPaddingSpacer()
    }
}

@Composable
private fun navigationBarsPaddingSpacer() {
    Spacer(Modifier.navigationBarsPadding().height(30.dp))
}

@Composable
private fun GalleryBlock(
    block: Block,
    place: String?,
    onPhoto: (Photo) -> Unit,
    registerRect: (Photo, Rect) -> Unit,
) {
    when (block) {
        is Block.Divider -> DateDivider(block.photo, place)
        is Block.Hero -> BookPhoto(
            photo = block.photo,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).aspectRatio(0.78f),
            onPhoto = onPhoto,
            registerRect = registerRect
        )
        is Block.PairBlock -> Row(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BookPhoto(
                photo = block.a,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onPhoto = onPhoto,
                registerRect = registerRect
            )
            BookPhoto(
                photo = block.b,
                modifier = Modifier.weight(1f).aspectRatio(1f),
                onPhoto = onPhoto,
                registerRect = registerRect
            )
        }
        is Block.Wide -> BookPhoto(
            photo = block.photo,
            modifier = Modifier.fillMaxWidth().aspectRatio(if (block.photo.aspect >= 1f) 1.5f else 1.15f),
            onPhoto = onPhoto,
            registerRect = registerRect
        )
        is Block.Small -> Row(Modifier.fillMaxWidth()) {
            BookPhoto(
                photo = block.photo,
                modifier = Modifier.padding(start = 36.dp).width(150.dp).aspectRatio(0.8f),
                onPhoto = onPhoto,
                registerRect = registerRect
            )
        }
        is Block.Tall -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            BookPhoto(
                photo = block.photo,
                modifier = Modifier.padding(end = 36.dp).width(210.dp).aspectRatio(0.72f),
                onPhoto = onPhoto,
                registerRect = registerRect
            )
        }
    }
    Spacer(Modifier.height(30.dp))
}

@Composable
private fun DateDivider(photo: Photo, place: String?) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MetaText(
            Fmt.photoDate(photo.dateMs),
            color = MuseColors.White,
            size = 12.sp,
            align = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!place.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            MetaText(
                place,
                color = MuseColors.Gray3,
                size = 10.sp,
                align = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun BookPhoto(
    photo: Photo,
    modifier: Modifier,
    onPhoto: (Photo) -> Unit,
    registerRect: (Photo, Rect) -> Unit,
) {
    var shown by remember(photo.id) { mutableStateOf(false) }
    LaunchedEffect(photo.id) { shown = true }
    val entrance by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "entrance"
    )

    Pressable(
        onClick = { onPhoto(photo) },
        modifier = modifier.graphicsLayer {
            alpha = 0.75f + 0.25f * entrance
            scaleX = 0.97f + 0.03f * entrance
            scaleY = 0.97f + 0.03f * entrance
        }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .onGloballyPositioned { coords -> registerRect(photo, coords.boundsInRoot()) }
        ) {
            PhotoImage(uri = photo.uri, modifier = Modifier.fillMaxSize(), thumbnail = true)
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .background(MuseColors.ScrimVoid)
                .then(
                    Modifier.border(1.dp, MuseColors.HairDark)
                )
                .padding(26.dp)
                .width(280.dp)
        ) {
            Column {
                MetaText("RENAME EXHIBITION", color = MuseColors.Gray2, size = 10.sp)
                Spacer(Modifier.height(18.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(28) },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MuseColors.White
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MuseColors.White),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Hairline(MuseColors.Gray3.copy(alpha = 0.6f))
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Pressable(onClick = onDismiss) {
                        MetaText("CANCEL", color = MuseColors.Gray2, size = 11.sp)
                    }
                    Pressable(onClick = { onSave(text.trim().uppercase()) }) {
                        MetaText("SAVE", color = MuseColors.White, size = 11.sp, weight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
