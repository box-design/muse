package com.muse.app.data.model

data class Photo(
    val id: Long,
    val dateMs: Long,
    val width: Int,
    val height: Int,
    val bucket: String?,
    val lat: Double? = null,
    val lon: Double? = null
) {
    val uri: String get() = "content://media/external/images/media/$id"
    val aspect: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 0.75f
}

data class Exhibition(
    val key: String,
    val name: String,
    val place: String?,
    val startMs: Long,
    val endMs: Long,
    val cover: Photo,
    val photos: List<Photo>
)

data class DayGroup(
    val key: Int,
    val dateMs: Long,
    val photos: List<Photo>
)

object ArchiveTypes {
    const val PHOTO = "PHOTO"
    const val POSTCARD = "POSTCARD"
    const val PALETTE = "PALETTE"
    const val ZINE = "ZINE"
}

data class ArchiveEntry(
    val id: Long,
    val type: String,
    val mediaId: Long? = null,
    val postcardId: Long? = null,
    val paletteId: Long? = null,
    val zineId: Long? = null,
    val number: Int,
    val title: String,
    val subtitle: String,
    val createdAt: Long
)

enum class ZinePaper { MATTE, GLOSS, NEWSPRINT }

enum class ZineCoverStyle { FULL_BLEED, FRAME, EDITORIAL }

enum class ZinePageLayout { GALLERY, MUSEUM, JOURNAL }

data class ZinePage(
    val mediaId: Long,
    val layout: ZinePageLayout? = null
)

data class ZineRecord(
    val id: Long,
    val title: String,
    val coverMediaId: Long,
    val pages: List<ZinePage>,
    val paper: ZinePaper = ZinePaper.MATTE,
    val coverStyle: ZineCoverStyle = ZineCoverStyle.FULL_BLEED,
    val defaultLayout: ZinePageLayout = ZinePageLayout.GALLERY,
    val createdAt: Long,
    val updatedAt: Long
) {
    val pageCount: Int get() = pages.size
}

enum class CropMode { FIT, FILL, CROP }

data class PostcardRecord(
    val id: Long,
    val mediaId: Long,
    val photoAspect: Float,
    val templateId: String,
    val cropMode: CropMode,
    val offsetX: Float,
    val offsetY: Float,
    val title: String,
    val subtitle: String,
    val dateLabel: String,
    val placeLabel: String,
    val showDate: Boolean,
    val showLocation: Boolean,
    val showNumber: Boolean,
    val numberLabel: String,
    val paperArgb: Int,
    val inkArgb: Int,
    val paletteId: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

data class PaletteRecord(
    val id: Long,
    val name: String,
    val mediaId: Long,
    val colors: List<Int>,
    val createdAt: Long
)

data class ColorSwatch(
    val argb: Int,
    val population: Int
)

data class CollectOutcome(
    val collected: Boolean,
    val archiveNumber: Int?
)

sealed interface ExploreUi {
    data object Loading : ExploreUi
    data object Empty : ExploreUi
    data class Ready(val days: List<DayGroup>) : ExploreUi
}
