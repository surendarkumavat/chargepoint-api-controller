package com.suri.chargepoint

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepositoryImpl
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val repo: ChargingSessionRepository = ChargingSessionRepositoryImpl()

    //Configure Http Clients
    val httpConfig = environment.config.config("app.api-controller.http-client")
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
    val httpClient = configureHttpClient(httpClientConfig, httpClientEngine)

    val api = AuthorizeChargingSessionApi(
        baseUrl = environment.config.propertyOrNull("app.api-controller.auth-service.base-path")?.getAs()
            ?: throw IllegalStateException("Missing application configuration property app.api-controller.auth-service.base-path"),
        httpClientEngine = httpClientEngine,
        httpClientConfig = { config ->
            config.install(ContentNegotiation) {
                json()
            }
            config.defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
            config.expectSuccess = true
        })

    val apiWrapper = ChargingSessionAuthServiceApiWrapper(api, httpClient)

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
        httpClientEngine.close()
    }

    //Infra Config
    configureMonitoring()
    configureDatabases()
    configureRouting(repo, worker)
}
