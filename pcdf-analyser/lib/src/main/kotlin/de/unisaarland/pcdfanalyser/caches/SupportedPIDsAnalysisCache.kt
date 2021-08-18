package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.SupportedPIDsAnalysis
import de.unisaarland.pcdfanalyser.model.ParameterID
import de.unisaarland.pcdfanalyser.model.ParameterSupport
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

class SupportedPIDsAnalysisCache(val database: Database, val analysisCacheDelegate: AnalysisCacheDelegate<ParameterSupport> = AnalysisCacheDelegate { SupportedPIDsAnalysis(it) }): AnalysisCache<ParameterSupport>() {

    object SupportedPIDsAnalyses: Table() {
        val fileName: Column<String> = varchar("fileName", 1024)
        val mode: Column<Int> = integer("mode")
        val pid: Column<Int> = integer("PID")
        val isSupported: Column<Boolean> = bool("isSupported")
        val isAvailable: Column<Boolean> = bool("isAvailable")
        val analyserVersion: Column<Int> = integer("analyserVersion")
        override val primaryKey = PrimaryKey(fileName, mode, pid, name = "PK_SupportedPIDsAnalyses")
    }

    private fun enableLogging(t: Transaction) {
        t.addLogger(StdOutSqlLogger)
    }

    init {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            enableLogging(this)
            SchemaUtils.create(SupportedPIDsAnalyses)
        }
    }


    private fun fetchAnalysisResult(pcdfFile: File): ParameterSupport? {
        val records = mutableListOf<ParameterSupport.Record>()
        transaction(database) {
            enableLogging(this)
            SupportedPIDsAnalyses.select { SupportedPIDsAnalyses.fileName eq pcdfFile.absolutePath }.forEach {
                val parameterID = ParameterID(it[SupportedPIDsAnalyses.pid], it[SupportedPIDsAnalyses.mode])
                val supported = it[SupportedPIDsAnalyses.isSupported]
                val available = it[SupportedPIDsAnalyses.isAvailable]
                records.add(ParameterSupport.Record(parameterID, supported, available))
            }
        }

        return if (records.isEmpty()) {
            null
        } else {
            ParameterSupport(records)
        }


    }


    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return transaction(database) {
            enableLogging(this)
            SupportedPIDsAnalyses.select { SupportedPIDsAnalyses.fileName eq pcdfFile.absolutePath }.count() > 0
        }
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): ParameterSupport? {
        return fetchAnalysisResult(pcdfFile)
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: ParameterSupport) {
        transaction(database) {
            enableLogging(this)
            for (record in result.parameterRecords) {
                SupportedPIDsAnalyses.insert {
                    it[fileName] = pcdfFile.absolutePath
                    it[pid] = record.parameterID.id
                    it[mode] = record.parameterID.mode
                    it[isSupported] = record.supported
                    it[isAvailable] = record.available
                    it[analyserVersion] = VERSION
                }
            }

        }
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): ParameterSupport {
        val fetchResult = fetchAnalysisResult(pcdfFile)
        return if (fetchResult == null) {
            val analyser = analysisCacheDelegate.analyserForEventStream(FileEventStream(pcdfFile))
            val result = analyser.analyse()
            if (cacheResult) {
                addAnalysisResultToCache(pcdfFile, result)
            }
            result
        } else {
            fetchResult
        }
    }



    companion object {
        const val VERSION: Int = 1
    }


}