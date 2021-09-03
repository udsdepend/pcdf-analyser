package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.caches.CacheDatabase
import de.unisaarland.caches.NOxAnalysesQueries
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.NOxAnalyser
import java.io.File

class NOxAnalysisCache(
    database: CacheDatabase,
    val analysisCacheDelegate: AnalysisCacheDelegate<Double?> = AnalysisCacheDelegate {
        NOxAnalyser(it)
    }
) : AnalysisCache<Double?>() {
    private val queries: NOxAnalysesQueries = database.nOxAnalysesQueries

    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, Double?> {
        var result: Pair<Boolean, Double?>? = null
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach {
            result = Pair(
                true,
                it.nox
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