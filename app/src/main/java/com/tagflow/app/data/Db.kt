package com.tagflow.app.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val name: String,
    val colorIndex: Int = 0,
    val iconKey: String = "tag",
    val notes: String = "",
    val actionsJson: String = "[]",
    val enabled: Boolean = true,
    val ndefBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastScanAt: Long = 0,
    val scanCount: Int = 0
)

@Entity(tableName = "scan_log")
data class ScanLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val tagName: String?,
    val summary: String,
    val ranActions: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY lastScanAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    fun observe(id: Long): Flow<TagEntity?>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE uid = :uid LIMIT 1")
    suspend fun findByUid(uid: String): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE tags SET scanCount = scanCount + 1, lastScanAt = :ts WHERE id = :id")
    suspend fun markScanned(id: Long, ts: Long)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM scan_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanLogEntity>>

    @Query("SELECT * FROM scan_log WHERE uid = :uid ORDER BY timestamp DESC LIMIT 30")
    fun observeForUid(uid: String): Flow<List<ScanLogEntity>>

    @Query("SELECT COUNT(*) FROM scan_log")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(log: ScanLogEntity)

    @Query("DELETE FROM scan_log")
    suspend fun clear()

    @Query("DELETE FROM scan_log WHERE id NOT IN (SELECT id FROM scan_log ORDER BY timestamp DESC LIMIT 500)")
    suspend fun trim()
}

@Database(entities = [TagEntity::class, ScanLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun logDao(): LogDao
}
