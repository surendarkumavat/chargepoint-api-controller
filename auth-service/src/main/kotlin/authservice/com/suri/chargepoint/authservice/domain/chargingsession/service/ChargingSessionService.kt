package com.suri.chargepoint.domain.chargingsession.service

import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker

internal class ChargingSessionService(
    private val repo: ChargingSessionRepository,
    private val worker: AsyncAuthServiceWorker
) {
    suspend fun initiateSession(dto: ChargingSessionDto) {
        if (!repo.sessionAuthRequestExists(dto)) {
            repo.add(dto)
            worker.enqueueChargingSession(dto)
        }
    }
}