package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.analysers.SimResult.*
import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.events.GPSEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SpeedEvent

class TripTypeAnalyser(eventStream: EventStream): Analyser<SimResult?>(eventStream) {
    private var result: SimResult? = null
    private var prepared = false

    private fun detect(): SimResult {
        var gpsSpeed = 0.0
        var gpsCount = 0
        var obdSpeed = 0.0
        var obdCount = 0
        for(event in eventStream) {
            when (event) {
                is SpeedEvent -> {
                    obdSpeed += event.speed
                    obdCount += 1
                }
                is GPSEvent -> {
                    gpsSpeed += event.speed?.times(3.6) ?: 0.0
                    gpsCount += 1
                }
            }
        }

        if (gpsCount == 0 || obdCount == 0 || obdSpeed == 0.0) return MONITORING

        val avgGPSSpeed = gpsSpeed / gpsCount
        val avgOBDSpeed = obdSpeed / obdCount
        return if (avgGPSSpeed / avgOBDSpeed < 0.8) {
            SIMULATOR
        } else {
            RDE
        }
    }

    /**
     * If the VIN is still unknown, scans [eventStream] for the VIN.
     */
    private fun prepare() {
        if (!prepared) {
            prepared = true
            result = detect()
        }
    }

    /**
     * Checks whether [prepare] is able to find the VIN.
     * @return true iff [analyse] returns a VIN.
     */
    override fun analysisIsAvailable(): Boolean {
        prepare()
        return result != null
    }

    /**
     * Calls [prepare].
     * @return the VIN, if available, or null otherwise
     */
    override fun analyse(): SimResult? {
        prepare()
        return result
    }

}

enum class SimResult {
    SIMULATOR,
    RDE,
    MONITORING
}