package com.suri.chargepoint

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepositoryImpl
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.server.application.*


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val repo: ChargingSessionRepository = ChargingSessionRepositoryImpl()
    val httpClient = configureHttpClient()
    val apiWrapper = ChargingSessionAuthServiceApiWrapper(AuthorizeChargingSessionApi(), httpClient)
    val worker = AsyncAuthServiceWorker(repo, apiWrapper)

    configureMonitoring()
    configureDatabases()
    configureRouting(repo, worker)


    environment.monitor.subscribe(ApplicationStarted) {
        worker.start()
    }

    environment.monitor.subscribe(ApplicationStopping) {
        worker.stop()
    }
}
