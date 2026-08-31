package com.muse.app.data

import com.muse.app.data.model.ArchiveEntry
import com.muse.app.data.model.ArchiveTypes
import com.muse.app.data.model.CollectOutcome
import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.Photo
import com.muse.app.data.model.PostcardRecord
import com.muse.app.data.model.ZineRecord
import com.muse.app.util.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArchiveRepository(private val store: MuseStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _archives = MutableStateFlow(store.data.archives.sortedByDescending { it.number })
    val archives: StateFlow<List<ArchiveEntry>> = _archives.asStateFlow()

    private val _collectedPhotoIds = MutableStateFlow(
        store.data.archives
            .filter { it.type == ArchiveTypes.PHOTO }
            .mapNotNull { it.mediaId }
            .toSet()
    )
    val collectedPhotoIds: StateFlow<Set<Long>> = _collectedPhotoIds.asStateFlow()

    private fun publish(d: StoreData) {
        _archives.value = d.archives.sortedByDescending { it.number }
        _collectedPhotoIds.value = d.archives
            .filter { it.type == ArchiveTypes.PHOTO }
            .mapNotNull { it.mediaId }
            .toSet()
    }

    fun nextNumber(): Int = (_archives.value.maxOfOrNull { it.number } ?: 0) + 1

    suspend fun togglePhoto(photo: Photo, contextTitle: String?): CollectOutcome {
        val d = store.mutate { cur ->
            val existing = cur.archives.find {
                it.type == ArchiveTypes.PHOTO && it.mediaId == photo.id
            }
            if (existing != null) {
                cur.copy(archives = cur.archives - existing)
            } else {
                val n = cur.archiveSeq + 1
                val entry = ArchiveEntry(
                    id = cur.nextId,
                    type = ArchiveTypes.PHOTO,
                    mediaId = photo.id,
                    number = n,
                    title = (contextTitle ?: "").uppercase(),
                    subtitle = Fmt.photoDate(photo.dateMs),
                    createdAt = System.currentTimeMillis()
                )
                cur.copy(
                    archiveSeq = n,
                    nextId = cur.nextId + 1,
                    archives = cur.archives + entry
                )
            }
        }
        publish(d)
        val kept = d.archives.find { it.type == ArchiveTypes.PHOTO && it.mediaId == photo.id }
        return CollectOutcome(collected = kept != null, archiveNumber = kept?.number)
    }

    suspend fun collectPostcard(record: PostcardRecord): Int {
        val d = store.mutate { cur ->
            val existing = cur.archives.find {
                it.type == ArchiveTypes.POSTCARD && it.postcardId == record.id
            }
            if (existing != null) cur else {
                val n = cur.archiveSeq + 1
                cur.copy(
                    archiveSeq = n,
                    nextId = cur.nextId + 1,
                    archives = cur.archives + ArchiveEntry(
                        id = cur.nextId,
                        type = ArchiveTypes.POSTCARD,
                        postcardId = record.id,
                        number = n,
                        title = record.title.ifBlank { "UNTITLED" },
                        subtitle = record.dateLabel.ifBlank { Fmt.photoDate(record.updatedAt) },
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        publish(d)
        return d.archiveSeq
    }

    suspend fun collectPalette(record: PaletteRecord): Int {
        val d = store.mutate { cur ->
            val existing = cur.archives.find {
                it.type == ArchiveTypes.PALETTE && it.paletteId == record.id
            }
            if (existing != null) cur else {
                val n = cur.archiveSeq + 1
                cur.copy(
                    archiveSeq = n,
                    nextId = cur.nextId + 1,
                    archives = cur.archives + ArchiveEntry(
                        id = cur.nextId,
                        type = ArchiveTypes.PALETTE,
                        paletteId = record.id,
                        number = n,
                        title = record.name.ifBlank { "UNTITLED PALETTE" },
                        subtitle = Fmt.photoDate(record.createdAt),
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        publish(d)
        return d.archiveSeq
    }

    suspend fun collectZine(record: ZineRecord): Int {
        val d = store.mutate { cur ->
            val existing = cur.archives.find {
                it.type == ArchiveTypes.ZINE && it.zineId == record.id
            }
            if (existing != null) cur else {
                val n = cur.archiveSeq + 1
                cur.copy(
                    archiveSeq = n,
                    nextId = cur.nextId + 1,
                    archives = cur.archives + ArchiveEntry(
                        id = cur.nextId,
                        type = ArchiveTypes.ZINE,
                        zineId = record.id,
                        number = n,
                        title = record.title.ifBlank { "UNTITLED" },
                        subtitle = "${record.pageCount} PLATES · ${Fmt.photoDate(record.createdAt)}",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        publish(d)
        return d.archiveSeq
    }

    suspend fun remove(entryId: Long) {
        val d = store.mutate { cur -> cur.copy(archives = cur.archives.filterNot { it.id == entryId }) }
        publish(d)
    }
}
