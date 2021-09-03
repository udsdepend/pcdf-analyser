package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.caches.CO2AnalysesQueries
import de.unisaarland.caches.CacheDatabase
import de.unisaarland.pcdfanalyser.analysers.CO2Analyser
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import java.io.File

class CO2AnalysisCache(
    database: CacheDatabase,
    val analysisCacheDelegate: AnalysisCacheDelegate<Double?> = AnalysisCacheDelegate {
        CO2Analyser(it)
    }
) : AnalysisCache<Double?>() {
    private val queries: CO2AnalysesQueries = database.cO2AnalysesQueries

    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, Double?> {
        var result: Pair<Boolean, Double?>? = null
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach {
            result = Pair(
                true,
                it.co2
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

    override fun cachedAnalysisResultForFile(pcdfFile: File): Double? {
        return fetchAnalysisResult(pcdfFile).second
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): Double? {
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

    private fun addAnalysisResultToCache(pcdfFile: File, result: Double?) {
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