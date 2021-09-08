package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.EventType
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent

// `frequency` values per event type per second
// !! Works only for OBD events!!
/**
 * Class under construction, use with care!
 * PCDF event stream transducer to reduce the frequency of events (e.g., to get a stream with a frequency of 1Hz).
 */
class StreamFrequencyTransducer(inputStream: EventStream, val frequency: Int): AbstractStreamTransducer(inputStream) {

    override fun iterator(): Iterator<PCDFEvent> {
        return NewFrequencyIterator(inputStream.iterator(), frequency)
    }


    class NewFrequencyIterator(private val streamIterator: Iterator<PCDFEvent>, val frequency: Int): Iterator<PCDFEvent> {
        private var currentEvent = if (streamIterator.hasNext()) streamIterator.next() else null
        var currentTime = currentEvent?.timestamp?.nanoSecondsToSecondsTimestamp() ?: -1

        private val obdJar: MutableList<Pair<Int, Int>> = mutableListOf()
        // ToDo: Add analyser jar and filter out analyser events too

        override fun hasNext(): Boolean {
            return currentEvent != null
        }


        override fun next(): PCDFEvent {
            val res = currentEvent!!

            currentEvent = null
            while (streamIterator.hasNext()) {
                val nextEvent = streamIterator.next()
                val nextTimestamp = nextEvent.timestamp.nanoSecondsToSecondsTimestamp()
                if (nextTimestamp > currentTime) {
                    currentTime = nextTimestamp
                    obdJar.clear()
                }
                if (nextEvent.type == EventType.OBD_RESPONSE) {
                    if (nextEvent !is OBDIntermediateEvent) {
                        throw Error("StreamFrequencyTransducer does only support intermediate OBD events!")
                    }
                    val obdType = Pair(nextEvent.mode, nextEvent.pid)
                    if (!obdJar.contains(obdType)) {
                        currentEvent = nextEvent
                        obdJar.add(obdType)
                        break
                    }
                } else {
                    currentEvent = nextEvent
                    break
                }
            }

            return res
        }

    }

}