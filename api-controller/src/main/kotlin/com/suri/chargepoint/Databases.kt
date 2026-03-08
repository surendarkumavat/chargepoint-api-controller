package com.suri.chargepoint

import com.suri.chargepoint.domain.chargingsession.dao.ChargingSessionTable
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )
    transaction {
        SchemaUtils.create(ChargingSessionTable) // add more tables here
        // SchemaUtils.createMissingTablesAndColumns(Users) // alternative for dev
    }
}
