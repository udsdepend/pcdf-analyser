package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfUtilities.CO2MassFlowComputation
import pcdfUtilities.computeCO2MGPerKM

class CO2Analyser(eventStream: EventStream): Analyser<Double?>(eventStream) {

    override fun analysisIsAvailable(): Boolean {
        // ToDo: Computing this function is not trivial without doing the actual computation
        return true
    }

    override fun analyse(): Double? {
        try {
            val noxStream = CO2MassFlowComputation(eventStream)
            return computeCO2MGPerKM(noxStream)
        } catch (error: Exception) {
            println(error)
            return null
        }
    }

}