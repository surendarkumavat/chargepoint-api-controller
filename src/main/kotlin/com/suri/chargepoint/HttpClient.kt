package com.suri.chargepoint

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*

internal fun configureHttpClient(config: ApplicationConfig): HttpClient {

    //Configure Http Clients
    val httpConfig = config.config("app.api-controller.http-client")
    val httpClientConfig = HttpClientConfig(
        maxConnectionsCount = httpConfig.propertyOrNull("max-connections-count")?.getAs() ?: 1000,
        maxConnectionsPerRoute = httpConfig.propertyOrNull("max-connections-per-route")?.getAs() ?: 100,
        pipelineMaxSize = httpConfig.propertyOrNull("pipeline-max-size")?.getAs() ?: 20,
        keepAliveTime = httpConfig.propertyOrNull("keep-alive-time")?.getAs() ?: 5000,
        connectTimeout = httpConfig.propertyOrNull("connect-timeout")?.getAs() ?: 5000,
        socketTimeout = httpConfig.propertyOrNull("socket-timeout")?.getAs() ?: 5000,
        requestTimeout = httpConfig.propertyOrNull("request-timeout")?.getAs() ?: 10_000,
        connectAttempts = httpConfig.propertyOrNull("connect-attempts")?.getAs() ?: 5
    )
    val httpClientEngine = CIO.create() {
        // this: CIOEngineConfig
        maxConnectionsCount = httpClientConfig.maxConnectionsCount
        requestTimeout = httpClientConfig.requestTimeout
        endpoint {
            // this: EndpointConfig
            maxConnectionsPerRoute = httpClientConfig.maxConnectionsPerRoute
            pipelineMaxSize = httpClientConfig.pipelineMaxSize
            keepAliveTime = httpClientConfig.keepAliveTime
            connectTimeout = httpClientConfig.connectTimeout
            socketTimeout = httpClientConfig.socketTimeout
            connectAttempts = httpClientConfig.connectAttempts
        }
    }

    val client = HttpClient(httpClientEngine) {
        install(ContentNegotiation) { json() }

        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }

        expectSuccess = true
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