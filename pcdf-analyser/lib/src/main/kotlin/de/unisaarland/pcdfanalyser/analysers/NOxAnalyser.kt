package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.EventStream

class NOxAnalyser(eventStream: EventStream): Analyser<Double?>(eventStream) {

    override fun analysisIsAvailable(): Boolean {
        return false
    }

    override fun analyse(): Double? {
        return -1.0
    }

}