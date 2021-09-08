package de.unisaarland.pcdfanalyser.caches

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import de.unisaarland.caches.CacheDatabase
import java.io.File

/**
 * Abstract class for coordination of analysis and caching analysis results.
 * @param V: The type of analysis results
 */
abstract class AnalysisCache<V> {

    /**
     * Checks if analysis results are available for [pcdfFile].
     */
    abstract fun hasAnalysisResultForFile(pcdfFile: File): Boolean

    /**
     * Returns analysis results for [pcdfFile] if available, or null otherwise
     */
    abstract fun cachedAnalysisResultForFile(pcdfFile: File): V?

    /**
     * Returns analysis results for [pcdfFile] if available, or computes and returns the result otherwise.
     * If [cacheResult] is true, the analysis result is stored in the cache record.
     */
    abstract fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean = true): V


    companion object {
        private val sharedDatabases: MutableMap<File, CacheDatabase> = mutableMapOf()

        /**
         * Databases should be opened only once. This method remembers which databases are
         * already opened, and opens a database if it is requested for the first time.
         * @return A database object for analysis caching.
         */
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