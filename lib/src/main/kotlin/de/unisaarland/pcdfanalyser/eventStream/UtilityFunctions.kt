package pcdfUtilities

import de.unisaarland.pcdfanalyser.eventStream.EventStream
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SpeedEvent



val NOX_MAX = 65535

/**
 * Auxiliary types and functions to handle time.
 */
typealias NanoSeconds = Long
typealias Seconds = Double
fun nanoSecondsToSeconds(nanoSeconds: NanoSeconds): Seconds {
    return nanoSeconds / 1E9
}
fun Long.nanoSecondsToSecondsTimestamp(): Int {
    return (this / 1_000_000_000).toInt()
}

/**
 * Computes the average absolute amount of NOx emitted in [noxStream].
 * [noxStream] must contain [NOxMassFlowComputation.ComputedNOxMassFlowEvent] events and [SpeedEvent] events.
 * @return Average absolute amount of NOx in mg/km, or null if [noxStream] is incompatible.
 */
fun computeNOxMGPerKM(noxStream: EventStream): Double? {
    val onlyNOxStream = StreamFilter(noxStream) { event -> event is NOxMassFlowComputation.ComputedNOxMassFlowEvent }
    val noxPairs = onlyNOxStream.pairIterator()
    if (!noxPairs.hasNext()) {
        // ToDo: This computation is incorrect if NOx values are available only at the beginning (or end) of the stream
        return null
    }

    var noxSum = 0.0
    for ((firstEvent, secondEvent) in noxPairs) {
        val timeDiff = nanoSecondsToSeconds(secondEvent.timestamp - firstEvent.timestamp)
        val relativeNOx = ((firstEvent as NOxMassFlowComputation.ComputedNOxMassFlowEvent).noxMassFlow + (secondEvent as NOxMassFlowComputation.ComputedNOxMassFlowEvent).noxMassFlow) / 2.0
        //println(relativeNOx)
        var totalNOx = relativeNOx * timeDiff
        noxSum += totalNOx
    }

    val onlySpeedStream = StreamFilter(noxStream) { it is SpeedEvent }
    val speedPairs = onlySpeedStream.pairIterator()
    if (!speedPairs.hasNext()) {
        // ToDo: This computation is incorrect if speed values are available only at the beginning (or end) of the stream
        return null
    }

    var distanceSum = 0.0
    for ((firstEvent, secondEvent) in speedPairs) {
        val timeDiff = nanoSecondsToSeconds(secondEvent.timestamp - firstEvent.timestamp)
        val avgSpeed =
            ((firstEvent as SpeedEvent).speed + (secondEvent as SpeedEvent).speed) / 2.0
        val distance = avgSpeed / 3.6 * timeDiff    // in m
        distanceSum += distance
    }
    distanceSum /= 1000.0   // in km

    //println("Nox: $noxSum, Distance: $distanceSum")

    return noxSum * 1000.0 / distanceSum
}

/**
 * Computes the average absolute amount of NOx emitted in [co2Stream].
 * [co2Stream] must contain [CO2MassFlowComputation.ComputedCO2MassFlowEvent] events and [SpeedEvent] events.
 * @return Average absolute amount of NOx in g/km, or null if [co2Stream] is incompatible.
 */
fun computeCO2MGPerKM(co2Stream: EventStream): Double? {
    val onlyCO2Stream = StreamFilter(co2Stream) { event ->
        event is CO2MassFlowComputation.ComputedCO2MassFlowEvent }
    val co2Pairs = onlyCO2Stream.pairIterator()
    if (!co2Pairs.hasNext()) {
        // ToDo: This computation is incorrect if CO2 values are available only at the beginning (or end) of the stream
        return null
    }

    var co2Sum = 0.0
    for ((firstEvent, secondEvent) in co2Pairs) {
        val timeDiff = nanoSecondsToSeconds(secondEvent.timestamp - firstEvent.timestamp)
        val relativeCO2 = ((firstEvent as CO2MassFlowComputation.ComputedCO2MassFlowEvent).co2MassFlow + (secondEvent as CO2MassFlowComputation.ComputedCO2MassFlowEvent).co2MassFlow) / 2.0
        //println(relativeNOx)
        var totalCO2 = relativeCO2 * timeDiff
        co2Sum += totalCO2
    }

    val onlySpeedStream = StreamFilter(co2Stream) { it is SpeedEvent }
    val speedPairs = onlySpeedStream.pairIterator()
    if (!speedPairs.hasNext()) {
        // ToDo: This computation is incorrect if speed values are available only at the beginning (or end) of the stream
        return null
    }

    var distanceSum = 0.0
    for ((firstEvent, secondEvent) in speedPairs) {
        val timeDiff = nanoSecondsToSeconds(secondEvent.timestamp - firstEvent.timestamp)
        val avgSpeed =
            ((firstEvent as SpeedEvent).speed + (secondEvent as SpeedEvent).speed) / 2.0
        val distance = avgSpeed / 3.6 * timeDiff    // in m
        distanceSum += distance
    }
    distanceSum /= 1000.0   // in km

    //println("CO2: $co2Sum, Distance: $distanceSum")

    return co2Sum / distanceSum
}







