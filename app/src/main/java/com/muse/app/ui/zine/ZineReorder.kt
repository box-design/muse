package com.muse.app.ui.zine

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

class ZineReorderState(
    val listState: LazyGridState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingIndex by mutableStateOf(-1)
        private set
    var dragOffset by mutableStateOf(Offset.Zero)
        private set

    val isDragging: Boolean get() = draggingIndex >= 0

    fun gridModifier(): Modifier = Modifier.pointerInput(listState) {
        detectDragGesturesAfterLongPress(
            onDragStart = { pos ->
                locate(pos)?.let {
                    draggingIndex = it.index
                    dragOffset = Offset.Zero
                }
            },
            onDrag = { change, amount ->
                change.consume()
                val dragged = draggingIndex
                if (dragged < 0) return@detectDragGesturesAfterLongPress
                dragOffset += amount
                val infos = listState.layoutInfo.visibleItemsInfo
                val from = infos.firstOrNull { it.index == dragged }
                    ?: return@detectDragGesturesAfterLongPress
                val center = Offset(
                    from.offset.x + from.size.width / 2f + dragOffset.x,
                    from.offset.y + from.size.height / 2f + dragOffset.y
                )
                val target = infos.firstOrNull { info ->
                    info.index != dragged &&
                        center.x >= info.offset.x &&
                        center.x <= (info.offset.x + info.size.width) &&
                        center.y >= info.offset.y &&
                        center.y <= (info.offset.y + info.size.height)
                }?.index ?: return@detectDragGesturesAfterLongPress
                val to = infos.first { it.index == target }
                val shift = Offset(
                    (to.offset.x - from.offset.x).toFloat(),
                    (to.offset.y - from.offset.y).toFloat()
                )
                onMove(dragged, target)
                draggingIndex = target
                dragOffset -= shift
            },
            onDragEnd = { reset() },
            onDragCancel = { reset() }
        )
    }

    fun itemModifier(index: Int): Modifier =
        if (index == draggingIndex) {
            Modifier
                .zIndex(2f)
                .graphicsLayer {
                    scaleX = 1.05f
                    scaleY = 1.05f
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                }
        } else {
            Modifier
        }

    private fun reset() {
        draggingIndex = -1
        dragOffset = Offset.Zero
    }

    private fun locate(pos: Offset) = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
        pos.x >= info.offset.x &&
            pos.x <= (info.offset.x + info.size.width) &&
            pos.y >= info.offset.y &&
            pos.y <= (info.offset.y + info.size.height)
    }
}

@Composable
fun rememberZineReorder(onMove: (fromIndex: Int, toIndex: Int) -> Unit): ZineReorderState {
    val listState = rememberLazyGridState()
    val latest = rememberUpdatedState(onMove)
    return remember(listState) {
        ZineReorderState(listState) { from, to -> latest.value(from, to) }
    }
}
