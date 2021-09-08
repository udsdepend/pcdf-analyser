package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfUtilities.CO2MassFlowComputation
import pcdfUtilities.computeCO2MGPerKM

/**
 * Implementation of the approximation of emitted CO2 during the trip recorded into [eventStream].
 */
class CO2Analyser(eventStream: EventStream): Analyser<Double?>(eventStream) {

    /**
     * Currently, there is no efficient implementation to check if CO2 can be approximated.
     * @return always true
     */
    override fun analysisIsAvailable(): Boolean {
        // ToDo: Find an efficient way of checking this
        return true
    }

    /**
     * Performs the approximation of CO2, by accumulating CO2 mass flow
     * @return The average amount of CO2 emitted in [eventStream] in g/km, or null, if CO2 cannot be computed.
     */
    override fun analyse(): Double? {
        try {
            val co2Stream = CO2MassFlowComputation(eventStream)
            return computeCO2MGPerKM(co2Stream)
        } catch (error: Exception) {
            println(error)
            return null
        }
    }

}