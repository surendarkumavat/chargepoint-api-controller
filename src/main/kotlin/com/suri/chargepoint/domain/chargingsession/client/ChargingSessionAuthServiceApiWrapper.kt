package com.suri.chargepoint.domain.chargingsession.client

import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPost200Response
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

internal class ChargingSessionAuthServiceApiWrapper(
    private val authServiceUrl: String,
    private val client: HttpClient
) {
    suspend fun authorize(dto: ChargingSessionDto): String {
        return try {
            val response = client.post(authServiceUrl) {
                contentType(ContentType.Application.Json)
                setBody(ChargingSessionsPostRequest(
                    stationId = dto.stationId.toString(),
                    driverToken = dto.driverId,
                    callbackUrl = dto.callbackUrl
                ))
            }
            val results = response.body<ChargingSessionsPost200Response>()
            results.status.toString()
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
                        stationId = dto.stationId.toString(),
                        driverToken = dto.driverId,
                        status = ChargingSessionsPost200Response.Status.valueOf(dto.status)
                    )
                )
            }
        } catch (e: Exception) {
            log.error(e) { "Error calling callback" }
        }
    }
}