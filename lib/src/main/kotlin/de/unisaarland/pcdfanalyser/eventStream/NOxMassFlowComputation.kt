package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.getComputedExhaustMassFlow
import de.unisaarland.pcdfanalyser.eventStream.getNOX
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent

class NOxMassFlowComputation(inputStream: EventStream) : AbstractStreamTransducer(ExhaustMassFlowComputation(inputStream)) {
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
            NOxMassFlowIterator(inputStream.iterator())
        }
    }


    private class NOxMassFlowIterator(val inputIterator: Iterator<PCDFEvent>): Iterator<PCDFEvent> {
        var exhaustMassFlow: Double? = null
        var noxPpm: Int? = null

        var nextEvent: PCDFEvent? = null

        val canComputeNewValue: Boolean
        get() = exhaustMassFlow != null && noxPpm != null

        fun processEvent(event: PCDFEvent) {
            event.getComputedExhaustMassFlow()?.let { exhaustMassFlow = it }
            event.getNOX()?.let { noxPpm = it }
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

    class ComputedNOxMassFlowEvent(timestamp: NanoSeconds, val noxMassFlow: Double): PCDFEvent("NOxMassFlowComputation", EventType.CUSTOM, timestamp) {

    }
}