package com.muse.app.ui.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.muse.app.data.model.CollectOutcome
import com.muse.app.data.model.Photo

data class ViewerRequest(
    val photos: List<Photo>,
    val startIndex: Int,
    val origin: Rect?,
    val contextTitle: String?,
)

@Stable
class PhotoViewerState {
    var request by mutableStateOf<ViewerRequest?>(null)
        private set

    fun open(r: ViewerRequest) {
        request = r
    }

    fun dismissImmediate() {
        request = null
    }
}

@Composable
fun PhotoViewerOverlay(
    state: PhotoViewerState,
    collectedIds: Set<Long>,
    onToggleCollect: suspend (Photo, String?) -> CollectOutcome,
    onCreatePostcard: (Photo) -> Unit,
    onExtractPalette: (Photo) -> Unit,
) {
    val req = state.request ?: return
    key(req) {
        ViewerContent(
            req = req,
            collectedIds = collectedIds,
            onToggleCollect = onToggleCollect,
            onCreatePostcard = onCreatePostcard,
            onExtractPalette = onExtractPalette,
            onClose = { state.dismissImmediate() }
        )
    }
}
