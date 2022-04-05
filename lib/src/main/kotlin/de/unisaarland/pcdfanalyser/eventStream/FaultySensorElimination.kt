package de.unisaarland.pcdfanalyser.eventStream

import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfUtilities.AbstractStreamTransducer

class FaultySensorElimination(inputStream: EventStream, val sensorVerdict: (PCDFEvent) -> FaultySensorEliminationIterator.SensorState) : AbstractStreamTransducer(inputStream) {

    override fun iterator(): Iterator<PCDFEvent> {
        return FaultySensorEliminationIterator(inputStream, sensorVerdict)
    }

    class FaultySensorEliminationIterator(inputStream: EventStream, val sensorVerdict: (PCDFEvent) -> FaultySensorEliminationIterator.SensorState): Iterator<PCDFEvent> {
        val inputIterator = inputStream.iterator()
        var nextValue = computeNext()

        // true, when the stream is currently observing faulty sensor values
        var faultySegment = false


        private fun computeNext(): PCDFEvent? {
            var next: PCDFEvent? = null
            while (next == null && inputIterator.hasNext()) {
                next = inputIterator.next()
                val verdict = sensorVerdict(next)
                if (verdict == SensorState.Faulty) {
                    // Faulty segment has started
                    faultySegment = true
                    next = null
                } else if (verdict == SensorState.Valid) {
                    // Faulty segmented has ended
                    faultySegment = false
                } else {
                    if (faultySegment) {
                        // We ignore all event during faulty segments
                        next = null
                    }
                }
            }

            return next
        }


        override fun hasNext(): Boolean {
            return nextValue != null
        }



        override fun next(): PCDFEvent {
            val res = nextValue!!
            nextValue = computeNext()
            return res
        }

        enum class SensorState {
            Faulty,
            Valid,
            Irrelevant
        }

    }

}