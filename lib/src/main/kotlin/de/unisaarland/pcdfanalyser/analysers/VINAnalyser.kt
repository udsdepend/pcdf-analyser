package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.VINEvent

/**
 * This analyser extracts the Vehicle Identification Number (VIN) of the car,
 * from which data was recorded into [eventStream].
 */
class VINAnalyser(eventStream: EventStream): Analyser<String?>(eventStream) {

    private var vin: String? = null
    private var prepared = false

    private fun findVehicleID(): String? {
        for (event in eventStream) {
            if (event is VINEvent) {
                return event.vin
            }
        }

        return null
    }

    /**
     * If the VIN is still unknown, scans [eventStream] for the VIN.
     */
    fun prepare() {
        if (!prepared) {
            prepared = true
            vin = findVehicleID()
        }
    }

    /**
     * Checks whether [prepare] is able to find the VIN.
     * @return true iff [analyse] returns a VIN.
     */
    override fun analysisIsAvailable(): Boolean {
        prepare()
        return vin != null
    }

    /**
     * Calls [prepare].
     * @return the VIN, if available, or null otherwise
     */
    override fun analyse(): String? {
        prepare()
        return vin
    }


}