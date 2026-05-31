package org.nsh07.wikireader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SearchHistoryItem::class,
        ViewHistoryItem::class,
        SavedArticle::class,
        UserLanguage::class,
        StringPreference::class,
        IntPreference::class,
        BooleanPreference::class,
        TranslationCacheEntry::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun savedArticleDao(): SavedArticleDao
    abstract fun viewHistoryDao(): ViewHistoryDao
    abstract fun userLanguageDao(): UserLanguageDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun translationCacheDao(): TranslationCacheDao

    companion object {

        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { Instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `translation_cache` (
                        `cacheKey` TEXT NOT NULL,
                        `providerBaseUrl` TEXT NOT NULL,
                        `model` TEXT NOT NULL,
                        `sourceLang` TEXT NOT NULL,
                        `targetLang` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `textHash` TEXT NOT NULL,
                        `translatedText` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`cacheKey`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
