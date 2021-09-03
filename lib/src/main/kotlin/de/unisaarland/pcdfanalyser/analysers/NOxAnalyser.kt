package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfUtilities.NOxMassFlowComputation
import pcdfUtilities.computeNOxMGPerKM

class NOxAnalyser(eventStream: EventStream): Analyser<Double?>(eventStream) {

    override fun analysisIsAvailable(): Boolean {
        // ToDo: Computing this function is not trivial without doing the actual computation
        return true
    }

    override fun analyse(): Double? {
        try {
            val noxStream = NOxMassFlowComputation(eventStream)
            return computeNOxMGPerKM(noxStream)
        } catch (error: Exception) {
            println(error)
            return null
        }
    }

}