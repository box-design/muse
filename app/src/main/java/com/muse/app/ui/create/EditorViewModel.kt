package com.muse.app.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muse.app.data.ArchiveRepository
import com.muse.app.data.CreateRepository
import com.muse.app.data.MediaRepository
import com.muse.app.data.model.CropMode
import com.muse.app.data.model.PostcardRecord
import com.muse.app.util.Colors
import com.muse.app.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Draft(
    val mediaId: Long = 0L,
    val photoAspect: Float = 0.75f,
    val templateId: String = "classic",
    val cropMode: CropMode = CropMode.FILL,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val title: String = "",
    val dateLabel: String = "",
    val placeLabel: String = "",
    val showDate: Boolean = true,
    val showLocation: Boolean = true,
    val showNumber: Boolean = false,
    val paperArgb: Int = 0xFFF6F3EC.toInt(),
    val inkArgb: Int = 0xFF161512.toInt(),
    val paletteId: Long? = null,
)

fun buildView(draft: Draft, numberLabel: String): DraftView = DraftView(
    uri = "content://media/external/images/media/${draft.mediaId}",
    photoAspect = draft.photoAspect,
    templateId = draft.templateId,
    cropMode = draft.cropMode,
    offsetX = draft.offsetX,
    offsetY = draft.offsetY,
    title = draft.title,
    dateLabel = draft.dateLabel,
    placeLabel = draft.placeLabel,
    numberLabel = numberLabel,
    showDate = draft.showDate,
    showLocation = draft.showLocation,
    showNumber = draft.showNumber,
    paperArgb = draft.paperArgb,
    inkArgb = draft.inkArgb
)

fun generateVariants(base: Draft): List<Draft> {
    val rnd = Random(base.mediaId.hashCode())
    val others = Templates.all.filter { it.id != base.templateId }
    val picked = others.shuffled(rnd).take(5)
    val crops = listOf(CropMode.FILL, CropMode.CROP, CropMode.FIT)
    return picked.mapIndexed { i, t ->
        val crop = crops[(i + rnd.nextInt(3)) % 3]
        base.copy(
            templateId = t.id,
            cropMode = crop,
            offsetX = if (crop == CropMode.CROP) rnd.nextFloat() * 1.2f - 0.6f else 0f,
            offsetY = if (crop == CropMode.CROP) rnd.nextFloat() * 1.2f - 0.6f else 0f
        )
    } + listOf(base)
}

class EditorViewModel(
    private val media: MediaRepository,
    private val creations: CreateRepository,
    private val archiveRepository: ArchiveRepository,
    photoId: Long?,
    place: String?,
    postcardId: Long?,
) : ViewModel() {

    val draft = MutableStateFlow(Draft())
    var recordId: Long = 0L
        private set

    val nextNumber = MutableStateFlow(1)
    val message = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            if (postcardId != null) {
                loadExisting(postcardId)
            } else if (photoId != null && photoId > 0) {
                startNew(photoId, place)
            }
        }
        viewModelScope.launch {
            archiveRepository.archives.collect { list ->
                nextNumber.value = (list.maxOfOrNull { it.number } ?: 0) + 1
            }
        }
    }

    private suspend fun loadExisting(id: Long) {
        val rec = creations.postcards.value.find { it.id == id } ?: return
        recordId = rec.id
        draft.value = Draft(
            mediaId = rec.mediaId,
            photoAspect = rec.photoAspect,
            templateId = rec.templateId,
            cropMode = rec.cropMode,
            offsetX = rec.offsetX,
            offsetY = rec.offsetY,
            title = rec.title,
            dateLabel = rec.dateLabel,
            placeLabel = rec.placeLabel,
            showDate = rec.showDate,
            showLocation = rec.showLocation,
            showNumber = rec.showNumber,
            paperArgb = rec.paperArgb,
            inkArgb = rec.inkArgb,
            paletteId = rec.paletteId
        )
    }

    private suspend fun startNew(photoId: Long, place: String?) {
        val photo = runCatching { media.findById(photoId) }.getOrNull()
        var d = Draft(
            mediaId = photoId,
            photoAspect = photo?.aspect ?: 0.75f,
            dateLabel = photo?.let { Fmt.photoDate(it.dateMs) }.orEmpty(),
            placeLabel = place.orEmpty()
        )
        val scheme = creations.pendingScheme.value
        if (scheme != null && scheme.size >= 2) {
            d = d.copy(
                paperArgb = scheme.first(),
                inkArgb = Colors.readableInk(scheme.first()),
                paletteId = null
            )
            creations.pendingScheme.value = null
        }
        draft.value = d
    }

    fun edit(transform: (Draft) -> Draft) {
        val next = transform(draft.value)
        draft.value = next.copy(
            offsetX = next.offsetX.coerceIn(-1f, 1f),
            offsetY = next.offsetY.coerceIn(-1f, 1f)
        )
    }

    fun applyPalette(record: com.muse.app.data.model.PaletteRecord) {
        if (record.colors.isEmpty()) return
        edit {
            it.copy(
                paperArgb = record.colors.first(),
                inkArgb = Colors.readableInk(record.colors.first()),
                paletteId = record.id
            )
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun toRecord(d: Draft, now: Long): PostcardRecord = PostcardRecord(
        id = recordId,
        mediaId = d.mediaId,
        photoAspect = d.photoAspect,
        templateId = d.templateId,
        cropMode = d.cropMode,
        offsetX = d.offsetX,
        offsetY = d.offsetY,
        title = d.title.trim().uppercase().ifBlank { "UNTITLED" },
        subtitle = "",
        dateLabel = d.dateLabel,
        placeLabel = d.placeLabel.trim(),
        showDate = d.showDate,
        showLocation = d.showLocation,
        showNumber = d.showNumber,
        numberLabel = Fmt.archiveNo(nextNumber.value),
        paperArgb = d.paperArgb,
        inkArgb = d.inkArgb,
        paletteId = d.paletteId,
        createdAt = now,
        updatedAt = now
    )

    private suspend fun persist(): PostcardRecord? {
        val d = draft.value
        if (d.mediaId <= 0L) return null
        val saved = creations.upsertPostcard(toRecord(d, System.currentTimeMillis()))
        recordId = saved.id
        return saved
    }

    fun save() {
        viewModelScope.launch {
            val saved = persist() ?: return@launch
            message.value = "SAVED TO CREATIONS"
            touchArchiveIfCollected(saved)
        }
    }

    fun collect() {
        viewModelScope.launch {
            val saved = persist() ?: return@launch
            val seq = archiveRepository.collectPostcard(saved)
            message.value = "ARCHIVED · ${Fmt.no(seq)}"
        }
    }

    private suspend fun touchArchiveIfCollected(saved: PostcardRecord) {
        val existing = archiveRepository.archives.value.find {
            it.type == com.muse.app.data.model.ArchiveTypes.POSTCARD && it.postcardId == saved.id
        }
        if (existing != null) {
            archiveRepository.collectPostcard(saved)
        }
    }
}
