package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.PCDFEvent

class StreamFilter(inputStream: EventStream, val predicate: (PCDFEvent) -> Boolean) : AbstractStreamTransducer(inputStream) {

    override fun iterator(): Iterator<PCDFEvent> {
        return StreamFilterIterator(inputStream.iterator(), predicate)
    }


    private class StreamFilterIterator(val inputIterator: Iterator<PCDFEvent>, val predicate: (PCDFEvent) -> Boolean) : Iterator<PCDFEvent> {

        var next: PCDFEvent? = findNext()

        private fun findNext(): PCDFEvent? {
            while (inputIterator.hasNext()) {
                val candidate = inputIterator.next()
                if (predicate(candidate)) {
                    return candidate
                }
            }

            return null
        }

        override fun hasNext(): Boolean {
            return next != null
        }

        override fun next(): PCDFEvent {
            if (next == null) {
                throw IllegalStateException("Iterator does not have next value!")
            } else {
                val result = next!!
                next = findNext()
                return result
            }
        }

    }
}

