package com.suri.chargepoint

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*

fun Application.configureHttpClient(): HttpClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }

        engine {
            // this: CIOEngineConfig
            maxConnectionsCount = 1000
            requestTimeout = 10_000
            endpoint {
                // this: EndpointConfig
                maxConnectionsPerRoute = 100
                pipelineMaxSize = 20
                keepAliveTime = 5000
                connectTimeout = 5000
                socketTimeout = 5000
                connectAttempts = 5
            }
        }

        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        expectSuccess = true
    }

    environment.monitor.subscribe(ApplicationStopping) {
        client.close()
    }
    return client
}