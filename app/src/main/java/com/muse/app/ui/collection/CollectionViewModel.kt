package com.muse.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.app.data.ArchiveRepository
import com.muse.app.data.CreateRepository
import com.muse.app.data.model.ArchiveEntry
import com.muse.app.data.model.ArchiveTypes
import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.PostcardRecord
import com.muse.app.data.model.ZineRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ArchiveFilter(val label: String) {
    ALL("ALL"),
    PHOTOS("PHOTOS"),
    POSTCARDS("POSTCARDS"),
    PALETTES("PALETTES"),
    ZINES("ZINES");

    fun matches(type: String): Boolean = when (this) {
        ALL -> type != ArchiveTypes.ZINE
        PHOTOS -> type == ArchiveTypes.PHOTO
        POSTCARDS -> type == ArchiveTypes.POSTCARD
        PALETTES -> type == ArchiveTypes.PALETTE
        ZINES -> type == ArchiveTypes.ZINE
    }
}

sealed interface ArtifactContent {
    data class PhotoArt(val mediaId: Long) : ArtifactContent
    data class PostcardArt(val record: PostcardRecord) : ArtifactContent
    data class PaletteArt(val record: PaletteRecord) : ArtifactContent
    data class ZineArt(val record: ZineRecord) : ArtifactContent
}

data class ArtifactUi(
    val entry: ArchiveEntry,
    val content: ArtifactContent?,
)

class CollectionViewModel(
    private val archiveRepository: ArchiveRepository,
    creations: CreateRepository,
) : ViewModel() {

    val filter = MutableStateFlow(ArchiveFilter.ALL)

    val items: StateFlow<List<ArtifactUi>> = combine(
        archiveRepository.archives,
        creations.postcards,
        creations.palettes,
        creations.zines,
        filter
    ) { archives, postcards, palettes, zines, f ->
        archives
            .sortedByDescending { it.number }
            .filter { f.matches(it.type) }
            .map { entry ->
                val content = when (entry.type) {
                    ArchiveTypes.PHOTO -> entry.mediaId?.let { ArtifactContent.PhotoArt(it) }
                    ArchiveTypes.POSTCARD ->
                        postcards.find { it.id == entry.postcardId }
                            ?.let { ArtifactContent.PostcardArt(it) }
                    ArchiveTypes.PALETTE ->
                        palettes.find { it.id == entry.paletteId }
                            ?.let { ArtifactContent.PaletteArt(it) }
                    ArchiveTypes.ZINE ->
                        zines.find { it.id == entry.zineId }
                            ?.let { ArtifactContent.ZineArt(it) }
                    else -> null
                }
                ArtifactUi(entry, content)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(entryId: Long) {
        viewModelScope.launch {
            archiveRepository.remove(entryId)
        }
    }
}
