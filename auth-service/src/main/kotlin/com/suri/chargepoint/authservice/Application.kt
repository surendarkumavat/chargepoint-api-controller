package com.suri.chargepoint.authservice

import io.ktor.server.application.*
import io.ktor.server.netty.*


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val httpClient = configureHttpClient(environment.config)

    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }

    //Infra Config
    configureMonitoring()
    configureRouting()
}
