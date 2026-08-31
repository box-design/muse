package com.muse.app.data

import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.PostcardRecord
import com.muse.app.data.model.ZineRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreateRepository(private val store: MuseStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _postcards = MutableStateFlow(store.data.postcards.sortedByDescending { it.updatedAt })
    val postcards: StateFlow<List<PostcardRecord>> = _postcards.asStateFlow()

    private val _palettes = MutableStateFlow(store.data.palettes.sortedByDescending { it.createdAt })
    val palettes: StateFlow<List<PaletteRecord>> = _palettes.asStateFlow()

    private val _zines = MutableStateFlow(store.data.zines.sortedByDescending { it.updatedAt })
    val zines: StateFlow<List<ZineRecord>> = _zines.asStateFlow()

    val pendingScheme = MutableStateFlow<List<Int>?>(null)
    val pendingZineSelection = MutableStateFlow<List<Long>?>(null)

    private fun publish(d: StoreData) {
        _postcards.value = d.postcards.sortedByDescending { it.updatedAt }
        _palettes.value = d.palettes.sortedByDescending { it.createdAt }
        _zines.value = d.zines.sortedByDescending { it.updatedAt }
    }

    suspend fun upsertPostcard(record: PostcardRecord): PostcardRecord {
        val now = System.currentTimeMillis()
        var saved = record
        val d = store.mutate { cur ->
            val existing = cur.postcards.find { it.id == record.id }
            val merged = if (existing == null) {
                record.copy(id = cur.nextId, createdAt = now, updatedAt = now)
            } else {
                record.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = now)
            }
            saved = merged
            cur.copy(
                postcards = cur.postcards.filterNot { it.id == merged.id } + merged,
                nextId = if (existing == null) cur.nextId + 1 else cur.nextId
            )
        }
        publish(d)
        return saved
    }

    suspend fun savePalette(record: PaletteRecord): PaletteRecord {
        val now = System.currentTimeMillis()
        var saved = record
        val d = store.mutate { cur ->
            val existing = cur.palettes.find { it.id == record.id }
            val merged = if (existing == null) {
                record.copy(id = cur.nextId, createdAt = now)
            } else {
                record.copy(id = existing.id, createdAt = existing.createdAt)
            }
            saved = merged
            cur.copy(
                palettes = cur.palettes.filterNot { it.id == merged.id } + merged,
                nextId = if (existing == null) cur.nextId + 1 else cur.nextId
            )
        }
        publish(d)
        return saved
    }

    suspend fun deletePostcard(id: Long) {
        val d = store.mutate { cur -> cur.copy(postcards = cur.postcards.filterNot { it.id == id }) }
        publish(d)
    }

    suspend fun deletePalette(id: Long) {
        val d = store.mutate { cur -> cur.copy(palettes = cur.palettes.filterNot { it.id == id }) }
        publish(d)
    }

    suspend fun upsertZine(record: ZineRecord): ZineRecord {
        val now = System.currentTimeMillis()
        var saved = record
        val d = store.mutate { cur ->
            val existing = cur.zines.find { it.id == record.id }
            val merged = if (existing == null) {
                record.copy(id = cur.nextId, createdAt = now, updatedAt = now)
            } else {
                record.copy(id = existing.id, createdAt = existing.createdAt, updatedAt = now)
            }
            saved = merged
            cur.copy(
                zines = cur.zines.filterNot { it.id == merged.id } + merged,
                nextId = if (existing == null) cur.nextId + 1 else cur.nextId
            )
        }
        publish(d)
        return saved
    }

    suspend fun deleteZine(id: Long) {
        val d = store.mutate { cur -> cur.copy(zines = cur.zines.filterNot { it.id == id }) }
        publish(d)
    }
}
