package de.unisaarland.pcdfanalyser.caches

import org.jetbrains.exposed.sql.Database
import java.io.File

abstract class AnalysisCache<V> {

    abstract fun hasAnalysisResultForFile(pcdfFile: File): Boolean

    abstract fun cachedAnalysisResultForFile(pcdfFile: File): V?

    abstract fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean = true): V?



    companion object {
        private val sharedDatabases: MutableMap<File, Database> = mutableMapOf()

        fun sharedDatabase(cacheFile: File): Database {
            if (sharedDatabases.containsKey(cacheFile)) {
                return sharedDatabases[cacheFile]!!
            } else {
                println("Creating a new database instance for $cacheFile")
                val db = Database.connect("jdbc:sqlite:${cacheFile.absolutePath}", "org.sqlite.JDBC")
                sharedDatabases[cacheFile] = db
                return db
            }

        }
    }

}