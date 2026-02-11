package com.suri.chargepoint

import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostDefaultResponse
import com.suri.chargepoint.domain.chargingsession.controller.chargingSessionRoutes
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

internal fun Application.configureRouting(repo: ChargingSessionRepository, worker: AsyncAuthServiceWorker) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(status = HttpStatusCode.InternalServerError, message = ChargingSessionsPostDefaultResponse(type = "api/status-codes/internal-server-error", title = "Internal Server Error", status = 500, detail = "$cause"))
        }
    }
    chargingSessionRoutes(
        ChargingSessionService(
            repo,
            worker
        )
    )
}
