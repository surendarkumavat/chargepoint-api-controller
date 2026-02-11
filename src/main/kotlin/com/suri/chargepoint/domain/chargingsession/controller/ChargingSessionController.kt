package com.suri.chargepoint.domain.chargingsession.controller


import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost200Response
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post("/charging-sessions") {
            val driverRegex = Regex("^[\\w-._~]{20,80}$")
            val callbackRegex =
                Regex("^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$'")
            val body: ChargingSessionsPostRequest = call.receive()
            val correlationId: UUID = UUID.fromString(call.callId)

            val stationId = try {
                UUID.fromString(body.stationId)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid charging station ID")
            }

            if (!driverRegex.matches(body.driverToken))
                throw BadRequestException("Invalid Driver Token")

            if (!callbackRegex.matches(body.callbackUrl))
                throw BadRequestException("Invalid Callback Url")

            val dto =
                ChargingSessionDto(
                    correlationId = correlationId,
                    stationId = stationId,
                    body.driverToken,
                    body.callbackUrl
                )

            service.initiateSession(dto)

            val response = ChargingSessionsPost200Response(
                status = ChargingSessionsPost200Response.Status.valueOf(dto.status),
                message = "Request is being processed asynchronously. The result will be sent to the provided callback URL."
            )
            call.respond(status = HttpStatusCode.Accepted, message = response)
        }
    }
}