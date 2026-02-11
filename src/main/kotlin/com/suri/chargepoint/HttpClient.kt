package com.suri.chargepoint

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*

fun Application.configureHttpClient(config: HttpClientConfig, httpClientEngine: HttpClientEngine): HttpClient {
    val client = HttpClient(httpClientEngine) {
        install(ContentNegotiation) { json() }

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

data class HttpClientConfig(
    val maxConnectionsCount: Int = 1000,
    val pipelineMaxSize: Int = 20,
    val keepAliveTime: Long = 5000,
    val connectTimeout: Long = 5000,
    val socketTimeout: Long = 5000,
    val requestTimeout: Long = 10_000,
    val connectAttempts: Int = 5,
    val maxConnectionsPerRoute: Int = 100
)