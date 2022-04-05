package de.unisaarland.pcdfanalyser

import de.unisaarland.pcdfanalyser.analysers.CSVExporter
import de.unisaarland.pcdfanalyser.analysers.SimResult
import de.unisaarland.pcdfanalyser.analysers.TripTypeAnalyser
import de.unisaarland.pcdfanalyser.eventStream.FaultySensorElimination
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import org.junit.Test
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.AftertreatmentStatus
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelRateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SpeedEvent
import pcdfUtilities.CO2MassFlowComputation
import pcdfUtilities.NOX_MAX
import de.unisaarland.pcdfanalyser.eventStream.NOxMassFlowComputation
import pcdfUtilities.StreamFilter
import java.io.File

class CSVExporterTest {

    @Test
    fun testExport1() {
        val inputStream = FileEventStream(File("../audi_new_rde1_2021_09_24.ppcdf").absoluteFile)
        val outputFile = File("../audi_new_rde1_2021_09_24.csv")

        val exporter = CSVExporter(inputStream, outputFile)
        exporter.analyse()
    }

    // audi_a6new_trip3.ppcdf
    @Test
    fun testExport2() {
        val inputStream = FileEventStream(File("../audi_a6new_trip3.ppcdf").absoluteFile)
        val outputFile = File("../audi_a6new_trip3.csv")

        val unfaulty = FaultySensorElimination(inputStream) { event ->
            if (event is NOXSensorAlternativeEvent) {
                if (event.sensor1_1 < 0 || event.sensor1_1 == NOX_MAX) {
                    FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Faulty
                } else {
                    FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Valid
                }
            } else {
                FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Irrelevant
            }
        }

        val withNOx = NOxMassFlowComputation(unfaulty) { event ->
            if (event is NOXSensorAlternativeEvent) {
                if (event.sensor1_1 >= 0 && event.sensor1_1 != NOX_MAX) {
                    event.sensor1_1
                } else {
                    null
                }
            } else {
                null
            }
        }

        val valuesOfInterest = StreamFilter(withNOx) {event ->
            event is NOxMassFlowComputation.ComputedNOxMassFlowEvent || event is CO2MassFlowComputation.ComputedCO2MassFlowEvent ||
                    event is SpeedEvent || event is NOXSensorAlternativeEvent || event is FuelRateEvent || event is AftertreatmentStatus
        }

        val exporter = CSVExporter(valuesOfInterest, outputFile)
        exporter.analyse()
    }


    // audi_a6new_trip4.ppcdf
    @Test
    fun testExport3() {
        val inputStream = FileEventStream(File("../audi_a6new_trip4.ppcdf").absoluteFile)
        val outputFile = File("../audi_a6new_trip4.csv")

//        val unfaulty = FaultySensorElimination(inputStream) { event ->
//            if (event is NOXSensorAlternativeEvent) {
//                if (event.sensor1_1 < 0 || event.sensor1_1 == NOX_MAX) {
//                    FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Faulty
//                } else {
//                    FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Valid
//                }
//            } else {
//                FaultySensorElimination.FaultySensorEliminationIterator.SensorState.Irrelevant
//            }
//        }

        val withNOx = NOxMassFlowComputation(inputStream) { event ->
            if (event is NOXSensorAlternativeEvent) {
                if (event.sensor1_1 >= 0 && event.sensor1_1 != NOX_MAX) {
                    event.sensor1_1
                } else {
                    null
                }
            } else {
                null
            }
        }

        val valuesOfInterest = StreamFilter(withNOx) {event ->
            event is NOxMassFlowComputation.ComputedNOxMassFlowEvent ||
                    event is SpeedEvent || event is NOXSensorAlternativeEvent  || event is NOXSensorEvent || event is AftertreatmentStatus
        }

        val exporter = CSVExporter(valuesOfInterest, outputFile)
        exporter.analyse()
    }

    @Test
    fun testDetection() {
        val inputStream = FileEventStream(File("../audi_a6new_trip4.ppcdf").absoluteFile)
        val detector = TripTypeAnalyser(inputStream)

        assert(detector.analyse() == SimResult.RDE)

    }
}