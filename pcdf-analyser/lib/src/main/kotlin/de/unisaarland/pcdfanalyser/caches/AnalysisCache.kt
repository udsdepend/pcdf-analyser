package de.unisaarland.pcdfanalyser.caches

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import de.unisaarland.caches.CacheDatabase
import java.io.File

abstract class AnalysisCache<V> {

    abstract fun hasAnalysisResultForFile(pcdfFile: File): Boolean

    abstract fun cachedAnalysisResultForFile(pcdfFile: File): V?

    abstract fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean = true): V


    companion object {
        private val sharedDatabases: MutableMap<File, CacheDatabase> = mutableMapOf()

        fun sharedDatabase(
            cacheFile: File,
            driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${cacheFile.path}")
        ): CacheDatabase {
            return if (sharedDatabases.containsKey(cacheFile)) {
                sharedDatabases[cacheFile]!!
            } else {
                println("Creating a new database instance for $cacheFile")
                try {
                    CacheDatabase.Schema.create(driver)
                } catch (e: Exception) {
                    // file was already an exisiting database
                }
                val db = CacheDatabase(driver)
                sharedDatabases[cacheFile] = db
                db
            }
        }
    }
}