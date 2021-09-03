package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.caches.CacheDatabase
import de.unisaarland.caches.SupportedPIDsAnalysesQueries
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.SupportedPIDsAnalyser
import de.unisaarland.pcdfanalyser.model.ParameterID
import de.unisaarland.pcdfanalyser.model.ParameterSupport
import de.unisaarland.pcdfanalyser.model.ParameterSupport.Record
import java.io.File

class SupportedPIDsAnalysisCache(
    val database: CacheDatabase,
    val analysisCacheDelegate: AnalysisCacheDelegate<ParameterSupport> = AnalysisCacheDelegate {
        SupportedPIDsAnalyser(
            it
        )
    }
) : AnalysisCache<ParameterSupport>() {

    private val queries: SupportedPIDsAnalysesQueries = database.supportedPIDsAnalysesQueries

    private fun fetchAnalysisResult(pcdfFile: File): ParameterSupport? {
        val records = mutableListOf<Record>()
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach {
            records.add(
                Record(
                    ParameterID(it.PID, it.mode),
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
        queries.transaction {
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
