package com.muse.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

@Composable
fun PhotoImage(
    uri: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnail: Boolean = true,
    crossfade: Int = 220,
    colorFilter: ColorFilter? = null,
) {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(uri)
        .apply { if (thumbnail) size(720) }
        .crossfade(crossfade)
        .build()
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier
    )
}
