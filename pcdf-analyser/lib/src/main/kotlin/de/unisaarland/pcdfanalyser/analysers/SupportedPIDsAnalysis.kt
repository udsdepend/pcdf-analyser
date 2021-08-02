package de.unisaarland.pcdfanalyser.analysers

import de.unisaarland.pcdfanalyser.EventStream
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SupportedPidsEvent

typealias PID = Int

class SupportedPIDsAnalysis(eventStream: EventStream, val doConsistencyCheck: Boolean = false): CacheableAnalysis<SupportedPIDsAnalysis.AnalysisResult>(eventStream) {

    private var result: SupportedPIDsAnalysis.AnalysisResult? = null

    fun prepare() {
        if (result != null) {
            return
        }

        val supported = mutableListOf<PID>()
        val discovered = if (doConsistencyCheck) mutableSetOf<PID>() else null

        var timeout = 30
        for (event in eventStream) {
            when (event) {
                is SupportedPidsEvent -> {if (event.mode == 1) supported.addAll(event.supportedPids)}
                is OBDIntermediateEvent -> {
                    if (doConsistencyCheck) {
                        if (event.mode == 1) {
                            discovered!!.add(event.pid)
                        }
                    } else {
                        timeout--
                        if (timeout == 0)
                            break
                    }
                }
                else -> { /* no-op */ }
            }
        }

        val consistent: Boolean? = if (doConsistencyCheck) {
            supported.toSet().containsAll(discovered!!)
        } else {
            null
        }


        result = AnalysisResult(supported.sorted(), discovered?.toList()?.sorted(), consistent)
    }


    override fun analysisIsAvailable(): Boolean {
        return true
    }

    override fun analyse(): AnalysisResult {
        prepare()
        return result!!
    }


    companion object {
        const val analysisName = "SupportedPIDs"
    }


    class AnalysisResult(val supportedPIDs: List<PID>, val availablePIDs: List<PID>?, val consistent: Boolean?) {

        fun nameForPID(pid: PID): String {
            val command = OBDCommand.getCommand(1, pid)
            if (command != null) {
                return command.name
            } else {
                return "0x${pid.toString(16)}"
            }
        }

        override fun toString(): String {
            val supportedString = supportedPIDs.map {nameForPID(it)}
            val availableString = availablePIDs?.map {nameForPID(it)} ?: "not analysed"
            return "SupportedPIDs {supported=[$supportedString], available=[$availableString], consistent=$consistent}"
        }
    }
}