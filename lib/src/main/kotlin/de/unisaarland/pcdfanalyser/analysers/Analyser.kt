package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream


/**
 * Abstract class from which concrete analysers must inherit
 *
 * @property eventStream: The PCDF event stream that shall be analysed
 */
abstract class Analyser<V>(val eventStream: EventStream) {

    /**
     * Checks if the analysis is available for [eventStream].
     * Subclasses may always return true, but returning null after [analyse] if the analysis is not available
     * @return false iff [analyse] should not be called
     */
    abstract fun analysisIsAvailable(): Boolean


    /**
     * Runs the analysis
     * @return The result of the analysis
     */
    abstract fun analyse(): V

}
