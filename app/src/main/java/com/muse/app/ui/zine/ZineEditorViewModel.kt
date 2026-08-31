package com.muse.app.ui.zine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.app.data.ArchiveRepository
import com.muse.app.data.CreateRepository
import com.muse.app.data.MediaRepository
import com.muse.app.data.model.ZineCoverStyle
import com.muse.app.data.model.ZinePage
import com.muse.app.data.model.ZinePageLayout
import com.muse.app.data.model.ZinePaper
import com.muse.app.data.model.ZineRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class ZineDraft(
    val title: String = "",
    val pages: List<Long> = emptyList(),
    val layouts: Map<Long, ZinePageLayout> = emptyMap(),
    val coverMediaId: Long = 0L,
    val paper: ZinePaper = ZinePaper.MATTE,
    val coverStyle: ZineCoverStyle = ZineCoverStyle.FULL_BLEED,
    val defaultLayout: ZinePageLayout = ZinePageLayout.GALLERY,
)

fun draftRecord(draft: ZineDraft): ZineRecord = ZineRecord(
    id = 0L,
    title = draft.title,
    coverMediaId = draft.coverMediaId,
    pages = draft.pages.map { ZinePage(it, draft.layouts[it]) },
    paper = draft.paper,
    coverStyle = draft.coverStyle,
    defaultLayout = draft.defaultLayout,
    createdAt = 0L,
    updatedAt = 0L
)

class ZineEditorViewModel(
    private val media: MediaRepository,
    private val creations: CreateRepository,
    private val archiveRepository: ArchiveRepository,
    private val zineId: Long?,
) : ViewModel() {

    val draft = MutableStateFlow(ZineDraft())
    val dates = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val ready = MutableStateFlow(false)

    var recordId: Long = 0L
        private set

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (zineId != null) {
            val rec = creations.zines.value.find { it.id == zineId } ?: return
            recordId = rec.id
            draft.value = ZineDraft(
                title = rec.title,
                pages = rec.pages.map { it.mediaId },
                layouts = rec.pages.mapNotNull { p -> p.layout?.let { p.mediaId to it } }.toMap(),
                coverMediaId = rec.coverMediaId,
                paper = rec.paper,
                coverStyle = rec.coverStyle,
                defaultLayout = rec.defaultLayout
            )
        } else {
            val picked = creations.pendingZineSelection.value
            creations.pendingZineSelection.value = null
            if (picked.isNullOrEmpty() || picked.size < ZINE_MIN_PAGES) return
            draft.value = ZineDraft(pages = picked, coverMediaId = picked.first())
        }
        loadDates(draft.value.pages)
        ready.value = true
    }

    private suspend fun loadDates(ids: List<Long>) {
        val map = mutableMapOf<Long, Long>()
        ids.distinct().forEach { id ->
            runCatching { media.findById(id) }.getOrNull()?.let { map[id] = it.dateMs }
        }
        dates.value = map
    }

    fun edit(transform: (ZineDraft) -> ZineDraft) {
        draft.value = transform(draft.value)
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val list = draft.value.pages.toMutableList()
        if (fromIndex == toIndex) return
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        edit { it.copy(pages = list) }
    }

    fun removePage(index: Int) {
        val list = draft.value.pages.toMutableList()
        if (index !in list.indices || list.size <= ZINE_MIN_PAGES) return
        val removed = list.removeAt(index)
        edit { d ->
            d.copy(
                pages = list,
                coverMediaId = if (d.coverMediaId == removed) (list.firstOrNull() ?: 0L) else d.coverMediaId,
                layouts = d.layouts - removed
            )
        }
    }

    fun setCover(mediaId: Long) = edit { it.copy(coverMediaId = mediaId) }

    fun setPaper(paper: ZinePaper) = edit { it.copy(paper = paper) }

    fun setCoverStyle(style: ZineCoverStyle) = edit { it.copy(coverStyle = style) }

    fun setDefaultLayout(layout: ZinePageLayout) = edit { it.copy(defaultLayout = layout) }

    fun setPageLayout(mediaId: Long, layout: ZinePageLayout?) = edit { d ->
        d.copy(
            layouts = if (layout == null) d.layouts - mediaId else d.layouts + (mediaId to layout)
        )
    }

    fun canBind(d: ZineDraft = draft.value): Boolean =
        d.pages.size in ZINE_MIN_PAGES..ZINE_MAX_PAGES && d.coverMediaId > 0L

    fun publish(onBound: (Long) -> Unit) {
        viewModelScope.launch {
            val d = draft.value
            if (!canBind(d)) return@launch
            val now = System.currentTimeMillis()
            val record = ZineRecord(
                id = recordId,
                title = d.title.trim().uppercase().ifBlank { "UNTITLED" },
                coverMediaId = d.coverMediaId,
                pages = d.pages.map { ZinePage(it, d.layouts[it]) },
                paper = d.paper,
                coverStyle = d.coverStyle,
                defaultLayout = d.defaultLayout,
                createdAt = now,
                updatedAt = now
            )
            val saved = creations.upsertZine(record)
            recordId = saved.id
            archiveRepository.collectZine(saved)
            onBound(saved.id)
        }
    }
}
