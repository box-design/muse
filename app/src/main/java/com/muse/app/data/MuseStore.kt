package com.muse.app.data

import android.content.Context
import com.muse.app.data.model.ArchiveEntry
import com.muse.app.data.model.CropMode
import com.muse.app.data.model.PaletteRecord
import com.muse.app.data.model.PostcardRecord
import com.muse.app.data.model.ZineCoverStyle
import com.muse.app.data.model.ZinePage
import com.muse.app.data.model.ZinePageLayout
import com.muse.app.data.model.ZinePaper
import com.muse.app.data.model.ZineRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StoreData(
    val archiveSeq: Int = 0,
    val nextId: Long = 1L,
    val archives: List<ArchiveEntry> = emptyList(),
    val postcards: List<PostcardRecord> = emptyList(),
    val palettes: List<PaletteRecord> = emptyList(),
    val zines: List<ZineRecord> = emptyList(),
    val exhibitionNames: Map<String, String> = emptyMap()
)

class MuseStore(context: Context) {

    private val file = File(context.filesDir, "muse_store.json")
    private val mutex = Mutex()

    var data: StoreData = StoreData()
        private set

    init {
        data = runCatching { read() }.getOrDefault(StoreData())
    }

    private fun read(): StoreData {
        if (!file.exists()) return StoreData()
        val root = JSONObject(file.readText())
        val archives = mutableListOf<ArchiveEntry>()
        val archiveArr = root.optJSONArray("archives") ?: JSONArray()
        for (i in 0 until archiveArr.length()) {
            val o = archiveArr.getJSONObject(i)
            archives += ArchiveEntry(
                id = o.getLong("id"),
                type = o.getString("type"),
                mediaId = if (o.has("mediaId") && !o.isNull("mediaId")) o.getLong("mediaId") else null,
                postcardId = if (o.has("postcardId") && !o.isNull("postcardId")) o.getLong("postcardId") else null,
                paletteId = if (o.has("paletteId") && !o.isNull("paletteId")) o.getLong("paletteId") else null,
                zineId = if (o.has("zineId") && !o.isNull("zineId")) o.getLong("zineId") else null,
                number = o.getInt("number"),
                title = o.optString("title", ""),
                subtitle = o.optString("subtitle", ""),
                createdAt = o.getLong("createdAt")
            )
        }
        val postcards = mutableListOf<PostcardRecord>()
        val pcArr = root.optJSONArray("postcards") ?: JSONArray()
        for (i in 0 until pcArr.length()) {
            val o = pcArr.getJSONObject(i)
            postcards += PostcardRecord(
                id = o.getLong("id"),
                mediaId = o.getLong("mediaId"),
                photoAspect = o.getDouble("photoAspect").toFloat(),
                templateId = o.getString("templateId"),
                cropMode = runCatching { CropMode.valueOf(o.getString("cropMode")) }.getOrDefault(CropMode.FILL),
                offsetX = o.getDouble("offsetX").toFloat(),
                offsetY = o.getDouble("offsetY").toFloat(),
                title = o.optString("title", ""),
                subtitle = o.optString("subtitle", ""),
                dateLabel = o.optString("dateLabel", ""),
                placeLabel = o.optString("placeLabel", ""),
                showDate = o.optBoolean("showDate", true),
                showLocation = o.optBoolean("showLocation", true),
                showNumber = o.optBoolean("showNumber", false),
                numberLabel = o.optString("numberLabel", ""),
                paperArgb = o.getInt("paperArgb").toInt(),
                inkArgb = o.getInt("inkArgb").toInt(),
                paletteId = if (o.has("paletteId") && !o.isNull("paletteId")) o.getLong("paletteId") else null,
                createdAt = o.getLong("createdAt"),
                updatedAt = o.getLong("updatedAt")
            )
        }
        val palettes = mutableListOf<PaletteRecord>()
        val palArr = root.optJSONArray("palettes") ?: JSONArray()
        for (i in 0 until palArr.length()) {
            val o = palArr.getJSONObject(i)
            val colors = mutableListOf<Int>()
            val cArr = o.getJSONArray("colors")
            for (j in 0 until cArr.length()) colors += cArr.getInt(j)
            palettes += PaletteRecord(
                id = o.getLong("id"),
                name = o.optString("name", ""),
                mediaId = o.getLong("mediaId"),
                colors = colors,
                createdAt = o.getLong("createdAt")
            )
        }
        val zines = mutableListOf<ZineRecord>()
        val zineArr = root.optJSONArray("zines") ?: JSONArray()
        for (i in 0 until zineArr.length()) {
            val o = zineArr.getJSONObject(i)
            val pages = mutableListOf<ZinePage>()
            val pageArr = o.optJSONArray("pages") ?: JSONArray()
            for (j in 0 until pageArr.length()) {
                val po = pageArr.getJSONObject(j)
                pages += ZinePage(
                    mediaId = po.getLong("mediaId"),
                    layout = if (po.has("layout") && !po.isNull("layout")) {
                        runCatching { ZinePageLayout.valueOf(po.getString("layout")) }.getOrNull()
                    } else null
                )
            }
            zines += ZineRecord(
                id = o.getLong("id"),
                title = o.optString("title", ""),
                coverMediaId = o.getLong("coverMediaId"),
                pages = pages,
                paper = runCatching { ZinePaper.valueOf(o.getString("paper")) }.getOrDefault(ZinePaper.MATTE),
                coverStyle = runCatching { ZineCoverStyle.valueOf(o.getString("coverStyle")) }
                    .getOrDefault(ZineCoverStyle.FULL_BLEED),
                defaultLayout = runCatching { ZinePageLayout.valueOf(o.getString("defaultLayout")) }
                    .getOrDefault(ZinePageLayout.GALLERY),
                createdAt = o.getLong("createdAt"),
                updatedAt = o.getLong("updatedAt")
            )
        }
        val names = mutableMapOf<String, String>()
        val namesObj = root.optJSONObject("exhibitionNames") ?: JSONObject()
        namesObj.keys().forEach { key -> names[key] = namesObj.getString(key) }
        return StoreData(
            archiveSeq = root.optInt("archiveSeq", 0),
            nextId = root.optLong("nextId", 1L),
            archives = archives,
            postcards = postcards,
            palettes = palettes,
            zines = zines,
            exhibitionNames = names
        )
    }

