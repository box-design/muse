package com.muse.app.ui.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.Curator
import com.muse.app.data.model.Exhibition
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import com.muse.app.util.Fmt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class CoverViewModel(private val curator: Curator, private val key: String) : ViewModel() {
    private val _exhibition = kotlinx.coroutines.flow.MutableStateFlow<Exhibition?>(null)
    val exhibition: StateFlow<Exhibition?> = _exhibition

    init {
        viewModelScope.launch {
            _exhibition.value = runCatching { curator.ensureLoaded(key) }.getOrNull()
        }
    }
}

@Composable
fun ExhibitionCoverScreen(
    application: MuseApplication,
    key: String,
    onClose: () -> Unit,
    onEnterGallery: () -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: CoverViewModel = viewModel(key = "cover_$key") {
        CoverViewModel(application.container.curator, key)
    }
    val exhibition by vm.exhibition.collectAsStateWithLifecycle()

    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val maxUp = with(density) { 420.dp.toPx() }
    val progress = (-drag.value / maxUp).coerceIn(0f, 1f)

    val ex = exhibition
    Box(Modifier.fillMaxSize().background(MuseColors.Void)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { scope.launch { drag.stop() } },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            val next = (drag.value + dy).coerceIn(-maxUp * 1.2f, 40f)
                            scope.launch { drag.snapTo(next) }
                        },
                        onDragEnd = {
                            val settled = (-drag.value / maxUp).coerceIn(0f, 1f)
                            if (settled >= 0.4f) {
                                onEnterGallery()
                            } else if (abs(drag.value) > 1f) {
                                scope.launch { drag.animateTo(0f, spring(0.75f, 300f)) }
                            }
                        },
                        onDragCancel = {
                            scope.launch { drag.animateTo(0f, spring(0.75f, 300f)) }
                        }
                    )
                }
                .graphicsLayer {
                    translationY = drag.value * 0.8f
                    alpha = 1f - progress * 0.4f
                }
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("EXHIBITION")
                MetaText("MUSE", color = MuseColors.Gray3, letterSpacing = 4.sp)
            }

            val hintAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)

            if (isAtLeastMediumWidth()) {
                Row(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Pressable(onClick = onEnterGallery) {
                            Box(
                                Modifier
                                    .fillMaxHeight(0.86f)
                                    .aspectRatio(0.78f)
                                    .clipToBounds(),
                                contentAlignment = Alignment.Center
                            ) {
                                ex?.let {
                                    PhotoImage(uri = it.cover.uri, modifier = Modifier.fillMaxSize(), thumbnail = false)
                                }
                            }
                        }
                    }

                    Column(
                        Modifier.weight(1f).fillMaxHeight().padding(start = 44.dp, end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = (ex?.name ?: "").uppercase(),
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.W500,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 2.sp,
                                color = MuseColors.White
                            ),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(12.dp))
                        MetaText(
                            text = ex?.let { Fmt.coverRange(it.startMs, it.endMs) } ?: "",
                            color = MuseColors.Gray1,
                            size = 11.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        MetaText(
                            text = ex?.let { Fmt.artifacts(it.photos.size) } ?: "",
                            color = MuseColors.Gray3,
                            size = 11.sp
                        )
                        Spacer(Modifier.height(34.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_vector_arrow_up),
                                contentDescription = null,
                                tint = MuseColors.Gray2.copy(alpha = hintAlpha),
                                modifier = Modifier.size(15.dp).graphicsLayer {
                                    translationY = -progress * with(density) { 14.dp.toPx() }
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            MetaText(
                                "SWIPE UP TO ENTER",
                                color = MuseColors.Gray3.copy(alpha = hintAlpha),
                                letterSpacing = 3.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.navigationBarsPadding().height(18.dp))
            } else {
                Spacer(Modifier.weight(0.7f))

                Pressable(onClick = onEnterGallery, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .aspectRatio(0.78f)
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        ex?.let {
                            PhotoImage(uri = it.cover.uri, modifier = Modifier.fillMaxSize(), thumbnail = false)
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Text(
                    text = (ex?.name ?: "").uppercase(),
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.W500,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp,
                        color = MuseColors.White
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
                Spacer(Modifier.height(10.dp))
                MetaText(
                    text = ex?.let { Fmt.coverRange(it.startMs, it.endMs) } ?: "",
                    color = MuseColors.Gray1,
                    size = 11.sp,
                    modifier = Modifier.padding(horizontal = 31.dp)
                )
                Spacer(Modifier.height(5.dp))
                MetaText(
                    text = ex?.let { Fmt.artifacts(it.photos.size) } ?: "",
                    color = MuseColors.Gray3,
                    size = 11.sp,
                    modifier = Modifier.padding(horizontal = 31.dp)
                )

                Spacer(Modifier.weight(1f))

                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vector_arrow_up),
                        contentDescription = null,
                        tint = MuseColors.Gray2.copy(alpha = hintAlpha),
                        modifier = Modifier.size(15.dp).graphicsLayer {
                            translationY = -progress * with(density) { 14.dp.toPx() }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    MetaText(
                        "SWIPE UP TO ENTER",
                        color = MuseColors.Gray3.copy(alpha = hintAlpha),
                        letterSpacing = 3.sp
                    )
                }
            }
        }

        Pressable(
            onClick = onClose,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 54.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_vector_arrow_back),
                contentDescription = null,
                tint = MuseColors.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp).padding(2.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.SansSerif,
            color = MuseColors.White
        )
    )
}
