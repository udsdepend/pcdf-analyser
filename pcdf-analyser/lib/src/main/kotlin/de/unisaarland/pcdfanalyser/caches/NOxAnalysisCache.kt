package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.NOxAnalyser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

class NOxAnalysisCache(val database: Database, val analysisCacheDelegate: AnalysisCacheDelegate<Double?> = AnalysisCacheDelegate { NOxAnalyser(it) }): AnalysisCache<Double?>() {

    object NOxAnalyses: Table() {
        val fileName: Column<String> = varchar("fileName", 1024)
        val analysisResult: Column<Double?> = double("nox").nullable()
        val analyserVersion: Column<Int> = integer("analyserVersion")
        override val primaryKey = PrimaryKey(fileName, name = "PK_NOxAnalyses")
    }

    private fun enableLogging(t: Transaction) {
        t.addLogger(StdOutSqlLogger)
    }

    init {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            enableLogging(this)
            SchemaUtils.create(NOxAnalyses)
        }
    }


    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, Double?> {
        var result: Pair<Boolean, Double?>? = null
        transaction(database) {
            enableLogging(this)
            NOxAnalyses.select { NOxAnalyses.fileName eq pcdfFile.absolutePath }.forEach {
                result = Pair(
                    true,
                    it[NOxAnalyses.analysisResult]
                ) // result could still be null if no NOx records are in PCDF file
            }
        }

        return if (result == null) {
            Pair(false, null)
        } else {
            result!!
        }


    }

    override fun hasAnalysisResultForFile(pcdfFile: File): Boolean {
        return transaction(database) {
            enableLogging(this)
            NOxAnalyses.select { NOxAnalyses.fileName eq pcdfFile.absolutePath }.count() > 0
        }
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): Double? {
        return fetchAnalysisResult(pcdfFile).second
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: Double?) {
        transaction(database) {
            enableLogging(this)
            NOxAnalyses.insert {
                it[fileName] = pcdfFile.absolutePath
                it[analysisResult] = result
                it[analyserVersion] = VERSION
            }
        }
    }

    override fun analysisResultForFile(pcdfFile: File, cacheResult: Boolean): Double? {
        val fetchResult = fetchAnalysisResult(pcdfFile)
        return if (!fetchResult.first) {
            val analyser = analysisCacheDelegate.analyserForEventStream(FileEventStream(pcdfFile))
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