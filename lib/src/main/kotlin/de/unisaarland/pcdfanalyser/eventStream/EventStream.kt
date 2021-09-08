package de.unisaarland.pcdfanalyser.eventStream

import pcdfEvent.PCDFEvent

/**
 * General interface to represent PCDF event streams as iterable objects.
 */
interface EventStream: Iterable<PCDFEvent> {

    /**
     * Iterator to iterate over two consecutive values. For example, a pair iterator over
     * a list [a,b,c,d] would have iteration steps (a,b), (b,c) and (c,d).
     */
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

