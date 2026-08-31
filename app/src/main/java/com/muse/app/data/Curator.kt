package com.muse.app.data

import android.content.Context
import android.location.Geocoder
import com.muse.app.data.model.Exhibition
import com.muse.app.data.model.Photo
import com.muse.app.util.Fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class Curator(
    context: Context,
    private val media: MediaRepository,
    private val store: MuseStore
) {

    @Volatile
    var cache: List<Exhibition> = emptyList()
        private set

    private val appContext = context.applicationContext
    private val cityCache = ConcurrentHashMap<String, String?>()

    suspend fun curate(): List<Exhibition> = withContext(Dispatchers.Default) {
        val photos = media.loadPhotos()
        val result = if (photos.isEmpty()) emptyList() else buildExhibitions(photos)
        cache = result
        result
    }

    fun byKey(key: String): Exhibition? = cache.find { it.key == key }

    suspend fun ensureLoaded(key: String): Exhibition? {
        byKey(key)?.let { return it }
        curate()
        return byKey(key)
    }

    suspend fun rename(key: String, rawName: String) {
        val name = rawName.trim().uppercase(Locale.US).ifBlank { return }
        store.mutate { d -> d.copy(exhibitionNames = d.exhibitionNames + (key to name)) }
        cache = cache.map { if (it.key == key) it.copy(name = name) else it }
    }

    private suspend fun buildExhibitions(photos: List<Photo>): List<Exhibition> {
        val sorted = photos.sortedByDescending { it.dateMs }
        val sessions = splitOversized(sessions(sorted))
        var candidates = sessions.filter { it.size >= MIN_PHOTOS }
        if (candidates.isEmpty()) candidates = sessions.filter { it.size >= RELAXED_PHOTOS }
        val overrides = store.data.exhibitionNames
        val list = if (candidates.isNotEmpty()) {
            candidates.take(MAX_EXHIBITIONS).map { assemble(it, overrides) }
        } else {
            monthlyFallback(sorted, overrides)
        }
        return list
            .filter { it.photos.isNotEmpty() }
            .distinctBy { it.name }
            .sortedByDescending { it.startMs }
            .take(MAX_EXHIBITIONS)
    }

    private fun sessions(desc: List<Photo>): List<List<Photo>> {
        if (desc.isEmpty()) return emptyList()
        val out = mutableListOf<List<Photo>>()
        var current = mutableListOf(desc.first())
        for (i in 1 until desc.size) {
            val prev = current.last()
            val gap = prev.dateMs - desc[i].dateMs
            if (gap > GAP_MS || current.size >= HARD_CAP) {
                out += current.toList()
                current = mutableListOf(desc[i])
            } else {
                current += desc[i]
            }
        }
        out += current.toList()
        return out
    }

    private fun splitOversized(sessions: List<List<Photo>>): List<List<Photo>> {
        val out = mutableListOf<List<Photo>>()
        val queue = ArrayDeque(sessions)
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            if (s.size <= MAX_SESSION) {
                out += s
                continue
            }
            val mid = s.size / 2
            queue.addLast(s.subList(0, mid).toList())
            queue.addLast(s.subList(mid, s.size).toList())
        }
        return out
    }

    private suspend fun assemble(session: List<Photo>, overrides: Map<String, String>): Exhibition {
        val asc = session.sortedBy { it.dateMs }
        val key = "K${asc.first().id}-${asc.size}"
        var city: String? = null
        val sampled = sampleIndices(asc.size, SAMPLE_COUNT)
        val coords = sampled.mapNotNull { idx ->
            val p = asc[idx]
            val ll = media.readLatLon(media.uriFor(p.id))
            if (ll != null) p.copy(lat = ll.first, lon = ll.second) else null
        }
        if (coords.isNotEmpty()) {
            val lat = coords.map { it.lat!! }.average()
            val lon = coords.map { it.lon!! }.average()
            city = cityName(lat, lon)
        }
        val bucketName = majorityBucket(asc)
        val place = city ?: bucketName
        val fallbackName = bucketName ?: city ?: "${Fmt.monthName(asc.first().dateMs)} DAYS"
        val name = overrides[key] ?: fallbackName
        return Exhibition(
            key = key,
            name = name.uppercase(Locale.US),
            place = place?.uppercase(Locale.US),
            startMs = asc.first().dateMs,
            endMs = asc.last().dateMs,
            cover = pickCover(asc),
            photos = asc
        )
    }

    private fun majorityBucket(session: List<Photo>): String? {
        val buckets = session.mapNotNull { it.bucket?.trim()?.uppercase(Locale.US) }
            .filter { it.isNotBlank() && it !in GENERIC_BUCKETS }
        if (buckets.isEmpty()) return null
        val counts = buckets.groupingBy { it }.eachCount()
        val entry = counts.maxByOrNull { it.value } ?: return null
        return if (entry.value >= session.size * 0.55f) entry.key else null
    }

    private fun pickCover(asc: List<Photo>): Photo {
        val head = asc.take(maxOf(1, asc.size / 3))
        return head.filter { it.width >= it.height }
            .maxByOrNull { minOf(it.width, it.height) } ?: head.first()
    }

    private fun sampleIndices(size: Int, count: Int): List<Int> {
        if (size == 0) return emptyList()
        if (size <= count) return (0 until size).toList()
        val step = size / count
        val idx = mutableListOf<Int>()
        var i = step / 2
        while (i < size && idx.size < count) {
            idx += i
            i += step
        }
        return idx
    }

    private suspend fun cityName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val key = String.format(Locale.US, "%.1f,%.1f", lat, lon)
        cityCache.getOrPut(key) {
            runCatching {
                if (!Geocoder.isPresent()) return@getOrPut null
                @Suppress("DEPRECATION")
                Geocoder(appContext, Locale.ENGLISH)
                    .getFromLocation(lat, lon, 1)
                    ?.firstOrNull()
                    ?.let { g ->
                        (g.locality ?: g.subAdminArea ?: g.adminArea ?: g.countryName)
                            ?.uppercase(Locale.ENGLISH)
                            ?.take(28)
                    }
            }.getOrNull()
        }
    }

    private suspend fun monthlyFallback(sorted: List<Photo>, overrides: Map<String, String>): List<Exhibition> {
        val groups = sorted.groupBy { Fmt.monthKey(it.dateMs) }
        val usable = groups.entries
            .filter { it.value.size >= MONTHLY_MIN }
            .sortedByDescending { it.key }
            .take(8)
        if (usable.isNotEmpty()) {
            return usable.map { e ->
                val asc = e.value.sortedBy { it.dateMs }
                val key = "M${e.key}"
                val name = overrides[key] ?: "${Fmt.monthName(asc.first().dateMs)} DAYS"
                Exhibition(key, name.uppercase(Locale.US), null, asc.first().dateMs, asc.last().dateMs, pickCover(asc), asc)
            }
        }
        val latest = sorted.take(FALLBACK_COUNT).sortedBy { it.dateMs }
        val key = "ALL"
        val name = overrides[key] ?: "SELECTED PHOTOGRAPHS"
        return listOf(
            Exhibition(key, name.uppercase(Locale.US), null, latest.first().dateMs, latest.last().dateMs, pickCover(latest), latest)
        )
    }

    companion object {
        private const val GAP_MS = 48L * 60L * 60L * 1000L
        private const val MIN_PHOTOS = 15
        private const val RELAXED_PHOTOS = 6
        private const val MAX_SESSION = 240
        private const val HARD_CAP = 400
        private const val MAX_EXHIBITIONS = 12
        private const val MONTHLY_MIN = 6
        private const val FALLBACK_COUNT = 60
        private const val SAMPLE_COUNT = 8
        private val GENERIC_BUCKETS = setOf(
            "CAMERA", "SCREENSHOTS", "SCREENSHOT", "DOWNLOAD", "DOWNLOADS",
            "PICTURES", "IMAGES", "DCIM", "PHOTOS", "PHOTO", "RESTORED", "EDITED"
        )
    }
}
