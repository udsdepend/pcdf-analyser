package de.unisaarland.pcdfanalyser.eventStream

import pcdfEvent.PCDFEvent

interface EventStream: Iterable<PCDFEvent> {

    fun pairIterator(): PairIterator {
        return PairIterator(this)
    }


    class PairIterator(private val stream: EventStream): Iterator<PairIterator.EventPair> {
        private val valueIterator = stream.iterator()
        var previousEvent = if (valueIterator.hasNext()) valueIterator.next() else null

        override fun hasNext(): Boolean {
            return valueIterator.hasNext()
        }

        override fun next(): EventPair {
            val result = EventPair(previousEvent!!, valueIterator.next())
            previousEvent = result.second
            return result
        }


        data class EventPair(
            val first: PCDFEvent,
            val second: PCDFEvent
        )
    }
}

