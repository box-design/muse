package com.muse.app.ui.palette

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import com.muse.app.util.Colors

@Composable
fun PaletteToolScreen(
    application: MuseApplication,
    photoId: Long?,
    paletteId: Long?,
    onClose: () -> Unit,
    onApplyToPostcard: (Long) -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: PaletteViewModel = viewModel(key = "palette_${photoId ?: 0}_${paletteId ?: 0}") {
        PaletteViewModel(
            media = application.container.media,
            creations = application.container.creations,
            archiveRepository = application.container.archive,
            photoId = photoId,
            paletteId = paletteId
        )
    }

    val photoUri by vm.photoUri.collectAsStateWithLifecycle()
    val swatches by vm.swatches.collectAsStateWithLifecycle()
    val name by vm.name.collectAsStateWithLifecycle()
    val editingIndex by vm.editingIndex.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2000)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(MuseColors.Void)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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
                        tint = MuseColors.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(36.dp).padding(9.dp)
                    )
                }
                Text(
                    text = "PALETTE",
                    style = TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MuseColors.White
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Pressable(onClick = { vm.save() }) {
                        MetaText("SAVE", color = MuseColors.White, size = 10.sp, weight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                    Pressable(onClick = { vm.collect() }) {
                        MetaText("COLLECT", color = MuseColors.Gray1, size = 10.sp)
                    }
                }
            }

            if (isAtLeastMediumWidth()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        Modifier.weight(1.15f).fillMaxHeight(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        photoUri?.let { uri ->
                            PhotoImage(
                                uri = uri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .aspectRatio(1.5f),
                                thumbnail = false
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        SwatchArea(
                            loading = loading,
                            swatches = swatches,
                            editingIndex = editingIndex,
                            onSelect = vm::selectEdit
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Box(Modifier.width(1.dp).fillMaxHeight().background(MuseColors.HairDark))

                    Column(Modifier.weight(1f).fillMaxHeight().padding(top = 14.dp)) {
                        AnimatedVisibility(
                            visible = editingIndex != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            val idx = editingIndex ?: -1
                            if (idx >= 0 && idx < swatches.size) {
                                HsvSliders(
                                    index = idx,
                                    readInitial = vm::hsvOf,
                                    onChange = { h, s, v -> vm.adjust(idx, h, s, v) }
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        NamingBlock(
                            name = name,
                            onName = { vm.setName(it) },
                            onApply = {
                                val mediaId = vm.applyToPostcard()
                                if (mediaId != null) onApplyToPostcard(mediaId)
                            }
                        )

                        ToastBlock(message)
                        Spacer(Modifier.height(20.dp))
                    }
                }
            } else {
                photoUri?.let { uri ->
                    PhotoImage(
                        uri = uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(1.5f),
                        thumbnail = false
                    )
                }

                Spacer(Modifier.height(24.dp))

                SwatchArea(
                    loading = loading,
                    swatches = swatches,
                    editingIndex = editingIndex,
                    onSelect = vm::selectEdit
                )

                AnimatedVisibility(
                    visible = editingIndex != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val idx = editingIndex ?: -1
                    if (idx >= 0 && idx < swatches.size) {
                        HsvSliders(
                            index = idx,
                            readInitial = vm::hsvOf,
                            onChange = { h, s, v -> vm.adjust(idx, h, s, v) }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                NamingBlock(
                    name = name,
                    onName = { vm.setName(it) },
                    onApply = {
                        val mediaId = vm.applyToPostcard()
                        if (mediaId != null) onApplyToPostcard(mediaId)
                    }
                )

                ToastBlock(message)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SwatchArea(
    loading: Boolean,
    swatches: List<Int>,
    editingIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    if (loading && swatches.isEmpty()) {
        PulsingHint("ANALYZING COLOR")
    } else if (!loading && swatches.isEmpty()) {
        MetaText(
            "NOTHING TO ANALYZE",
            color = MuseColors.Gray3,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    } else {
        SwatchStrip(swatches, editingIndex, onSelect)
    }
}

@Composable
private fun NamingBlock(
    name: String,
    onName: (String) -> Unit,
    onApply: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        MetaText("NAME THIS PALETTE — E.G. TOKYO NIGHT", color = MuseColors.Gray3, size = 9.sp)
        BasicTextField(
            value = name,
            onValueChange = onName,
            textStyle = TextStyle(
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif,
                color = MuseColors.White
            ),
            cursorBrush = SolidColor(MuseColors.White),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuseColors.HairDark))

        Spacer(Modifier.height(18.dp))
        Pressable(onClick = onApply) {
            Text(
                text = "APPLY TO POSTCARD",
                style = TextStyle(
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MuseColors.White
                )
            )
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun ToastBlock(message: String?) {
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, MuseColors.HairDark)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                MetaText(message ?: "", color = MuseColors.White, size = 9.5.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun SwatchStrip(colors: List<Int>, editingIndex: Int?, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEachIndexed { index, argb ->
            val selected = editingIndex == index
            Pressable(onClick = { onSelect(index) }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .background(Color(argb))
                            .then(
                                if (selected) Modifier.border(1.dp, MuseColors.White)
                                else Modifier.border(0.5f.dp, MuseColors.HairDark)
                            )
                    )
                    Spacer(Modifier.height(7.dp))
                    MetaText(
                        Colors.hex(argb),
                        color = if (selected) MuseColors.White else MuseColors.Gray3,
                        size = 8.sp,
                        align = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HsvSliders(
    index: Int,
    readInitial: (Int) -> FloatArray,
    onChange: (Float, Float, Float) -> Unit,
) {
    val seed = remember(index) { readInitial(index) }
    var h by remember(index) { mutableStateOf(seed[0] / 360f) }
    var s by remember(index) { mutableStateOf(seed[1]) }
    var v by remember(index) { mutableStateOf(seed[2]) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
        SliderRow("HUE", h) { h = it; onChange(h, s, v) }
        SliderRow("SAT", s) { s = it; onChange(h, s, v) }
        SliderRow("LUM", v) { v = it; onChange(h, s, v) }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MetaText(label, color = MuseColors.Gray3, size = 9.sp, modifier = Modifier.width(44.dp))
        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = MuseColors.White,
                activeTrackColor = MuseColors.Gray1,
                inactiveTrackColor = MuseColors.Gray3.copy(alpha = 0.4f)
            ),
            modifier = Modifier.weight(1f).height(28.dp)
        )
        MetaText(
            "${(value * 100).toInt()}",
            color = MuseColors.Gray3,
            size = 9.sp,
            modifier = Modifier.width(34.dp),
            align = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun PulsingHint(text: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "alpha"
    )
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        MetaText(
            text,
            color = MuseColors.Gray2,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}
