package com.suri.chargepoint.apicontroller.domain.chargingsession.repository

import com.suri.chargepoint.apicontroller.domain.chargingsession.dto.ChargingSessionDto

interface ChargingSessionRepository {
    suspend fun sessionAuthRequestExists(session: ChargingSessionDto): Boolean
    suspend fun updateSessionStatus(session: ChargingSessionDto)
    suspend fun add(session: ChargingSessionDto)
}