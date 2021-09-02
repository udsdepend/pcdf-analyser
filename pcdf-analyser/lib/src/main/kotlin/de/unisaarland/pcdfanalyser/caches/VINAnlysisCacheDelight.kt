package de.unisaarland.pcdfanalyser.caches

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import de.unisaarland.caches.CacheDatabase
import org.jetbrains.exposed.sql.Database

class VINAnlysisCacheDelight {
    val driver: SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    init {
        CacheDatabase.Schema.create(driver)
        val database = CacheDatabase(driver)
        val queries = database.vINAnlysesQueries

    }
}