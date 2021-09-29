package de.unisaarland.pcdfanalyser.analysers

import com.github.doyaaaaaken.kotlincsv.dsl.context.WriteQuoteMode
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.unisaarland.pcdfanalyser.eventStream.EventStream
import de.unisaarland.pcdfanalyser.model.ParameterID
import pcdfEvent.PCDFEvent
import pcdfEvent.events.obdEvents.OBDCommand
import pcdfEvent.events.obdEvents.obdIntermediateEvents.OBDIntermediateEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorAlternativeEvent
import pcdfEvent.events.obdEvents.obdIntermediateEvents.multiComponentEvents.NOXSensorEvent
import pcdfUtilities.StreamFilter
import java.io.File

class CSVExporter(
    inputStream: EventStream,
    val outputFile: File,
    val restrictedOBDParameters: List<ParameterID>? = listOf(ParameterID(OBDCommand.NOX_SENSOR),ParameterID(OBDCommand.NOX_SENSOR_ALTERNATIVE))
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

            val columnNames = listOf("Timestamp") + collectColumnNames().toList()

            writer.open(outputFile) {
                writeRow(columnNames)
                for (row in rows) {
                    val timestamp = row.first
                    val data = row.second
                    val rowData = columnNames.map { if(it == "Timestamp") timestamp.toString() else data[it] ?: "" }
                    writeRow(rowData)
                }
            }


            return true
        } catch (e: Error) {
            return false
        }
    }

    private fun collectColumnNames(): Collection<String> {
        val res = mutableSetOf<String>()
        for (row in rows) {
            res.addAll(row.second.keys)
        }
        return res
    }

    private var rows: MutableList<Pair<Long, Map<String, String>>> = mutableListOf()



    private fun initialiseRows() {
        if (rows.isNotEmpty()) {
            println("Rows already initialised.")
        }

        val stream = if (restrictedOBDParameters == null) eventStream else StreamFilter(eventStream) { event ->
            event is OBDIntermediateEvent && ParameterID(
                event.pid,
                event.mode
            ) in restrictedOBDParameters
        }

        for (event in stream) {
            rows.add(Pair(event.timestamp, cellsForEvent(event)))
        }
    }



    private fun cellsForEvent(event: PCDFEvent): Map<String, String> {
        return when (event) {
            is OBDIntermediateEvent -> cellsForOBDIntermediateEvent(event)
            else -> mapOf()
        }
    }

    private fun cellsForOBDIntermediateEvent(event: OBDIntermediateEvent): Map<String, String> {
        return when (event) {
            is NOXSensorEvent -> mapOf(
                "NOx_Sensor_1_1" to (event as NOXSensorEvent).sensor1_1.toString(),
                "NOx_Sensor_1_2" to (event as NOXSensorEvent).sensor1_2.toString(),
                "NOx_Sensor_2_1" to (event as NOXSensorEvent).sensor2_1.toString(),
                "NOx_Sensor_2_2" to (event as NOXSensorEvent).sensor2_2.toString(),
            )
            is NOXSensorAlternativeEvent -> mapOf(
                "NOx_Sensor_Alt_1_1" to (event as NOXSensorAlternativeEvent).sensor1_1.toString(),
                "NOx_Sensor_Alt_1_2" to (event as NOXSensorAlternativeEvent).sensor1_2.toString(),
                "NOx_Sensor_Alt_2_1" to (event as NOXSensorAlternativeEvent).sensor2_1.toString(),
                "NOx_Sensor_Alt_2_2" to (event as NOXSensorAlternativeEvent).sensor2_2.toString(),
            )
            else -> {
                // ToDo: Add support for more sensors!!
                val colName = OBDCommand.getCommand(event.mode, event.pid)?.name ?: "OBD<0x${event.mode.toString(16)}, 0x${event.pid.toString(16)}>"
                return mapOf(colName to event.toString())
            }
        }
    }

}