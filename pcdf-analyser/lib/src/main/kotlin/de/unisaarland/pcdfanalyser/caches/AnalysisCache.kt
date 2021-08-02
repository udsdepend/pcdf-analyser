package de.unisaarland.pcdfanalyser.caches

import java.io.File

abstract class AnalysisCache<V> {

    abstract fun hasAnalysisResultForFile(pcdfFile: File): Boolean

    abstract fun cachedAnalysisResultForFile(pcdfFile: File): V?

    abstract fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean = true): V?

}