package com.muse.app.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.PostcardRecord
import com.muse.app.ui.adaptive.isAtLeastMediumWidth
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.Hairline
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors

@Composable
fun CreateHomeScreen(
    application: MuseApplication,
    onPickPostcard: () -> Unit,
    onPickPalette: () -> Unit,
    onOpenPostcard: (Long) -> Unit,
    onOpenPalette: (Long) -> Unit,
    onCreateZine: () -> Unit = {},
    onOpenZine: (Long) -> Unit = {},
) {
    val vm: CreateHomeViewModel = viewModel {
        CreateHomeViewModel(application.container.creations)
    }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val creationCount = ui.postcards.size + ui.palettes.size + ui.zines.size

    if (isAtLeastMediumWidth()) {
        Row(
            Modifier
                .fillMaxSize()
                .background(MuseColors.Paper)
                .statusBarsPadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CREATE",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MuseColors.Ink
                        )
                    )
                    MetaText("MUSE", color = MuseColors.Gray2.copy(alpha = 0.7f), letterSpacing = 4.sp)
                }

                ToolEntry(
                    title = "POSTCARD",
                    subtitle = "明信片",
                    caption = "DESIGN A PHOTOGRAPH INTO A WORK",
                    iconRes = R.drawable.ic_vector_postcard,
                    onClick = onPickPostcard
                )
                Spacer(Modifier.height(14.dp))
                ToolEntry(
                    title = "PALETTE",
                    subtitle = "调色盘",
                    caption = "EXTRACT COLOR FROM A PHOTOGRAPH",
                    iconRes = R.drawable.ic_vector_palette,
                    onClick = onPickPalette
                )
                Spacer(Modifier.height(14.dp))
                ToolEntry(
                    title = "ZINE",
                    subtitle = "影集",
                    caption = "BIND 8–20 PHOTOGRAPHS INTO A BOOK",
                    iconRes = R.drawable.ic_vector_zine,
                    onClick = onCreateZine
                )

                Spacer(Modifier.height(92.dp))
            }

            Box(Modifier.width(1.dp).fillMaxHeight().background(MuseColors.HairLight))

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .navigationBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY CREATIONS",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MuseColors.Ink
                        )
                    )
                    MetaText("$creationCount", color = MuseColors.Gray2, size = 10.sp)
                }

                if (creationCount == 0) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MetaText(
                            "NOTHING CREATED YET — CHOOSE A TOOL ABOVE",
                            color = MuseColors.Gray2,
                            modifier = Modifier.padding(horizontal = 28.dp)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(148.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 28.dp, end = 28.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(ui.postcards, key = { "pc${it.id}" }) { record ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Pressable(onClick = { onOpenPostcard(record.id) }) {
                                    PostcardThumb(record)
                                }
                            }
                        }
                        items(ui.palettes, key = { "pl${it.id}" }) { record ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Pressable(onClick = { onOpenPalette(record.id) }) {
                                    PaletteThumb(record)
                                }
                            }
                        }
                        items(ui.zines, key = { "zn${it.id}" }) { record ->
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Pressable(onClick = { onOpenZine(record.id) }) {
                                    ZineThumb(record)
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(MuseColors.Paper)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CREATE",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MuseColors.Ink
                    )
                )
                MetaText("MUSE", color = MuseColors.Gray2.copy(alpha = 0.7f), letterSpacing = 4.sp)
            }

            ToolEntry(
                title = "POSTCARD",
                subtitle = "明信片",
                caption = "DESIGN A PHOTOGRAPH INTO A WORK",
                iconRes = R.drawable.ic_vector_postcard,
                onClick = onPickPostcard
            )
            Spacer(Modifier.height(14.dp))
            ToolEntry(
                title = "PALETTE",
                subtitle = "调色盘",
                caption = "EXTRACT COLOR FROM A PHOTOGRAPH",
                iconRes = R.drawable.ic_vector_palette,
                onClick = onPickPalette
            )
            Spacer(Modifier.height(14.dp))
            ToolEntry(
                title = "ZINE",
                subtitle = "影集",
                caption = "BIND 8–20 PHOTOGRAPHS INTO A BOOK",
                iconRes = R.drawable.ic_vector_zine,
                onClick = onCreateZine
            )

            Spacer(Modifier.height(34.dp))
            Hairline(MuseColors.HairLight)
            Spacer(Modifier.height(20.dp))

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MY CREATIONS",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MuseColors.Ink
                    )
                )
                MetaText("$creationCount", color = MuseColors.Gray2, size = 10.sp)
            }

            Spacer(Modifier.height(16.dp))

            if (creationCount == 0) {
                MetaText(
                    "NOTHING CREATED YET — CHOOSE A TOOL ABOVE",
                    color = MuseColors.Gray2,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(ui.postcards, key = { "pc${it.id}" }) { record ->
                        Pressable(onClick = { onOpenPostcard(record.id) }) {
                            PostcardThumb(record)
                        }
                    }
                    items(ui.palettes, key = { "pl${it.id}" }) { record ->
                        Pressable(onClick = { onOpenPalette(record.id) }) {
                            PaletteThumb(record)
                        }
                    }
                    items(ui.zines, key = { "zn${it.id}" }) { record ->
                        Pressable(onClick = { onOpenZine(record.id) }) {
                            ZineThumb(record)
                        }
                    }
                }
            }

            Spacer(Modifier.height(92.dp))
        }
    }
}

