package de.unisaarland.pcdfanalyser.caches

import de.unisaarland.pcdfanalyser.FileEventStream
import de.unisaarland.pcdfanalyser.analysers.VINAnalysis
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Connection

class VINAnalysisCache(val database: Database): AnalysisCache<String>() {

    constructor(cacheFile: File): this(Database.connect("jdbc:sqlite:${cacheFile.absolutePath}", "org.sqlite.JDBC"))

    object VINAnalyses: Table() {
        val fileName: Column<String> = varchar("fileName", 1024)
        val analysisResult: Column<String?> = varchar("VIN", 17).nullable()
        val analyserVersion: Column<Int> = integer("analyserVersion")
        override val primaryKey = PrimaryKey(fileName, name = "PK_VINAnalyses")
    }

    private fun enableLogging(t: Transaction) {
        t.addLogger(StdOutSqlLogger)
    }

    init {
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        transaction(database) {
            enableLogging(this)
            SchemaUtils.create(VINAnalyses)
        }
    }


    private fun fetchAnalysisResult(pcdfFile: File): Pair<Boolean, String?> {
        var result: Pair<Boolean, String?>? = null
        transaction(database) {
            enableLogging(this)
            VINAnalyses.select { VINAnalyses.fileName eq pcdfFile.absolutePath }.forEach {
                result = Pair(
                    true,
                    it[VINAnalyses.analysisResult]
                ) // result could still be null if no VIN record is in PCDF file
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
            VINAnalyses.select { VINAnalyses.fileName eq pcdfFile.absolutePath }.count() > 0
        }
    }

    override fun cachedAnalysisResultForFile(pcdfFile: File): String? {
        return fetchAnalysisResult(pcdfFile).second
    }

    private fun addAnalysisResultToCache(pcdfFile: File, result: String?) {
        transaction(database) {
            enableLogging(this)
            VINAnalyses.insert {
                it[fileName] = pcdfFile.absolutePath
                it[analysisResult] = result
                it[analyserVersion] = VERSION
            }
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