package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream

interface StreamTransducer {
    val inputStream: EventStream
    val outputStream: EventStream
}

abstract class AbstractStreamTransducer(override val inputStream: EventStream) : StreamTransducer, EventStream {
    override val outputStream: EventStream
        get() = this
}