package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream


/**
 * Interface and abstract class for PCDF event stream transducers.
 * Stream transducers have an [inputStream] from which they compute an [outputStream].
 * Stream transducers may insert, modify or delete events from [inputStream].
 */
interface StreamTransducer {
    val inputStream: EventStream
    val outputStream: EventStream
}

/**
 * Abstract stream transducers represent the [outputStream] themselves.
 */
abstract class AbstractStreamTransducer(override val inputStream: EventStream) : StreamTransducer, EventStream {
    override val outputStream: EventStream
        get() = this
}