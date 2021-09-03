package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.VINEvent

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

    fun prepare() {
        if (!prepared) {
            prepared = true
            vin = findVehicleID()
        }
    }

    override fun analysisIsAvailable(): Boolean {
        prepare()
        return vin != null
    }

    override fun analyse(): String? {
        prepare()
        return vin
    }


}