@Composable
private fun ToolEntry(
    title: String,
    subtitle: String,
    caption: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Pressable(onClick = onClick, modifier = Modifier.padding(horizontal = 28.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MuseColors.Ink.copy(alpha = 0.28f))
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.W500,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.5.sp,
                            color = MuseColors.Ink
                        )
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = subtitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MuseColors.Gray2
                        )
                    )
                    Spacer(Modifier.height(14.dp))
                    MetaText(caption, color = MuseColors.Gray2.copy(alpha = 0.75f), size = 9.sp)
                }
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MuseColors.Ink.copy(alpha = 0.85f),
                    modifier = Modifier.width(36.dp).height(36.dp)
                )
            }
        }
    }
}

@Composable
private fun PostcardThumb(record: PostcardRecord) {
    Column(Modifier.width(124.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ScaledPostcard(
            view = draftViewOf(record),
            scale = 124f / BASE_CARD_DP,
            modifier = Modifier.border(1.dp, MuseColors.Ink.copy(alpha = 0.12f))
        )
        Spacer(Modifier.height(8.dp))
        MetaText(
            record.title.ifBlank { "UNTITLED" },
            color = MuseColors.Gray2,
            size = 8.5.sp,
            letterSpacing = 1.5.sp,
            align = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun PaletteThumb(record: PaletteRecord) {
    Column(Modifier.width(124.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, MuseColors.Ink.copy(alpha = 0.12f))
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
        Spacer(Modifier.height(8.dp))
        MetaText(
            record.name.ifBlank { "UNTITLED PALETTE" },
            color = MuseColors.Gray2,
            size = 8.5.sp,
            letterSpacing = 1.5.sp,
            align = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ZineThumb(record: com.muse.app.data.model.ZineRecord) {
    Column(Modifier.width(124.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        com.muse.app.ui.zine.ScaledZinePage(
            scale = 124f / com.muse.app.ui.zine.BASE_ZINE_DP,
            modifier = Modifier.border(1.dp, MuseColors.Ink.copy(alpha = 0.12f))
        ) {
            com.muse.app.ui.zine.ZineSheet(paper = record.paper) {
                com.muse.app.ui.zine.ZineCoverContent(record, emptyMap())
            }
        }
        Spacer(Modifier.height(8.dp))
        MetaText(
            record.title.ifBlank { "UNTITLED" },
            color = MuseColors.Gray2,
            size = 8.5.sp,
            letterSpacing = 1.5.sp,
            align = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
