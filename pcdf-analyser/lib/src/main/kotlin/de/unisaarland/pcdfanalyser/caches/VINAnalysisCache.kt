package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.VINAnalysis
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.sql.Connection

class VINAnalysisCache(val cacheFile: File): AnalysisCache<String>() {

    object VINAnalyses: Table() {
        val fileName: Column<String> = varchar("fileName", 1024)
        val analysisResult: Column<String?> = varchar("VIN", 16).nullable()
        val analyserVersion: Column<Int> = integer("analyserVersion")
        override val primaryKey = PrimaryKey(fileName, name = "PK_VINAnalysis")
    }

    private val database: Database = Database.connect("jdbc:sqlite:${cacheFile.absolutePath}", "org.sqlite.JDBC")

    init {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    }


    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, String?> {
        VINAnalyses.select { VINAnalyses.fileName eq pcdfFile.absolutePath }.forEach {
            return Pair(true, it[VINAnalyses.analysisResult]) // result could still be null if no VIN record is in PCDF file
        }

        return Pair(false, null)
    }

    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return VINAnalyses.select { VINAnalyses.fileName eq pcdfFile.absolutePath }.count() > 0
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): String? {
        return fetchAnalysisResult(pcdfFile).second
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: String?) {
        VINAnalyses.insert {
            it[fileName] = pcdfFile.absolutePath
            it[analysisResult] = result
            it[analyserVersion] = VERSION
        }
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): String? {
        val fetchResult = fetchAnalysisResult(pcdfFile)
        return if (!fetchResult.first) {
            val analyser = VINAnalysis(FileEventStream(pcdfFile))
            val result = analyser.analyse()
            if (cacheResult) {
                addAnalysisResultToCache(pcdfFile, result)
            }
            result
        } else {
            fetchResult.second
        }
    }



    companion object {
        const val VERSION: Int = 1
    }

}