package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.model.ParameterID
import de.unisaarland.pcdfanalyser.model.ParameterSupport
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SupportedPidsEvent

/**
 * Scans [eventStream] to find all PIDs that are supported by the car,
 * and all PIDs that are available (have been logged) in [eventStream].
 */
class SupportedPIDsAnalyser(eventStream: EventStream): Analyser<ParameterSupport>(eventStream) {

    private var result: ParameterSupport? = null

    /**
     * Performs the actual analysis. The result is stored in an internal variable and can be accessed by [analyse].
     * If called twice, the analysis is not repeated.
     */
    fun prepare() {
        if (result != null) {
            return
        }

        val supported = mutableSetOf<ParameterID>()
        val available = mutableSetOf<ParameterID>()

        var timeout = 120
        for (event in eventStream) {
            when (event) {
                is SupportedPidsEvent -> supported.addAll(event.supportedPids.map { ParameterID(it, event.mode) })
                is OBDIntermediateEvent -> {
                    available.add(ParameterID(event.pid, event.mode))
                    timeout--
                    if (timeout == 0)
                        break
                }
                else -> { /* no-op */ }
            }
        }

        // Merge lists
        val supportedSorted = supported.sorted()
        val availableSorted = available.sorted()
        val supportedIterator = supportedSorted.iterator()
        val availableIterator = availableSorted.iterator()

        val records: MutableList<ParameterSupport.Record> = mutableListOf()

        var currentSupported: ParameterID? = if(supportedIterator.hasNext()) supportedIterator.next() else null
        var currentAvailable: ParameterID? = if(availableIterator.hasNext()) availableIterator.next() else null
        while (currentSupported != null || currentAvailable != null) {
            if (currentSupported == currentAvailable) {
                records.add(ParameterSupport.Record(currentSupported!!, true, true))
                currentSupported = if(supportedIterator.hasNext()) supportedIterator.next() else null
                currentAvailable = if(availableIterator.hasNext()) availableIterator.next() else null
            } else if (currentSupported == null) {
                records.add(ParameterSupport.Record(currentAvailable!!, false, true))
                currentAvailable = if(availableIterator.hasNext()) availableIterator.next() else null
            } else if (currentAvailable == null) {
                records.add(ParameterSupport.Record(currentSupported, true, false))
                currentSupported = if(supportedIterator.hasNext()) supportedIterator.next() else null
            } else if (currentSupported < currentAvailable) {
                records.add(ParameterSupport.Record(currentSupported, true, false))
                currentSupported = if(supportedIterator.hasNext()) supportedIterator.next() else null
            } else if (currentAvailable < currentSupported) {
                records.add(ParameterSupport.Record(currentAvailable, false, true))
                currentAvailable = if(availableIterator.hasNext()) availableIterator.next() else null
            } else {
                throw Error("This case should be unreachable...")
            }
        }


        result = ParameterSupport(records)
    }


    /**
     * This analysis is always available.
     * @return always true
     */
    override fun analysisIsAvailable(): Boolean {
        return true
    }

    /**
     * Calls [prepare]
     * @return the result of the analysis
     */
    override fun analyse(): ParameterSupport {
        prepare()
        return result!!
    }
}