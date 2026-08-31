package com.muse.app.ui.zine

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.ui.adaptive.LocalWindowWidthClass
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.SectionTitle
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.create.PickerViewModel
import com.muse.app.ui.theme.MuseColors

const val ZINE_MIN_PAGES = 8
const val ZINE_MAX_PAGES = 20

@Composable
fun ZinePickerScreen(
    application: MuseApplication,
    onClose: () -> Unit,
    onDone: (List<Long>) -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: PickerViewModel = viewModel { PickerViewModel(application.container.media) }
    val photos by vm.photos.collectAsStateWithLifecycle()
    val selected = remember { mutableStateListOf<Long>() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MuseColors.Void)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                SectionTitle("SELECT — ZINE")
                Spacer(Modifier.height(4.dp))
                MetaText(
                    text = "${selected.size} SELECTED · PICK $ZINE_MIN_PAGES–$ZINE_MAX_PAGES",
                    color = MuseColors.Gray3,
                    size = 9.sp
                )
            }
            Pressable(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_vector_close),
                    contentDescription = null,
                    tint = MuseColors.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(38.dp).padding(10.dp)
                )
            }
        }

        when {
            photos == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MetaText("LOADING", color = MuseColors.Gray3)
            }
            photos!!.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MetaText("NO PHOTOGRAPHS FOUND", color = MuseColors.Gray3)
            }
            else -> {
                val columns = when (LocalWindowWidthClass.current) {
                    WindowWidthSizeClass.Expanded -> GridCells.Fixed(5)
                    WindowWidthSizeClass.Medium -> GridCells.Fixed(4)
                    else -> GridCells.Fixed(3)
                }
                LazyVerticalGrid(
                    columns = columns,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
                ) {
                    items(count = photos!!.size, key = { photos!![it].id }) { index ->
                        val photo = photos!![index]
                        val order = selected.indexOf(photo.id)
                        PickerCell(
                            uri = photo.uri,
                            order = order,
                            onToggle = {
                                if (order >= 0) selected.remove(photo.id)
                                else if (selected.size < ZINE_MAX_PAGES) selected.add(photo.id)
                            }
                        )
                    }
                }
            }
        }

        PickerFooter(
            count = selected.size,
            onDone = { onDone(selected.toList()) }
        )
    }
}

@Composable
private fun PickerCell(uri: String, order: Int, onToggle: () -> Unit) {
    val picked = order >= 0
    val scrim by animateColorAsState(
        targetValue = if (picked) Color.Black.copy(alpha = 0.42f) else Color.Transparent,
        animationSpec = tween(160),
        label = "zinePickScrim"
    )
    val ringColor by animateColorAsState(
        targetValue = if (picked) MuseColors.White else Color.Transparent,
        animationSpec = tween(160),
        label = "zinePickRing"
    )
    Pressable(onClick = onToggle, scaleDown = 0.94f) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(1.dp, ringColor)
        ) {
            PhotoImage(
                uri = uri,
                modifier = Modifier.fillMaxSize(),
                thumbnail = true,
                crossfade = 120
            )
            Box(Modifier.fillMaxSize().background(scrim))
            if (picked) {
                Text(
                    text = (order + 1).toString().padStart(2, '0'),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W500,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MuseColors.White
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                )
            }
        }
    }
}

@Composable
private fun PickerFooter(count: Int, onDone: () -> Unit) {
    val ready = count in ZINE_MIN_PAGES..ZINE_MAX_PAGES
    Column(
        Modifier
            .fillMaxWidth()
            .background(MuseColors.Void)
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            MetaText(
                if (ready) "ORDER FOLLOWS YOUR TAPS" else "SELECT AT LEAST $ZINE_MIN_PAGES PHOTOGRAPHS",
                color = MuseColors.Gray3,
                size = 8.5.sp
            )
        }
        Pressable(onClick = { if (ready) onDone() }, scaleDown = if (ready) 0.97f else 1f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(if (ready) MuseColors.White else MuseColors.HairDark)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "BIND $count PHOTOGRAPHS",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = if (ready) MuseColors.Void else MuseColors.Gray3
                    )
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
