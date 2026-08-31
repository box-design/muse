package com.muse.app.ui.palette

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.app.data.ArchiveRepository
import com.muse.app.data.CreateRepository
import com.muse.app.data.MediaRepository
import com.muse.app.data.model.PaletteRecord
import com.muse.app.util.Colors
import com.muse.app.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PaletteViewModel(
    private val media: MediaRepository,
    private val creations: CreateRepository,
    private val archiveRepository: ArchiveRepository,
    photoId: Long?,
    paletteId: Long?,
) : ViewModel() {

    var recordId: Long = 0L
        private set
    private var sourceMediaId: Long = 0L

    val photoUri = MutableStateFlow<String?>(null)
    val swatches = MutableStateFlow<List<Int>>(emptyList())
    val name = MutableStateFlow("")
    val editingIndex = MutableStateFlow<Int?>(null)
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            if (paletteId != null && paletteId > 0) {
                loadExisting(paletteId)
            } else if (photoId != null && photoId > 0) {
                extract(photoId)
            }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val rec = creations.palettes.value.find { it.id == id } ?: return
        recordId = rec.id
        sourceMediaId = rec.mediaId
        swatches.value = rec.colors
        name.value = rec.name
        photoUri.value = media.uriFor(rec.mediaId).toString()
    }

    private suspend fun extract(pid: Long) {
        loading.value = true
        val photo = runCatching { media.findById(pid) }.getOrNull()
        if (photo == null) {
            loading.value = false
            return
        }
        sourceMediaId = photo.id
        photoUri.value = photo.uri
        val colors = runCatching { media.extractColors(Uri.parse(photo.uri)) }.getOrDefault(emptyList())
        swatches.value = colors
        editingIndex.value = null
        loading.value = false
    }

    fun setName(v: String) {
        name.value = v.uppercase().take(24)
    }

    fun selectEdit(index: Int) {
        editingIndex.value = if (editingIndex.value == index) null else index
    }

    fun hsvOf(index: Int): FloatArray {
        val color = swatches.value.getOrNull(index) ?: return floatArrayOf(0f, 0f, 1f)
        return Colors.hsvOf(color)
    }

    fun adjust(index: Int, h: Float, s: Float, v: Float) {
        swatches.value = swatches.value.mapIndexed { i, c ->
            if (i == index) Colors.fromHsv(h * 360f, s, v) else c
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private suspend fun persist(): PaletteRecord? {
        if (sourceMediaId <= 0L || swatches.value.isEmpty()) return null
        return creations.savePalette(
            PaletteRecord(
                id = recordId,
                name = name.value.trim().ifBlank { "UNTITLED PALETTE" },
                mediaId = sourceMediaId,
                colors = swatches.value,
                createdAt = System.currentTimeMillis()
            )
        ).also { recordId = it.id }
    }

    fun save() {
        viewModelScope.launch {
            persist() ?: return@launch
            message.value = "PALETTE SAVED"
        }
    }

    fun collect() {
        viewModelScope.launch {
            val saved = persist() ?: return@launch
            val seq = archiveRepository.collectPalette(saved)
            message.value = "ARCHIVED · ${Fmt.no(seq)}"
        }
    }

    fun applyToPostcard(): Long? {
        if (swatches.value.isEmpty()) return null
        viewModelScope.launch { persist() }
        creations.pendingScheme.value = swatches.value
        return sourceMediaId.takeIf { it > 0 }
    }
}
