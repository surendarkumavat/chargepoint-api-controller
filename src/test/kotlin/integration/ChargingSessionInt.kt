package com.suri.chargepoint.integration

import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPost200Response
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost202Response
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPostDefaultResponse
import com.suri.chargepoint.configureRouting
import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal fun ApplicationTestBuilder.setupTestApp(
    repo: ChargingSessionRepository,
    client: HttpClient
) {
    val worker = AsyncAuthServiceWorker(
        repo,
        ChargingSessionAuthServiceApiWrapper("http://auth-service/charging-sessions", client)
    )

    application {
        configureRouting(repo, worker)
        install(CallId) {
            header(HttpHeaders.XRequestId)
            generate {
                UUID.randomUUID().toString()
            }
            verify { callId: String ->
                callId.isNotEmpty()
            }
            replyToHeader(HttpHeaders.XRequestId)
        }
        install(CallLogging) {
            callIdMdc("call-id")
        }
    }
    worker.start()
}

class ChargingSessionInt {
    private val repo = mockk<ChargingSessionRepository>()

    @BeforeTest
    fun setup() {
        coEvery { repo.sessionAuthRequestExists(any()) } returns false
        coEvery { repo.updateSessionStatus(any()) } returns Unit
        coEvery { repo.add(any()) } returns Unit
    }

    @Test
    fun testValidateDriverId() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        setupTestApp(repo, client)

        val callbackBody = CompletableDeferred<JsonObject>()

        externalServices {
            hosts("http://client") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
                routing {
                    post("/callback") {
                        val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                        callbackBody.complete(body)

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        val stationId = UUID.randomUUID()
        val driverId = "abc123_"
        val callBackUrl = "http://client/callback"

        val dto = ChargingSessionsPostRequest(stationId.toString(), driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        // Driver Validation is communicated via callback. so expect ack as response to API
        val results = response.body<ChargingSessionsPost202Response>()
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals("accepted", results.status.toString())

        //Validate Invalid response sent to callback
        val bodySentToExternal = callbackBody.await()
        assertEquals("invalid", bodySentToExternal["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun testValidateStationId() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        setupTestApp(repo, client)

        val stationId = "abc"
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionsPostRequest(stationId, driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        val results = response.body<ChargingSessionsPostDefaultResponse>()

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals("Provided Input is invalid. Error: Invalid charging station ID", results.detail)
    }

    @Test
    fun testValidateCallbackUrl() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        setupTestApp(repo, client)

        val stationId = UUID.randomUUID().toString()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "abc"

        val dto = ChargingSessionsPostRequest(stationId, driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        val results = response.body<ChargingSessionsPostDefaultResponse>()

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals("Provided Input is invalid. Error: Invalid callback url", results.detail)
    }

    @Test
    fun testSessionAck() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        setupTestApp(repo, client)

        val callbackBody = CompletableDeferred<JsonObject>()
        val authServiceBody = CompletableDeferred<JsonObject>()

        externalServices {
            hosts("http://client") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
                routing {
                    post("/callback") {
                        val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                        callbackBody.complete(body)

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            hosts("http://auth-service") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
                routing {
                    post("/charging-sessions") {
                        val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                        authServiceBody.complete(body)

                        call.respond(
                            ChargingSessionsPost200Response(
                                body["station_id"]?.jsonPrimitive?.content,
                                body["driver_token"]?.jsonPrimitive?.content,
                                ChargingSessionsPost200Response.Status.allowed
                            )
                        )
                    }
                }
            }
        }

        val stationId = UUID.randomUUID().toString()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://client/callback"

        val dto = ChargingSessionsPostRequest(stationId, driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        // Assert API Ack Response
        val results = response.body<ChargingSessionsPost202Response>()
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals("accepted", results.status.toString())

        // Assert Auth Service Data
        val bodySentToAuthService = authServiceBody.await()
        assertEquals(callBackUrl, bodySentToAuthService["callback_url"]?.jsonPrimitive?.content)
        assertEquals(stationId, bodySentToAuthService["station_id"]?.jsonPrimitive?.content)
        assertEquals(driverId, bodySentToAuthService["driver_token"]?.jsonPrimitive?.content)

        // Assert Callback Data
        val bodySentToCallback = callbackBody.await()
        assertEquals("allowed", bodySentToCallback["status"]?.jsonPrimitive?.content)
        assertEquals(stationId, bodySentToCallback["station_id"]?.jsonPrimitive?.content)
        assertEquals(driverId, bodySentToCallback["driver_token"]?.jsonPrimitive?.content)
    }

    @Test
    fun testAuthServiceTimeout() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val appClient = createClient {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 1000
                socketTimeoutMillis = 1000
                connectTimeoutMillis = 1000
            }
        }

        setupTestApp(repo, appClient)

        val callbackBody = CompletableDeferred<JsonObject>()
        val authServiceBody = CompletableDeferred<JsonObject>()

        externalServices {
            hosts("http://client") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
                routing {
                    post("/callback") {
                        val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                        callbackBody.complete(body)

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
            hosts("http://auth-service") {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    json()
                }
                routing {
                    post("/charging-sessions") {
                        val body = Json.parseToJsonElement(call.receiveText()).jsonObject
                        authServiceBody.complete(body)

                        delay(1500) // 1500 ms delay

                        call.respond(
                            ChargingSessionsPost200Response(
                                body["station_id"]?.jsonPrimitive?.content,
                                body["driver_token"]?.jsonPrimitive?.content,
                                ChargingSessionsPost200Response.Status.allowed
                            )
                        )
                    }
                }
            }
        }

        val stationId = UUID.randomUUID().toString()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://client/callback"

        val dto = ChargingSessionsPostRequest(stationId, driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }

        // Assert API Ack Response
        val results = response.body<ChargingSessionsPost202Response>()
        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals("accepted", results.status.toString())

        // Assert Auth Service Data
        val bodySentToAuthService = authServiceBody.await()
        assertEquals(callBackUrl, bodySentToAuthService["callback_url"]?.jsonPrimitive?.content)
        assertEquals(stationId, bodySentToAuthService["station_id"]?.jsonPrimitive?.content)
        assertEquals(driverId, bodySentToAuthService["driver_token"]?.jsonPrimitive?.content)

        // Assert Callback Data
        val bodySentToCallback = callbackBody.await()
        assertEquals("unknown", bodySentToCallback["status"]?.jsonPrimitive?.content)
        assertEquals(stationId, bodySentToCallback["station_id"]?.jsonPrimitive?.content)
        assertEquals(driverId, bodySentToCallback["driver_token"]?.jsonPrimitive?.content)
    }
}