    private fun encode(d: StoreData): JSONObject {
        val root = JSONObject()
        root.put("archiveSeq", d.archiveSeq)
        root.put("nextId", d.nextId)
        val archiveArr = JSONArray()
        d.archives.forEach { a ->
            archiveArr.put(JSONObject().apply {
                put("id", a.id)
                put("type", a.type)
                a.mediaId?.let { put("mediaId", it) }
                a.postcardId?.let { put("postcardId", it) }
                a.paletteId?.let { put("paletteId", it) }
                a.zineId?.let { put("zineId", it) }
                put("number", a.number)
                put("title", a.title)
                put("subtitle", a.subtitle)
                put("createdAt", a.createdAt)
            })
        }
        root.put("archives", archiveArr)
        val pcArr = JSONArray()
        d.postcards.forEach { p ->
            pcArr.put(JSONObject().apply {
                put("id", p.id)
                put("mediaId", p.mediaId)
                put("photoAspect", p.photoAspect.toDouble())
                put("templateId", p.templateId)
                put("cropMode", p.cropMode.name)
                put("offsetX", p.offsetX.toDouble())
                put("offsetY", p.offsetY.toDouble())
                put("title", p.title)
                put("subtitle", p.subtitle)
                put("dateLabel", p.dateLabel)
                put("placeLabel", p.placeLabel)
                put("showDate", p.showDate)
                put("showLocation", p.showLocation)
                put("showNumber", p.showNumber)
                put("numberLabel", p.numberLabel)
                put("paperArgb", p.paperArgb)
                put("inkArgb", p.inkArgb)
                p.paletteId?.let { put("paletteId", it) }
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("postcards", pcArr)
        val palArr = JSONArray()
        d.palettes.forEach { p ->
            val cArr = JSONArray()
            p.colors.forEach { cArr.put(it) }
            palArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("mediaId", p.mediaId)
                put("colors", cArr)
                put("createdAt", p.createdAt)
            })
        }
        root.put("palettes", palArr)
        val zineArr = JSONArray()
        d.zines.forEach { z ->
            val pageArr = JSONArray()
            z.pages.forEach { p ->
                pageArr.put(JSONObject().apply {
                    put("mediaId", p.mediaId)
                    p.layout?.let { put("layout", it.name) }
                })
            }
            zineArr.put(JSONObject().apply {
                put("id", z.id)
                put("title", z.title)
                put("coverMediaId", z.coverMediaId)
                put("pages", pageArr)
                put("paper", z.paper.name)
                put("coverStyle", z.coverStyle.name)
                put("defaultLayout", z.defaultLayout.name)
                put("createdAt", z.createdAt)
                put("updatedAt", z.updatedAt)
            })
        }
        root.put("zines", zineArr)
        val namesObj = JSONObject()
        d.exhibitionNames.forEach { (k, v) -> namesObj.put(k, v) }
        root.put("exhibitionNames", namesObj)
        return root
    }

    suspend fun mutate(transform: (StoreData) -> StoreData): StoreData = mutex.withLock {
        val next = transform(data)
        data = next
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(encode(next).toString())
                if (file.exists()) file.delete()
                tmp.renameTo(file)
            }
        }
        next
    }
}
