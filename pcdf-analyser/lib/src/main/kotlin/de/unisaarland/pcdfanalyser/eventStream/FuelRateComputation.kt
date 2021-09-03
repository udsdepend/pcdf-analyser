package pcdfUtilities

import de.unisaarland.pcdfanalyser.analysers.SupportedPIDsAnalyser
import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.getFuelAirEquivalenceRatio
import de.unisaarland.pcdfanalyser.eventStream.getFuelRate
import de.unisaarland.pcdfanalyser.eventStream.getMassAirFlow
import de.unisaarland.pcdfanalyser.model.ParameterID
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.OBDCommand

class FuelRateComputation(inputStream: EventStream) : AbstractStreamTransducer(inputStream) {
    private val parameterConfiguration = getParameterConfigurationForInputStream(inputStream)
    private val inputStreamHasComputedFuelrateEvents = inputStream.any { it is ComputedFuelRateEvent }
    init {
        if (inputStreamHasComputedFuelrateEvents) {
            println("Notice: Fuel rate already computed.")
        }
    }

    init {
        if (parameterConfiguration.isInvalid) {
            throw IllegalArgumentException("Fuel rate cannot be computed for this input stream!")
        }
    }

    override fun iterator(): Iterator<PCDFEvent> {
        return if (inputStreamHasComputedFuelrateEvents) {
            // No more computation is necessary
            inputStream.iterator()
        } else {
            ComputedFuelRateIterator(inputStream.iterator(), parameterConfiguration)
        }
    }




    private open class ParameterConfiguration(
        val noxPid: Int?,
        val massAirFlowPid: Int?,
        val fuelRatePid: Int?,
        val fuelAirEquivalencePid: Int?
    ) {
        val isConfig1 = noxPid != null && massAirFlowPid != null && fuelRatePid != null

        val isConfig2 = noxPid != null && massAirFlowPid != null && fuelAirEquivalencePid != null && fuelRatePid == null

        val isConfig3 = noxPid != null && massAirFlowPid != null && fuelRatePid == null && fuelAirEquivalencePid == null

        val isInvalid = noxPid == null || massAirFlowPid == null

        val isValid = !isInvalid

        val allPids = listOfNotNull(noxPid, massAirFlowPid, fuelRatePid, fuelAirEquivalencePid)

        override fun toString(): String {
            val pidMap = "internal: {noxPID = 0x${noxPid?.toString(16)}, massAirFlowPID = 0x${massAirFlowPid?.toString(16)}, fuelRatePID = 0x${fuelRatePid?.toString(16)}, fuelAirEquivalenceRatioPID = 0x${fuelAirEquivalencePid?.toString(16)}}"
            val validity = "valid: $isValid"
            val config = "config: " + when {
                isConfig1 -> "1"
                isConfig2 -> "2"
                isConfig3 -> "3"
                else -> "n/a"
            }
            return "Parameter Configuration {\n   $pidMap,\n   $validity,\n   $config\n}"
        }
    }



