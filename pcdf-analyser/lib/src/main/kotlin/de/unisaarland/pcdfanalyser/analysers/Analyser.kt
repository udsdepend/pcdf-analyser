package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.EventStream

abstract class Analyser<V>(val eventStream: EventStream) {

    abstract fun analysisIsAvailable(): Boolean
    abstract fun analyse(): V

}
