package org.nsh07.wikireader.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "translation_cache")
data class TranslationCacheEntry(
    @PrimaryKey val cacheKey: String,
    val providerBaseUrl: String,
    val model: String,
    val sourceLang: String,
    val targetLang: String,
    val kind: String,
    val textHash: String,
    val translatedText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun get(cacheKey: String): TranslationCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TranslationCacheEntry)
}
