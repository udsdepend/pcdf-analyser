package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.getComputedExhaustMassFlow
import de.unisaarland.pcdfanalyser.eventStream.getComputedFuelRate
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent

/**
 * PCDF stream transducer to insert CO2 mass flow in [inputStream].
 * Uses [ExhaustMassFlowComputation] (and [FuelRateComputation]) to compute the exhaust mass flow.
 * Computation is done according to the EU Commission Regulation 2017/1151, Appendix 4.
 */
class CO2MassFlowComputation (inputStream: EventStream) : AbstractStreamTransducer(ExhaustMassFlowComputation(inputStream)) {
    private val inputStreamHasCO2Events = inputStream.any { it is ComputedCO2MassFlowEvent }
    init {
        if (inputStreamHasCO2Events) {
            println("Notice: CO2 already computed.")
        }
    }

    override fun iterator(): Iterator<PCDFEvent> {
        return if (inputStreamHasCO2Events) {
            inputStream.iterator()
        } else {
            CO2MassFlowIterator(inputStream.iterator())
        }

    }


    private class CO2MassFlowIterator(val inputIterator: Iterator<PCDFEvent>): Iterator<PCDFEvent> {
        var exhaustMassFlow: Double? = null
        var fuelRate: Double? = null

        var nextEvent: PCDFEvent? = null

        val canComputeNewValue: Boolean
            get() = exhaustMassFlow != null && fuelRate != null

        fun processEvent(event: PCDFEvent) {
            event.getComputedExhaustMassFlow()?.let { exhaustMassFlow = it }
            event.getComputedFuelRate()?.let { fuelRate = it }
        }

        fun computeNewValue(): Double {
            val cMol = ((fuelRate!! / 3600.0) * 832.0 * 0.861) / 12.011
            val oMol = 2 * cMol
            val co2Gram = cMol * 12.011 + oMol * 15.999
            val co2Ppm = if (exhaustMassFlow!! > 0.0) {
                co2Gram / (0.001517 * exhaustMassFlow!!)
            } else {
                0.0
            }
            return exhaustMassFlow!! * 0.001517 * co2Ppm
            // ToDo: This can be simplified!?
        }


        fun resetVariables() {
            exhaustMassFlow = null
            fuelRate = null
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
                    nextEvent = ComputedCO2MassFlowEvent(nextInputEvent.timestamp, newValue)
                    resetVariables()
                }
                return nextInputEvent
            }
        }


    }

    /**
     * Custom PCDF event to represent CO2 mass flow.
     * @property co2MassFlow: The CO2 mass flow in g/s.
     */
    class ComputedCO2MassFlowEvent(timestamp: NanoSeconds, val co2MassFlow: Double): PCDFEvent("CO2MassFlowComputation", EventType.CUSTOM, timestamp) {

    }
}