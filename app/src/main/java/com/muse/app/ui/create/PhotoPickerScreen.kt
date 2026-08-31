package com.muse.app.ui.create

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muse.app.MuseApplication
import com.muse.app.R
import com.muse.app.data.MediaRepository
import com.muse.app.data.model.Photo
import com.muse.app.ui.adaptive.LocalWindowWidthClass
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.PhotoImage
import com.muse.app.ui.components.SectionTitle
import com.muse.app.ui.components.StatusBarAppearance
import com.muse.app.ui.theme.MuseColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class PickerViewModel(media: MediaRepository) : ViewModel() {
    val photos: StateFlow<List<Photo>?> = flow<List<Photo>?> {
        emit(runCatching { media.loadPhotos() }.getOrDefault(emptyList()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun PhotoPickerScreen(
    application: MuseApplication,
    mode: String,
    onClose: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    StatusBarAppearance(lightIcons = true)
    val vm: PickerViewModel = viewModel { PickerViewModel(application.container.media) }
    val photos by vm.photos.collectAsStateWithLifecycle()

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
                SectionTitle(if (mode == "PALETTE") "SELECT — PALETTE" else "SELECT — POSTCARD")
                Spacer(Modifier.height(4.dp))
                MetaText(
                    text = (photos?.size ?: 0).toString() + " PHOTOGRAPHS",
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

        Spacer(Modifier.height(6.dp))

        when {
            photos == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MetaText("LOADING", color = MuseColors.Gray3)
            }
            photos!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(count = photos!!.size, key = { photos!![it].id }) { index ->
                        val photo = photos!![index]
                        Pressable(onClick = { onPicked(photo.id) }) {
                            PhotoImage(
                                uri = photo.uri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                thumbnail = true,
                                crossfade = 120
                            )
                        }
                    }
                }
            }
        }
    }
}
