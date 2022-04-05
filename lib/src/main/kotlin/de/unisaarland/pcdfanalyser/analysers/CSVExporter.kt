package de.unisaarland.pcdfanalyser.analysers

import com.github.doyaaaaaken.kotlincsv.dsl.context.WriteQuoteMode
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.eventStream.NOxMassFlowComputation
import de.unisaarland.pcdfanalyser.model.ParameterID
import pcdfEvent.PCDFEvent
import pcdfEvent.events.AnalyserEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.AftertreatmentStatus
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.FuelRateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.singleComponentEvents.SpeedEvent
import pcdfUtilities.StreamFilter
import java.io.File

class CSVExporter(
    inputStream: EventStream,
    val outputFile: File

): Analyser<Boolean>(inputStream) {

    override fun analysisIsAvailable(): Boolean {
        return true
    }

    override fun analyse(): Boolean {
        try {
            initialiseRows()

            val writer = csvWriter {
                charset = "UTF-8"
                delimiter = ';'
                nullCode = "NULL"
                lineTerminator = "\n"
                outputLastLineTerminator = true
                quote {
                    mode = WriteQuoteMode.CANONICAL
                    char = '\''
                }
            }

            val columnNames = listOf("Timestamp [ns]") + collectColumnNames().toList()

            writer.open(outputFile) {
                writeRow(columnNames)
                for (row in rows) {
                    val timestamp = row.timestamp - rows[0].timestamp
                    val rowData = columnNames.map { if(it == "Timestamp [ns]") timestamp.toString() else row[it] ?: "" }
                    writeRow(rowData)
                }
            }


            return true
        } catch (e: Error) {
            return false
        }
    }

    private fun collectColumnNames(): Collection<String> {
        return rows.map { row -> row.columnNames }.flatten().toSet()
    }

    private var rows: MutableList<Row> = mutableListOf()



    private fun initialiseRows() {
        if (rows.isNotEmpty()) {
            println("Rows already initialised.")
        }

        for (event in eventStream) {
            rows.add(Row(event.timestamp, cellsForEvent(event)))
        }
    }



    private fun cellsForEvent(event: PCDFEvent): List<Cell> {
        return when (event) {
            is OBDIntermediateEvent -> cellsForOBDIntermediateEvent(event)
            is AnalyserEvent -> cellForAnalyserEvent(event)
            is NOxMassFlowComputation.ComputedNOxMassFlowEvent -> listOf(Cell("Computed absolute NOx [g/s]", event.noxMassFlow))
            else -> listOf(Cell(event.javaClass.name, event.toString()))
        }
    }

    private fun cellsForOBDIntermediateEvent(event: OBDIntermediateEvent): List<Cell> {
        return when (event) {
            is NOXSensorEvent -> listOf(
                Cell("NOx_Sensor_1_1", (event as NOXSensorEvent).sensor1_1.toString()),
                Cell("NOx_Sensor_1_2", (event as NOXSensorEvent).sensor1_2.toString()),
                Cell("NOx_Sensor_2_1", (event as NOXSensorEvent).sensor2_1.toString()),
                Cell("NOx_Sensor_2_2", (event as NOXSensorEvent).sensor2_2.toString())
            )
            is NOXSensorAlternativeEvent -> listOf(
                Cell("NOx_Sensor_Alt_1_1", (event as NOXSensorAlternativeEvent).sensor1_1.toString()),
                Cell("NOx_Sensor_Alt_1_2", (event as NOXSensorAlternativeEvent).sensor1_2.toString()),
                Cell("NOx_Sensor_Alt_2_1", (event as NOXSensorAlternativeEvent).sensor2_1.toString()),
                Cell("NOx_Sensor_Alt_2_2", (event as NOXSensorAlternativeEvent).sensor2_2.toString()),
            )
            is AftertreatmentStatus -> {
                val res = mutableListOf<Cell>()
                event.noxAdsorberRegenInProgress?.let { res.add(Cell("noxAdsorberRegenInProgress", it)) }
                event.noxAdsorberDesulfurizationInProgress?.let { res.add(Cell("noxAdsorberDesulfurizationInProgress", it)) }
                event.particulateFilterRegenInProgress?.let { res.add(Cell("particulateFilterRegenInProgress", it)) }
                event.particulateFilterActiveRegen?.let { res.add(Cell("particulateFilterActiveRegen", it)) }
                event.normalizedTriggerForPFRegen?.let { res.add(Cell("normalizedTriggerForPFRegen [%]", it)) }
                event.averageDistanceBetweenPFRegens?.let { res.add(Cell("averageDistanceBetweenPFRegens [km]", it)) }
                event.averageTimeBetweenPFRegens?.let { res.add(Cell("averageTimeBetweenPFRegens [min]", it)) }
                res
            }
            is SpeedEvent -> listOf(Cell("Speed [km/h]", event.speed))
            is FuelRateEvent -> listOf(Cell("Fuel Rate", event.engineFuelRate))
            else -> {
                // ToDo: Add support for more sensors!!
                val colName = OBDCommand.getCommand(event.mode, event.pid)?.name ?: "OBD<0x${event.mode.toString(16)}, 0x${event.pid.toString(16)}, ${event.javaClass.name}>"
                return listOf(Cell(colName, event.toString()))
            }
        }
    }

    private fun cellForAnalyserEvent(event: AnalyserEvent): List<Cell> {
        val result = mutableListOf<Cell>()
        event.nox_concentration?.let { result.add(Cell("NOx Concentration", it)) }
        event.co2_concentration?.let { result.add(Cell("CO2 Concentration", it)) }
        // ToDo: More on demand
        return result
    }



    class Cell(val columnName: String, val value: Any)
//    private operator fun Collection<Cell>.get(key: String): Any? {
//        for (cell in this) {
//            if (cell.columnName == key) {
//                return cell.value
//            }
//        }
//        return null
//    }

    class Row(val timestamp: Long, val cells: Collection<Cell>) {
        operator fun get(key: String): Any? {
            if (key == "Timestamp [ns]") {
                return timestamp
            } else {
                for (cell in cells) {
                    if (cell.columnName == key) {
                        return cell.value
                    }
                }
                return null
            }
        }

        val columnNames: List<String>
        get() = cells.map { it.columnName }
    }

}


