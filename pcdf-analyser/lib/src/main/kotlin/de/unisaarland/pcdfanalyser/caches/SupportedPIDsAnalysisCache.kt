package de.unisaarland.pcdfanalyser.caches

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import de.unisaarland.caches.CacheDatabase
import de.unisaarland.caches.SupportedPIDsAnalysisQueries
import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.SupportedPIDsAnalyser
import de.unisaarland.pcdfanalyser.model.ParameterID
import de.unisaarland.pcdfanalyser.model.ParameterSupport
import de.unisaarland.pcdfanalyser.model.ParameterSupport.Record
import java.io.File

class SupportedPIDsAnalysisCache(
    val file: File,
    val analysisCacheDelegate: AnalysisCacheDelegate<ParameterSupport> = AnalysisCacheDelegate {
        SupportedPIDsAnalyser(
            it
        )
    }
) : AnalysisCache<ParameterSupport>() {

    private val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${file.path}")
    private val database: CacheDatabase
    private val queries: SupportedPIDsAnalysisQueries

    init {
        try {
            CacheDatabase.Schema.create(driver)
        } catch (e: Exception) {
            // Table was already created
        }
        database = CacheDatabase(driver)
        queries = database.supportedPIDsAnalysisQueries
    }

    private fun fetchAnalysisResult(pcdfFile: File): ParameterSupport? {
        val records = mutableListOf<Record>()
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach {
            records.add(
                Record(
                    ParameterID(it.pid, it.mode),
                    it.isSupported,
                    it.isAvailable
                )
            )
        }

        return if (records.isEmpty()) {
            null
        } else {
            ParameterSupport(records)
        }
    }

    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return queries.selectByName(pcdfFile.absolutePath).executeAsList().isNotEmpty()
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): ParameterSupport? {
        return fetchAnalysisResult(pcdfFile)
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: ParameterSupport) {
        for (record in result.parameterRecords) {
            queries.insert(
                pcdfFile.absolutePath,
                record.parameterID.mode,
                record.parameterID.id,
                record.supported,
                record.available,
                VERSION
            )
        }
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): ParameterSupport {
        val fetchResult = fetchAnalysisResult(pcdfFile)
        return if (fetchResult == null) {
            val analyser = analysisCacheDelegate.analyserForEventStream(FileEventStream(pcdfFile))
            val result = analyser.analyse()
            if (cacheResult) {
                addAnalysisResultToCache(pcdfFile, result)
            }
            result
        } else {
            fetchResult
        }
    }

    companion object {
        const val VERSION: Int = 1
    }

}
