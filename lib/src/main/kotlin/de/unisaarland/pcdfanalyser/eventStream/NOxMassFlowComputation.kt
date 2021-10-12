package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.getComputedExhaustMassFlow
import de.unisaarland.pcdfanalyser.eventStream.getNOX
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent

/**
 * PCDF stream transducer to insert NOx mass flow in [inputStream].
 * Uses [ExhaustMassFlowComputation] (and [FuelRateComputation]) to compute the exhaust mass flow.
 * Computation is done according to the EU Commission Regulation 2017/1151, Appendix 4.
 */
class NOxMassFlowComputation(inputStream: EventStream, val noxReader: (PCDFEvent) -> Int? = {it.getNOX()}) : AbstractStreamTransducer(ExhaustMassFlowComputation(inputStream)) {
    private val inputStreamHasNOxEvents = inputStream.any { it is ComputedNOxMassFlowEvent }
    init {
        if (inputStreamHasNOxEvents)
        {
            println("Notice: NOx already computed.")
        }
    }

    override fun iterator(): Iterator<PCDFEvent> {
        return if (inputStreamHasNOxEvents) {
            inputStream.iterator()
        } else {
            NOxMassFlowIterator(inputStream.iterator(), noxReader)
        }
    }


    private class NOxMassFlowIterator(val inputIterator: Iterator<PCDFEvent>, val noxReader: (PCDFEvent) -> Int?): Iterator<PCDFEvent> {
        var exhaustMassFlow: Double? = null
        var noxPpm: Int? = null

        var nextEvent: PCDFEvent? = null

        val canComputeNewValue: Boolean
        get() = exhaustMassFlow != null && noxPpm != null

        fun processEvent(event: PCDFEvent) {
            event.getComputedExhaustMassFlow()?.let { exhaustMassFlow = it }
            //event.getNOX()?.let { noxPpm = it }
            noxReader(event)?.let { noxPpm = it }
        }

        fun computeNewValue(): Double {
            return exhaustMassFlow!! * 0.001586 * noxPpm!!
        }


        fun resetVariables() {
            exhaustMassFlow = null
            noxPpm = null
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
                    // ToDo: Make custom event ComputedNOxMassFlowEvent
                    nextEvent = ComputedNOxMassFlowEvent(nextInputEvent.timestamp, newValue)
                    resetVariables()
                }
                return nextInputEvent
            }
        }
    }

    /**
     * Custom PCDF event to represent NOx mass flow.
     * @property noxMassFlow: The NOx mass flow in g/s.
     */
    class ComputedNOxMassFlowEvent(timestamp: NanoSeconds, val noxMassFlow: Double): PCDFEvent("NOxMassFlowComputation", EventType.CUSTOM, timestamp) {

    }
}