    companion object {
        fun inputStreamIsCompatible(inputStream: EventStream): Boolean {
            return getParameterConfigurationForInputStream(inputStream).isValid
        }

        private fun getParameterConfigurationForInputStream(inputStream: EventStream): ParameterConfiguration {
            val availablePIDs = SupportedPIDsAnalyser(inputStream).analyse() //inputStream.availablePIDs

            val noxPid = when {
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR.pid)) -> OBDCommand.NOX_SENSOR.pid
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_ALTERNATIVE.pid)) -> OBDCommand.NOX_SENSOR_ALTERNATIVE.pid
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_CORRECTED.pid)) -> OBDCommand.NOX_SENSOR_CORRECTED.pid
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.NOX_SENSOR_CORRECTED_ALTERNATIVE.pid)) -> OBDCommand.NOX_SENSOR_CORRECTED_ALTERNATIVE.pid
                else -> null
            }

            val massFlowPid = when {
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.MAF_AIR_FLOW_RATE.pid)) -> OBDCommand.MAF_AIR_FLOW_RATE.pid
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.MAF_AIR_FLOW_RATE_SENSOR.pid)) -> OBDCommand.MAF_AIR_FLOW_RATE_SENSOR.pid
                else -> null
            }

            val fuelRatePid = when {
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.ENGINE_FUEL_RATE.pid)) -> OBDCommand.ENGINE_FUEL_RATE.pid
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.ENGINE_FUEL_RATE_MULTI.pid)) -> OBDCommand.ENGINE_FUEL_RATE_MULTI.pid
                else -> null
            }

            val fuelAirEquivalencePid = when {
                availablePIDs.isParameterAvailable(ParameterID(OBDCommand.FUEL_AIR_EQUIVALENCE_RATIO.pid)) -> OBDCommand.FUEL_AIR_EQUIVALENCE_RATIO.pid
                else -> null
            }

            return ParameterConfiguration(
                noxPid,
                massFlowPid,
                fuelRatePid,
                fuelAirEquivalencePid
            )
        }
    }



    private class ComputedFuelRateIterator(val inputIterator: Iterator<PCDFEvent>, val configuration: ParameterConfiguration): Iterator<PCDFEvent> {
        var fuelRate: Double? = null
        var fuelAirEquivalenceRatio: Double? = null
        var massAirFlow: Double? = null

        var nextEvent: PCDFEvent? = null

        val canComputeNewValue: Boolean
            get() {
                return massAirFlow != null && when {
                    configuration.isConfig1 -> fuelRate != null
                    configuration.isConfig2 -> fuelAirEquivalenceRatio != null
                    configuration.isConfig3 -> true
                    else -> false
                }
            }

        fun processEvent(event: PCDFEvent) {
            event.getFuelRate()?.let { fuelRate = it }
            event.getFuelAirEquivalenceRatio()?.let { fuelAirEquivalenceRatio = it }
            event.getMassAirFlow()?.let { massAirFlow = it }
        }

        fun computeNewValue(): Double {
            return when {
                configuration.isConfig1 -> fuelRate!!
                else -> {
                    val cFAEquivalenceRatio = when {
                        configuration.isConfig2 -> fuelAirEquivalenceRatio!!
                        configuration.isConfig3 -> 1.0
                        else -> throw Error("Invalid configuration!")
                    }
                    val consumptionMAF = massAirFlow!! / (14.5 * cFAEquivalenceRatio)
                    consumptionMAF / 832.0 * 3600.0
                }
            }
        }

        fun resetVariables() {
            fuelRate = null
            fuelAirEquivalenceRatio = null
            massAirFlow = null
        }


        override fun hasNext(): Boolean {
            return nextEvent != null || inputIterator.hasNext()
        }

        override fun next(): PCDFEvent {
            if (nextEvent != null) {
                val res = nextEvent!!
                nextEvent = null
                return res
            } else {
                val nextInputEvent = inputIterator.next()
                processEvent(nextInputEvent)
                if (canComputeNewValue) {
                    val newValue = computeNewValue()
                    nextEvent = ComputedFuelRateEvent(nextInputEvent.timestamp, newValue)
//                    PCDFEvent("FuelRateComputation", EventType.ANALYSER, nextInputEvent.timestamp, eventData)
                    resetVariables()
                }
                return nextInputEvent
            }
        }
    }


    class ComputedFuelRateEvent(timestamp: NanoSeconds, val fuelrate: Double): PCDFEvent("FuelRateComputation", EventType.CUSTOM, timestamp) {

        fun t() {
            this.fuelrate
        }
    }


//    class ComputedFuelRateEventData(val fuelrate: Double): PCDFEvent() {
//        override fun getPattern(): PCDFDataPattern {
//            TODO("Not yet implemented")
//        }
//    }
}