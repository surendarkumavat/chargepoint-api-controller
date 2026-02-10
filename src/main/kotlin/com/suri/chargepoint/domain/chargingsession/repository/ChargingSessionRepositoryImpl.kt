package com.suri.chargepoint.domain.chargingsession.repository

import com.suri.chargepoint.domain.chargingsession.dao.ChargingSessionDAO
import com.suri.chargepoint.domain.chargingsession.dao.ChargingSessionTable
import com.suri.chargepoint.domain.chargingsession.dao.suspendTransaction
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import org.jetbrains.exposed.sql.and

class ChargingSessionRepositoryImpl: ChargingSessionRepository {
    override suspend fun sessionAuthRequestExists(session: ChargingSessionDto): Boolean = suspendTransaction {
        !ChargingSessionDAO.find {
            (ChargingSessionTable.stationId eq session.stationId) and
                    (ChargingSessionTable.driverId eq session.driverId) and
                    (ChargingSessionTable.callbackUrl eq session.callbackUrl) and
                    (ChargingSessionTable.status eq session.status)
        }.empty()
    }

    override suspend fun updateSessionStatus(session: ChargingSessionDto):Unit = suspendTransaction {
        ChargingSessionDAO.findByIdAndUpdate(session.correlationId) {
            it.status = session.status
        }
    }

    override suspend fun add(session: ChargingSessionDto):Unit = suspendTransaction {
        ChargingSessionDAO.new(session.correlationId) {
            this.stationId = session.stationId
            this.driverId = session.driverId
            this.callbackUrl = session.callbackUrl
            this.status = session.status
        }
    }
}