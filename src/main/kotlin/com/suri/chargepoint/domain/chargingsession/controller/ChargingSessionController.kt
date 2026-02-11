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
import java.net.URI
import java.util.*

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post("/charging-sessions") {
            val driverRegex = Regex("^[\\w-._~]{20,80}$")
            val body: ChargingSessionsPostRequest = call.receive()
            val correlationId: UUID = UUID.fromString(call.callId)

            val stationId = try {
                UUID.fromString(body.stationId)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid charging station ID")
            }

            if (!driverRegex.matches(body.driverToken))
                throw BadRequestException("Invalid Driver Token")

            try {
                val uri = URI(body.callbackUrl)

                if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank())
                    throw BadRequestException("Invalid callback url")
            } catch (e: Exception) {
                throw BadRequestException("Invalid callback url")
            }

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