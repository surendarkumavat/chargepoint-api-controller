package com.suri.chargepoint.utils.authservice

import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepositoryImpl
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.netty.EngineMain


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val repo: ChargingSessionRepository = ChargingSessionRepositoryImpl()
    val httpClient = configureHttpClient(environment.config)

    val authServiceUrl: String = environment.config.propertyOrNull("app.api-controller.auth-service.base-path")?.getAs()
        ?: throw IllegalStateException("Missing application configuration property app.api-controller.auth-service.endpoint")

    val apiWrapper = ChargingSessionAuthServiceApiWrapper(authServiceUrl, httpClient)

    //configure worker
    val worker = AsyncAuthServiceWorker(
        repo,
        apiWrapper,
        environment.config.propertyOrNull("app.api-controller.auth-service.max-parallel-requests")?.getAs()
            ?: 100
    )
    environment.monitor.subscribe(ApplicationStarted) {
        worker.start()
    }

    environment.monitor.subscribe(ApplicationStopping) {
        worker.stop()
        httpClient.close()
    }

    //Infra Config
    configureMonitoring()
    configureDatabases()
    configureRouting(repo, worker)
}
