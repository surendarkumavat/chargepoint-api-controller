package com.suri.chargepoint.domain.chargingsession.service

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository

internal class ChargingSessionService(
    private val chargingSessionRepository: ChargingSessionRepository,
    private val authorizeChargingSessionApi: AuthorizeChargingSessionApi
) {
    suspend fun initiateSession(dto: ChargingSessionDto) {
        if (!chargingSessionRepository.sessionAuthRequestExists(dto)) {
            dto.status =
                try {
                    authorizeChargingSessionApi.chargingSessionsPost(
                        ChargingSessionsPostRequest(
                            dto.stationId.toString(),
                            dto.driverId,
                            dto.callbackUrl
                        )
                    ).body().status?.toString() ?: "unknown"
                } catch (e: Exception) {
                    "unknown"
                }
            chargingSessionRepository.updateSessionStatus(dto)
        }
    }
}