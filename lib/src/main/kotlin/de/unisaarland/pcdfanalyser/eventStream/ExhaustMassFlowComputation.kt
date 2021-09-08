package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.getComputedFuelRate
import de.unisaarland.pcdfanalyser.eventStream.getMassAirFlow
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent

/**
 * PCDF stream transducer to insert exhaust mass flow events in [inputStream] from mass air flow and fuel rate.
 * Uses [FuelRateComputation] to compute or conveniently access the fuel rate.
 * Computation is done according to the EU Commission Regulation 2017/1151, Appendix 4.
 */
class ExhaustMassFlowComputation(inputStream: EventStream) : AbstractStreamTransducer(FuelRateComputation(inputStream)) {
    private val inputStreamHasExhaustMassFlowEvents = inputStream.any { it is ExhaustMassFlowEvent }
    init {
        if (inputStreamHasExhaustMassFlowEvents) {
            println("Notice: Exhaust mass flow already computed.")
        }
    }

    override fun iterator(): Iterator<PCDFEvent> {
        return if (inputStreamHasExhaustMassFlowEvents) {
            // No more computation is necessary
            inputStream.iterator()
        } else {
            ExhaustMassFlowIterator(inputStream.iterator())
        }
    }


    private class ExhaustMassFlowIterator(val inputIterator: Iterator<PCDFEvent>): Iterator<PCDFEvent> {
        var fuelRate: Double? = null
        var massAirFlow: Double? = null

        var nextEvent: PCDFEvent? = null

        val canComputeNewValue: Boolean
        get() {
            return massAirFlow != null && fuelRate != null
        }

        var consecutiveMAFCounter = 0
        var consecutiveFuelCounter = 0
        fun processEvent(event: PCDFEvent) {
            event.getComputedFuelRate()?.let { fuelRate?.let { consecutiveFuelCounter++ }; fuelRate = it }
            event.getMassAirFlow()?.let { massAirFlow?.let { println(event.timestamp); consecutiveMAFCounter++ }; massAirFlow = it }
        }

        fun computeNewValue(): Double {
            val fuelMassFlow = fuelRate!! * 832.0 / 3600.0 / 1000.0
            return massAirFlow!! / 1000.0 + fuelMassFlow
        }

        fun resetVariables() {
            fuelRate = null
            massAirFlow = null
        }


        override fun hasNext(): Boolean {
            val res = nextEvent != null || inputIterator.hasNext()
//            if (!res) {
//                println("Consecutive values: MAF=$consecutiveMAFCounter, FUEL=$consecutiveFuelCounter")
//            }
            return res
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
                    nextEvent = ExhaustMassFlowEvent(nextInputEvent.timestamp, newValue)
//                    nextEvent = PCDFEvent("ExhaustMassFlowComputation", EventType.ANALYSER, nextInputEvent.timestamp, eventData)
                    resetVariables()
                }
                return nextInputEvent
            }
        }
    }

    /**
     * Custom PCDF event to represent exhaust mass flow.
     * @property exhaustMassFlow: The exhaust mass flow in g/s.
     */
    class ExhaustMassFlowEvent(timestamp: NanoSeconds, val exhaustMassFlow: Double): PCDFEvent("ExhaustMassFlowComputation", EventType.CUSTOM, timestamp) {

    }

}

