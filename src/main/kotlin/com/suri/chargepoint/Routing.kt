package com.suri.chargepoint

import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostDefaultResponse
import com.suri.chargepoint.domain.chargingsession.controller.chargingSessionRoutes
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun Application.configureRouting(repo: ChargingSessionRepository, worker: AsyncAuthServiceWorker) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            logger.info("Bad request caused by $cause")
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ChargingSessionsPostDefaultResponse(
                    type = "api/status-codes/bad-request",
                    title = "Bad Request",
                    status = 400,
                    detail = "Provided Input is invalid. Error: ${cause.message}"
                )
            )
        }
        exception<Throwable> { call, cause ->
            logger.error(t = cause, msg = { "Internal Server Error occured" })
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ChargingSessionsPostDefaultResponse(
                    type = "api/status-codes/internal-server-error",
                    title = "Internal Server Error",
                    status = 500,
                    detail = "$cause"
                )
            )
        }
    }
    chargingSessionRoutes(
        ChargingSessionService(
            repo,
            worker
        )
    )
}
