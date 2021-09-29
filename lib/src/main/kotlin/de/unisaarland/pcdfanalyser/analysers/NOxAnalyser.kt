package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.FaultyNOxSensorElimination
import pcdfUtilities.NOxMassFlowComputation
import pcdfUtilities.computeNOxMGPerKM

/**
 * Implementation of the approximation of emitted NOx during the trip recorded into [eventStream].
 */
class NOxAnalyser(eventStream: EventStream): Analyser<Double?>(eventStream) {

    /**
     * Currently, there is no efficient implementation to check if NOx can be approximated.
     * @return always true
     */
    override fun analysisIsAvailable(): Boolean {
        // ToDo: Computing this function is not trivial without doing the actual computation
        return true
    }

    /**
     * Performs the approximation of NOx, by accumulating NOx mass flow
     * @return The average amount of NOx emitted in [eventStream] in mg/km, or null, if NOx cannot be computed.
     */
    override fun analyse(): Double? {
        try {
            val noxStream = NOxMassFlowComputation(FaultyNOxSensorElimination(eventStream))
            return computeNOxMGPerKM(noxStream)
        } catch (error: Exception) {
            println(error)
            return null
        }
    }

}