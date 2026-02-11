package com.suri.chargepoint

import com.suri.chargepoint.domain.chargingsession.controller.chargingSessionRoutes
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*

internal fun Application.configureRouting(repo: ChargingSessionRepository, worker: AsyncAuthServiceWorker) {
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    chargingSessionRoutes(
        ChargingSessionService(
            repo,
            worker
        )
    )
}
