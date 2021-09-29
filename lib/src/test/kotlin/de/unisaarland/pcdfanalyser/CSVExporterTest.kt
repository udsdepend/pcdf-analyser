package de.unisaarland.pcdfanalyser

import de.unisaarland.pcdfanalyser.analysers.CSVExporter
import de.unisaarland.pcdfanalyser.eventStream.FileEventStream
import org.junit.Test
import java.io.File

class CSVExporterTest {

    @Test
    fun testExport() {
        val inputStream = FileEventStream(File("../audi_new_rde1_2021_09_24.ppcdf").absoluteFile)
        val outputFile = File("../audi_new_rde1_2021_09_24.csv")

        val exporter = CSVExporter(inputStream, outputFile)
        exporter.analyse()
    }

}