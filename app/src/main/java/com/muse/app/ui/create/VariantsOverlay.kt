package com.muse.app.ui.create

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.ui.adaptive.LocalWindowWidthClass
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.theme.MuseColors

@Composable
internal fun VariantsOverlay(
    base: Draft,
    numberLabel: String,
    onPick: (Draft) -> Unit,
    onClose: () -> Unit,
) {
    val variants = remember(base) { generateVariants(base) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MuseColors.Void.copy(alpha = 0.985f))
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetaText("GENERATE — PICK A VERSION", color = MuseColors.White, size = 10.sp, letterSpacing = 2.5.sp)
            Pressable(onClick = onClose) {
                MetaText("CLOSE", color = MuseColors.Gray2, size = 10.sp, letterSpacing = 2.5.sp)
            }
        }

        val columns = when (LocalWindowWidthClass.current) {
            WindowWidthSizeClass.Expanded -> GridCells.Fixed(4)
            WindowWidthSizeClass.Medium -> GridCells.Fixed(3)
            else -> GridCells.Fixed(2)
        }
        LazyVerticalGrid(
            columns = columns,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp
            )
        ) {
            items(count = variants.size, key = { variants[it].templateId }) { index ->
                val variant = variants[index]
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cellPx = constraints.maxWidth
                    val scale = if (cellPx <= 0) 1f else cellPx.toFloat() / with(
                        androidx.compose.ui.platform.LocalDensity.current
                    ) { BASE_CARD_DP.dp.toPx() }
                    Pressable(onClick = { onPick(variant) }, modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ScaledPostcard(
                                view = buildView(variant, numberLabel),
                                scale = scale,
                                modifier = Modifier.border(1.dp, MuseColors.HairDark)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = variant.templateId.uppercase(),
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    letterSpacing = 2.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MuseColors.Gray3
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
