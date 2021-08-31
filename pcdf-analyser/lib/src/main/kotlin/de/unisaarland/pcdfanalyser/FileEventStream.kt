package de.unisaarland.pcdfanalyser

import pcdfEvent.PCDFEvent
import java.io.File
import java.io.Reader
import java.nio.charset.Charset

class FileEventStream private constructor(private val reader: Reader, private val cacheEvents: Boolean = true): EventStream {
    // if cacheEvents is false, the stream can be iterated only once!

    constructor(reader: Reader): this(reader, true)
    constructor(file: File, charset: Charset = Charsets.UTF_8): this(file.bufferedReader(charset))

    private var eventCache: List<PCDFEvent> = listOf()
    private var parseError: Exception? = null

    private fun loadEvents() {
        if (eventCache.isNotEmpty()) {
            throw Error("Events have already been loaded.")
        }

        val lines = reader.readLines()
        eventCache = lines.map { PCDFEvent.fromString(it).toIntermediate() }
    }

    // This method may be called externally for performance reasons
    fun prepareStream() {
        // Ensure that the full record is loaded
        // (i.e., if an error occurs during parsing, rethrow it every time)
        if (this.parseError != null) {
            throw this.parseError!!
        }
        try {
            if (cacheEvents && eventCache.isEmpty()) {
                loadEvents()
            }
        } catch (e: Exception) {
            this.parseError = e
            throw e
        }

    }

    override fun iterator(): Iterator<PCDFEvent> {
        if (cacheEvents) {
            prepareStream()
            return eventCache.iterator()
        } else {
            TODO()
        }
    }


    override fun toString(): String {
        val details = if (eventCache.isEmpty()) {
            "data not loaded"
        } else {
            "${eventCache.size} events"
        }
        return "FileEventStream {$details}"
    }




    companion object {
        fun singleUseFileEventStream(reader: Reader): FileEventStream {
            return FileEventStream(reader, false)
        }
        fun singleUseFileEventStream(file: File, charset: Charset = Charsets.UTF_8): FileEventStream {
            return FileEventStream(file.bufferedReader(charset), false)
        }
    }

}