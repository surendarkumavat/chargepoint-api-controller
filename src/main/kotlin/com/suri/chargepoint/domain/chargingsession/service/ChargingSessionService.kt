package com.suri.chargepoint.domain.chargingsession.service

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPost200Response
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

internal class ChargingSessionService(
    private val chargingSessionRepository: ChargingSessionRepository,
    private val apiWrapper: ChargingSessionAuthServiceApiWrapper
) {
    suspend fun initiateSession(dto: ChargingSessionDto) {
        if (!chargingSessionRepository.sessionAuthRequestExists(dto)) {
            dto.status = apiWrapper.authorize(dto)
            apiWrapper.triggerCallBack(dto)
            chargingSessionRepository.updateSessionStatus(dto)
        }
    }
}

internal class ChargingSessionAuthServiceApiWrapper(
    private val api: AuthorizeChargingSessionApi,
    private val client: HttpClient
) {
    suspend fun authorize(dto: ChargingSessionDto): String {
        return try {
            api.chargingSessionsPost(
                ChargingSessionsPostRequest(
                    dto.stationId.toString(),
                    dto.driverId,
                    dto.callbackUrl
                )
            ).body().status?.toString() ?: "unknown"
        } catch (e: Exception) {
            log.error(e) { "Error calling internal Auth service" }
            "unknown"
        }
    }

    suspend fun triggerCallBack(dto: ChargingSessionDto) {
        try {
            client.post(dto.callbackUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    ChargingSessionsPost200Response(
                        dto.stationId,
                        dto.callbackUrl,
                        ChargingSessionsPost200Response.Status.valueOf(dto.status)
                    )
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Error calling callback" }
        }
    }
}