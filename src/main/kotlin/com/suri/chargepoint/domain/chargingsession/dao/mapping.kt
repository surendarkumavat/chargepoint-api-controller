package com.suri.chargepoint.domain.chargingsession.dao

import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object ChargingSessionTable : IdTable<UUID>("charging_session") {
    override val id = uuid("correlation_id").entityId()
    val stationId = uuid("station_id")
    val driverId = varchar("driver_id", 80)
    val callbackUrl = varchar("callback_url", 2048)
    val status = varchar("status", 50)

    override val primaryKey = PrimaryKey(id)
}

class ChargingSessionDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ChargingSessionDAO>(ChargingSessionTable)

    var stationId by ChargingSessionTable.stationId
    var driverId by ChargingSessionTable.driverId
    var callbackUrl by ChargingSessionTable.callbackUrl
    var status by ChargingSessionTable.status
}

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun daoToDto(dao: ChargingSessionDAO) = ChargingSessionDto(
    dao.id.value,
    dao.stationId,
    dao.driverId,
    dao.callbackUrl,
    dao.status
)