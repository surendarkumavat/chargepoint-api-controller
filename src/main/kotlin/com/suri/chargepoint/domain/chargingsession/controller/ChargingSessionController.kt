package com.suri.chargepoint.domain.chargingsession.controller

import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost200Response
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPostRequest
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.util.UUID

internal fun Application.chargingSessionRoutes(service: ChargingSessionService) {
    routing {
        post<ChargingSessionResource.Create> {
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
                ChargingSessionsPost200Response.Status.valueOf(dto.status),
                "Request is being processed asynchronously. The result will be sent to the provided callback URL."
            )
            call.respond(response)
        }
    }
}

@Serializable
@Resource("/charging-sessions")
class ChargingSessionResource {
    @Serializable
    @Resource("")
    class Create(val parent: ChargingSessionResource = ChargingSessionResource())
}