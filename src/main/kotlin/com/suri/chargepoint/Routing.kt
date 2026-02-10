package com.suri.chargepoint

import com.codahale.metrics.*
import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.domain.chargingsession.controller.chargingSessionRoutes
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepositoryImpl
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.metrics.dropwizard.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.slf4j.event.*

fun Application.configureRouting() {
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    chargingSessionRoutes(ChargingSessionService(
        ChargingSessionRepositoryImpl(),
        AuthorizeChargingSessionApi()
    ))
}
