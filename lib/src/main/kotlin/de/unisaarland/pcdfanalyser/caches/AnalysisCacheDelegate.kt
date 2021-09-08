package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.analysers.Analyser

/**
 * Delegate to perform an analysis if a cached analysis result is not available.
 * [code] is the code to be executed to perform the analysis.
 *
 * If more advanced delegation techniques are necessary, this class can be subclassed.
 */
open class AnalysisCacheDelegate<T>(private val code: ((EventStream) -> Analyser<T>)){

    /**
     * Runs [code] with the given [eventStream].
     */
    open fun analyserForEventStream(eventStream: EventStream): Analyser<T> {
        return code(eventStream)
    }

}