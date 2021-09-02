package de.unisaarland.pcdfanalyser.caches

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import de.unisaarland.caches.CacheDatabase
import de.unisaarland.caches.VINAnalysisQueries
import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.VINAnalyser
import java.io.File

class VINAnalysisCache(
    file: File,
    val analysisCacheDelegate: AnalysisCacheDelegate<String?> = AnalysisCacheDelegate {
        VINAnalyser(
            it
        )
    }
) :
    AnalysisCache<String?>() {
    //TODO: static file or variable?
    private val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${file.path}")
    private val database: CacheDatabase
    private val queries: VINAnalysisQueries

    init {
        try {
            CacheDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Table was already created
        }
        database = CacheDatabase(driver)
        queries = database.vINAnalysisQueries
    }

    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, String?> {
        var result: Pair<Boolean, String?>? = null
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach {
            result = Pair(
                true,
                it.anylsis_result
            )
        }

        return if (result == null) {
            Pair(false, null)
        } else {
            result!!
        }
    }

    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return queries.selectByName(pcdfFile.absolutePath).executeAsList().isNotEmpty()
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): String? {
        return fetchAnalysisResult(pcdfFile).second
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): String? {
        val fetchResult = fetchAnalysisResult(pcdfFile)
        return if (!fetchResult.first) {
            val analyser = analysisCacheDelegate.analyserForEventStream(FileEventStream(pcdfFile))
            val result = analyser.analyse()
            if (cacheResult) {
                addAnalysisResultToCache(pcdfFile, result)
            }
            result
        } else {
            fetchResult.second
        }
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: String?) {
        queries.insert(
            pcdfFile.absolutePath,
            result,
            VERSION
        )
    }

    companion object {
        const val VERSION = 1
    }
}