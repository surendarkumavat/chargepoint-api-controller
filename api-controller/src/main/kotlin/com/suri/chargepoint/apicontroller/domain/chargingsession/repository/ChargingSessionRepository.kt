package com.suri.chargepoint.domain.chargingsession.repository

import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto

interface ChargingSessionRepository {
    suspend fun sessionAuthRequestExists(session: ChargingSessionDto): Boolean
    suspend fun updateSessionStatus(session: ChargingSessionDto)
    suspend fun add(session: ChargingSessionDto)
}