package com.suri.chargepoint.domain.chargingsession.controller


import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost200Response
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post("/charging-sessions") {
            val body: ChargingSessionsPostRequest = call.receive()
            val dto =
                ChargingSessionDto(
                    UUID.fromString(call.callId),
                    UUID.fromString(body.stationId),
                    body.driverToken,
                    body.callbackUrl
                )

            service.initiateSession(dto)

            val response = ChargingSessionsPost200Response(
                status = ChargingSessionsPost200Response.Status.valueOf(dto.status),
                message = "Request is being processed asynchronously. The result will be sent to the provided callback URL."
            )
            call.respond(response)
        }
    }
}