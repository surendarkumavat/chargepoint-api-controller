package com.suri.chargepoint.authservice.domain.chargingsession.controller


import com.suri.chargepoint.authservice.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.authservice.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPost200Response
import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPostRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.net.URI
import java.util.*

val DRIVER_TOKEN_REGEX = Regex("^[\\w-._~]{20,80}$")

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post("/charging-sessions") {
            val body: ChargingSessionsPostRequest = call.receive()
            val correlationId: UUID = UUID.fromString(call.callId)

            val stationId = try {
                UUID.fromString(body.stationId)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid charging station ID")
            }

            try {
                val uri = URI(body.callbackUrl)

                if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank())
                    throw BadRequestException("Invalid callback url")
            } catch (e: Exception) {
                throw BadRequestException("Invalid callback url")
            }

            if (!DRIVER_TOKEN_REGEX.matches(body.driverToken))
                throw BadRequestException("Invalid Driver Token")

            val dto =
                ChargingSessionDto(
                    correlationId = correlationId,
                    stationId = stationId,
                    body.driverToken,
                    body.callbackUrl
                )

            service.authorizeSession(dto)

            val response = ChargingSessionsPost200Response(
                stationId = dto.stationId.toString(),
                driverToken = dto.driverId,
                status = ChargingSessionsPost200Response.Status.allowed,
            )
            call.respond(status = HttpStatusCode.OK, message = response)
        }
    }
}