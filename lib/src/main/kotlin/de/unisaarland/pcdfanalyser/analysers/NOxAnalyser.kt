package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.FaultySensorElimination
import de.unisaarland.pcdfanalyser.eventStream.getNOX
import de.unisaarland.pcdfanalyser.model.ParameterID
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorCorrectedAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorCorrectedEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfUtilities.NOX_MAX
import de.unisaarland.pcdfanalyser.eventStream.NOxMassFlowComputation
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
            val supportedPIDs = SupportedPIDsAnalyser(eventStream).analyse()
            val noxReader: (PCDFEvent) -> Int? =
                //TODO: Improve this by choosing sensor 1 or 2 (3 or 4) instead of using minimum
                when {
                    supportedPIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_CORRECTED_ALTERNATIVE)) ->
                        { event -> if (event is NOXSensorCorrectedAlternativeEvent) event.getNOX() else null }
                    supportedPIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_ALTERNATIVE)) ->
                        { event -> if (event is NOXSensorAlternativeEvent) event.getNOX() else null }
                    supportedPIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_CORRECTED)) ->
                        { event -> if (event is NOXSensorCorrectedEvent) event.getNOX() else null }
                    supportedPIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR)) ->
                        { event -> if (event is NOXSensorEvent) event.getNOX() else null }
                    else -> return null // NOx analysis is not available
                }

            val sensorVerdict: (PCDFEvent) -> FaultySensorElimination.FaultySensorEliminationIterator.SensorState = {event ->

                when (noxReader(event)) {
                    NOX_MAX -> FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Faulty
                    null -> FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Irrelevant
                    else -> FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Valid
                }
            }

            val noxStream = NOxMassFlowComputation(FaultySensorElimination(eventStream, sensorVerdict), noxReader)
            return computeNOxMGPerKM(noxStream)
        } catch (error: Exception) {
            println(error)
            return null
        }
    }

}