package com.muse.app.data

import android.content.ContentUris
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.muse.app.data.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class MediaRepository(private val resolver: ContentResolver) {

    fun uriFor(id: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

    suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        runCatching { query(null) }.getOrDefault(emptyList())
    }

    suspend fun findById(id: Long): Photo? = withContext(Dispatchers.IO) {
        runCatching {
            query(id).firstOrNull()
        }.getOrNull()
    }

    private fun query(singleId: Long?): List<Photo> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )
        val selection = if (singleId != null) "${MediaStore.Images.Media._ID} = ?" else null
        val args = if (singleId != null) arrayOf(singleId.toString()) else null
        val sort = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
        val out = mutableListOf<Photo>()
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, args, sort
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val taken = if (cursor.isNull(takenCol)) 0L else cursor.getLong(takenCol)
                val added = cursor.getLong(addedCol)
                val dateMs = if (taken > 0) taken else added * 1000L
                val width = if (cursor.isNull(wCol)) 0 else cursor.getInt(wCol)
                val height = if (cursor.isNull(hCol)) 0 else cursor.getInt(hCol)
                val bucket = if (cursor.isNull(bucketCol)) null else cursor.getString(bucketCol)
                out += Photo(
                    id = id,
                    dateMs = dateMs,
                    width = width,
                    height = height,
                    bucket = bucket
                )
            }
        }
        return out
    }

    suspend fun readLatLon(uri: Uri): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.latLong?.let { ll ->
                    if (ll.size >= 2 && (ll[0] != 0.0 || ll[1] != 0.0)) ll[0] to ll[1] else null
                }
            }
        }.getOrNull()
    }

    suspend fun extractColors(uri: Uri): List<Int> {
        val swatches = PaletteExtractor.extract(resolver, uri)
        return swatches.map { it.argb }
    }
}
