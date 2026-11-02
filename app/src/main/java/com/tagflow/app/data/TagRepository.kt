package com.tagflow.app.data

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    private val logDao: LogDao
) {
    val tags = tagDao.observeAll()
    val logCount = logDao.observeCount()

    fun tag(id: Long) = tagDao.observe(id)
    fun recentLogs(limit: Int) = logDao.observeRecent(limit)
    fun logsFor(uid: String) = logDao.observeForUid(uid)

    suspend fun getById(id: Long) = tagDao.getById(id)
    suspend fun findByUid(uid: String) = tagDao.findByUid(uid)
    suspend fun save(tag: TagEntity): Long = tagDao.insert(tag)
    suspend fun update(tag: TagEntity) = tagDao.update(tag)
    suspend fun delete(id: Long) = tagDao.delete(id)
    suspend fun markScanned(id: Long) = tagDao.markScanned(id, System.currentTimeMillis())

    suspend fun log(entry: ScanLogEntity) {
        logDao.insert(entry)
        logDao.trim()
    }

    suspend fun clearLogs() = logDao.clear()

    suspend fun exportJson(): String {
        val arr = JSONArray()
        tagDao.getAll().forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("uid", t.uid)
                    put("name", t.name)
                    put("color", t.colorIndex)
                    put("icon", t.iconKey)
                    put("notes", t.notes)
                    put("enabled", t.enabled)
                    put("actions", JSONArray(t.actionsJson))
                    put("ndef", t.ndefBase64 ?: JSONObject.NULL)
                    put("created", t.createdAt)
                }
            )
        }
        return JSONObject().put("app", "TagFlow").put("version", 1).put("tags", arr).toString(2)
    }

    /** Returns how many tags were imported (existing UIDs are skipped). */
    suspend fun importJson(json: String): Int {
        val arr = JSONObject(json).getJSONArray("tags")
        var imported = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val uid = o.getString("uid")
            if (tagDao.findByUid(uid) != null) continue
            tagDao.insert(
                TagEntity(
                    uid = uid,
                    name = o.optString("name", "Tag"),
                    colorIndex = o.optInt("color", 0),
                    iconKey = o.optString("icon", "tag"),
                    notes = o.optString("notes", ""),
                    enabled = o.optBoolean("enabled", true),
                    actionsJson = o.optJSONArray("actions")?.toString() ?: "[]",
                    ndefBase64 = if (o.isNull("ndef")) null else o.getString("ndef"),
                    createdAt = o.optLong("created", System.currentTimeMillis())
                )
            )
            imported++
        }
        return imported
    }
}
