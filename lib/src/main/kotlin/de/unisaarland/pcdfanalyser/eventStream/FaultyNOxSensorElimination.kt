package de.unisaarland.pcdfanalyser.eventStream

import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfUtilities.AbstractStreamTransducer

class FaultyNOxSensorElimination(inputStream: EventStream) : AbstractStreamTransducer(inputStream) {

    override fun iterator(): Iterator<PCDFEvent> {
        return FaultyNOxSensorEliminationIterator(inputStream)
    }

    class FaultyNOxSensorEliminationIterator(inputStream: EventStream): Iterator<PCDFEvent> {
        val inputIterator = inputStream.iterator()

        var noxSensor = NOxGroup<Int?>(null, null, null, null)
        var noxSensorCorrected = NOxGroup<Int?>(null, null, null, null)
        var noxSensorAlternative = NOxGroup<Int?>(null, null, null, null)
        var noxSensorAlternativeCorrected = NOxGroup<Int?>(null, null, null, null)

        var faultyNOxCount = NOxGroup(0,0,0,0)
        var totalNOxCount = NOxGroup(0,0,0,0)

        var faultyNOxCorrCount = NOxGroup(0,0,0,0)
        var totalNOxCorrCount = NOxGroup(0,0,0,0)

        var faultyNOxAltCount = NOxGroup(0,0,0,0)
        var totalNOxAltCount = NOxGroup(0,0,0,0)

        var faultyNOxAltCorrCount = NOxGroup(0,0,0,0)
        var totalNOxAltCorrCount = NOxGroup(0,0,0,0)

        override fun hasNext(): Boolean {
            val res = inputIterator.hasNext()
            if (!res) {

            }
            return res
        }

        private fun computeStatistics() {

        }

        //
        private fun updateNOxGroup(group: NOxGroup<Int?>, faultyCount: NOxGroup<Int>, totalCount: NOxGroup<Int>, s1_1: Int, s1_2: Int, s2_1: Int, s2_2: Int) {
            val NOX_MAX = 65535
            val faulty1_1 = s1_1 == -1 || s1_1 == NOX_MAX
            val faulty1_2 = s1_2 == -1 || s1_2 == NOX_MAX
            val faulty2_1 = s2_1 == -1 || s2_1 == NOX_MAX
            val faulty2_2 = s2_2 == -1 || s2_2 == NOX_MAX

            group.sensor1_1 = if (faulty1_1) group.sensor1_1 else s1_1
            group.sensor1_2 = if (faulty1_2) group.sensor1_2 else s1_2
            group.sensor2_1 = if (faulty2_1) group.sensor2_1 else s2_1
            group.sensor2_2 = if (faulty2_2) group.sensor2_2 else s2_2

            faultyCount.sensor1_1 += if (faulty1_1) 1 else 0
            faultyCount.sensor1_2 += if (faulty1_2) 1 else 0
            faultyCount.sensor2_1 += if (faulty2_1) 1 else 0
            faultyCount.sensor2_2 += if (faulty2_2) 1 else 0

            totalCount.sensor1_1 += 1
            totalCount.sensor1_2 += 1
            totalCount.sensor2_1 += 1
            totalCount.sensor2_2 += 1
        }

        override fun next(): PCDFEvent {
            val originalNext = inputIterator.next()
            when (originalNext) {
                is NOXSensorEvent -> {
                    val group = noxSensor
                    updateNOxGroup(group, faultyNOxCount, totalNOxCount, originalNext.sensor1_1, originalNext.sensor1_2, originalNext.sensor2_1, originalNext.sensor2_2)
                    val myNext = NOXSensorEvent(
                        originalNext.source,
                        originalNext.timestamp,
                        originalNext.bytes,
                        originalNext.pid,
                        originalNext.mode,
                        group.sensor1_1 ?: -1,
                        group.sensor1_2 ?: -1,
                        group.sensor2_1 ?: -1,
                        group.sensor2_2 ?: -1
                    )
                    return myNext
                }
                is NOXSensorAlternativeEvent -> {
                    val group = noxSensorAlternative
                    updateNOxGroup(group, faultyNOxAltCount, totalNOxAltCount, originalNext.sensor1_1, originalNext.sensor1_2, originalNext.sensor2_1, originalNext.sensor2_2)
                    val myNext = NOXSensorEvent(
                        originalNext.source,
                        originalNext.timestamp,
                        originalNext.bytes,
                        originalNext.pid,
                        originalNext.mode,
                        group.sensor1_1 ?: -1,
                        group.sensor1_2 ?: -1,
                        group.sensor2_1 ?: -1,
                        group.sensor2_2 ?: -1
                    )
                    return myNext
                }
                else -> {
                    // TODO: Add corrected NOX sensors
                    return originalNext
                }
            }
        }

        data class NOxGroup<V>(
            var sensor1_1: V,
            var sensor1_2: V,
            var sensor2_1: V,
            var sensor2_2: V)

    }

}