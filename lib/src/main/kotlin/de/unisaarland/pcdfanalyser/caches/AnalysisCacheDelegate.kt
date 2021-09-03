package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.analysers.Analyser

class AnalysisCacheDelegate<T>(private val code: ((EventStream) -> Analyser<T>)){

    fun analyserForEventStream(eventStream: EventStream): Analyser<T> {
        return code(eventStream)
    }

}