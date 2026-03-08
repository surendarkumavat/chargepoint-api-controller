package com.suri.chargepoint.authservice.domain.chargingsession.controller


import com.suri.chargepoint.authservice.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.authservice.domain.chargingsession.service.AuthException
import com.suri.chargepoint.authservice.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPost200Response
import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPostRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

val DRIVER_TOKEN_REGEX = Regex("^[\\w-._~]{20,80}$")

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post("/charging-sessions") {
            val body: ChargingSessionsPostRequest = call.receive()
            val correlationId: UUID = UUID.fromString(call.callId)

            val stationId = UUID.fromString(body.stationId)

            val dto =
                ChargingSessionDto(
                    correlationId = correlationId,
                    stationId = stationId,
                    body.driverToken,
                    body.callbackUrl
                )

            val status = try {
                service.authorizeSession(dto)
            } catch (e: AuthException) {
                ChargingSessionsPost200Response.Status.invalid
            }

            val response = ChargingSessionsPost200Response(
                stationId = dto.stationId.toString(),
                driverToken = dto.driverId,
                status = status,
            )
            call.respond(status = HttpStatusCode.OK, message = response)
        }
    }
}