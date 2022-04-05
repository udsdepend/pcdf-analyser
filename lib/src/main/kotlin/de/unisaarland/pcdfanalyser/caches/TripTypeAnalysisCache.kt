package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.caches.CacheDatabase
import de.unisaarland.caches.TripTypeAnalysesQueries
import de.unisaarland.pcdfanalyser.analysers.SimResult
import de.unisaarland.pcdfanalyser.analysers.TripTypeAnalyser
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import java.io.File

class TripTypeAnalysisCache(
    database: CacheDatabase,
    val analysisCacheDelegate: AnalysisCacheDelegate<SimResult?> = AnalysisCacheDelegate {
        TripTypeAnalyser(
            it
        )
    }
): AnalysisCache<SimResult?>() {
    private val queries: TripTypeAnalysesQueries = database.tripTypeAnalysesQueries

    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, SimResult?> {
        var result: Pair<Boolean, SimResult?>? = null
        queries.selectByName(pcdfFile.absolutePath).executeAsList().forEach{
            result = Pair(
                true,
                it.type?.let { it1 -> SimResult.valueOf(it1) }
            )
        }

        return if (result == null) {
            Pair(false, null)
        }  else {
            result!!
        }
    }
    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return queries.selectByName(pcdfFile.absolutePath).executeAsList().isNotEmpty()
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): SimResult? {
        return fetchAnalysisResult(pcdfFile).second
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): SimResult? {
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

    private fun addAnalysisResultToCache(pcdfFile: File, type: SimResult?) {
        queries.insert(
            pcdfFile.absolutePath,
            type.toString(),
            VERSION
        )
    }

    companion object {
        const val VERSION = 1
    }

}