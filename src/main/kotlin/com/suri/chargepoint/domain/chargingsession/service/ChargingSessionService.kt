package com.suri.chargepoint.domain.chargingsession.service

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository

internal class ChargingSessionService(
    private val chargingSessionRepository: ChargingSessionRepository,
    private val apiWrapper: ChargingSessionAuthServiceApiWrapper
) {
    suspend fun initiateSession(dto: ChargingSessionDto) {
        if (!chargingSessionRepository.sessionAuthRequestExists(dto)) {
            dto.status = apiWrapper.authorize(dto)
            chargingSessionRepository.updateSessionStatus(dto)
        }
    }
}

internal class ChargingSessionAuthServiceApiWrapper(private val api: AuthorizeChargingSessionApi) {

    suspend fun authorize(dto: ChargingSessionDto): String {
        return try {
            api.chargingSessionsPost(
                ChargingSessionsPostRequest(
                    dto.stationId.toString(),
                    dto.driverId,
                    dto.callbackUrl
                )
            ).body().status?.toString() ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}