package de.unisaarland.pcdfanalyser.eventStream

import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.*
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelAirEquivalenceRatioEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelRateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.MAFAirFlowRateEvent
import pcdfUtilities.ExhaustMassFlowComputation
import pcdfUtilities.FuelRateComputation

fun PCDFEvent.getNOX(): Int? {
    return when (this) {
        is NOXSensorEvent -> this.getNOX()
        is NOXSensorAlternativeEvent -> this.getNOX()
        is NOXSensorCorrectedEvent -> this.getNOX()
        is NOXSensorCorrectedAlternativeEvent -> this.getNOX()
        else -> null
    }
}

fun NOXSensorAlternativeEvent.getNOX(): Int? {
    val allValues = listOf(this.sensor1_1, this.sensor1_2, this.sensor2_1, this.sensor2_2)
    val availableValues = allValues.filter { it >= 0 }
    return availableValues.minOrNull()
}

fun NOXSensorCorrectedAlternativeEvent.getNOX(): Int? {
    val allValues = listOf(this.sensor1_1, this.sensor1_2, this.sensor2_1, this.sensor2_2)
    val availableValues = allValues.filter { it >= 0 }
    return availableValues.minOrNull()
}

fun NOXSensorCorrectedEvent.getNOX(): Int? {
    val allValues = listOf(this.sensor1_1, this.sensor1_2, this.sensor2_1, this.sensor2_2)
    val availableValues = allValues.filter { it >= 0 }
    return availableValues.minOrNull()
}

fun NOXSensorEvent.getNOX(): Int? {
    val allValues = listOf(this.sensor1_1, this.sensor1_2, this.sensor2_1, this.sensor2_2)
    val availableValues = allValues.filter { it >= 0 }
    return availableValues.minOrNull()
}


fun MAFSensorEvent.getMassAirFlow(): Double? {
    val values = listOf(mafSensorA, mafSensorB).filter { it >= 0.0 }
    return if (values.isEmpty()) {
        null
    } else {
        values.average()
    }
}

fun MAFAirFlowRateEvent.getMassAirFlow(): Double? {
    return if (this.rate >= 0.0) rate else null
}

fun PCDFEvent.getMassAirFlow(): Double? {
    return when(this) {
        is MAFSensorEvent -> this.getMassAirFlow()
        is MAFAirFlowRateEvent -> this.getMassAirFlow()
        else -> null
    }
}


fun FuelRateMultiEvent.getFuelRate(): Double? {
    return when {
        engineFuelRate >= 0.0 -> engineFuelRate  / 832.0 * 3600.0
        vehicleFuelRate >= 0.0 -> vehicleFuelRate  / 832.0 * 3600.0
        else -> null
    }
}

fun FuelRateEvent.getFuelRate(): Double? {
    return if (engineFuelRate >= 0.0) engineFuelRate else null
}

fun PCDFEvent.getFuelRate(): Double? {
    return when (this) {
        is FuelRateEvent -> getFuelRate()
        is FuelRateMultiEvent -> getFuelRate()
        else -> null
    }
}


fun PCDFEvent.getFuelAirEquivalenceRatio(): Double? {
    return if (this is FuelAirEquivalenceRatioEvent) {
        if (this.ratio >= 0.0) ratio else null
    } else {
        null
    }
}


//fun FuelRateComputation.ComputedFuelRateEventData.getComputedFuelRate(): Double? {
//    return this.fuelrate
//}
fun FuelRateComputation.ComputedFuelRateEvent.getComputedFuelRate(): Double? {
    return this.fuelrate
}

fun PCDFEvent.getComputedFuelRate(): Double? {
    if (this is FuelRateComputation.ComputedFuelRateEvent) {
        return this.getComputedFuelRate()
    } else {
        return null
    }
}

fun ExhaustMassFlowComputation.ExhaustMassFlowEvent.getComputedExhaustMassFlow(): Double? {
    return this.exhaustMassFlow
}

fun PCDFEvent.getComputedExhaustMassFlow(): Double? {
    return if (this is ExhaustMassFlowComputation.ExhaustMassFlowEvent) {
        this.getComputedExhaustMassFlow()
    } else {
        null
    }
}