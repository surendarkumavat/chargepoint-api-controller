package com.suri.chargepoint

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install

fun Application.configureHttpClient(): HttpClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }

        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 2_000
            socketTimeoutMillis = 5_000
        }

        // Optional: default headers / base behavior
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        // Optional: validate non-2xx as exceptions
        expectSuccess = false
    }

    environment.monitor.subscribe(ApplicationStopping) {
        client.close()
    }
    return client
}