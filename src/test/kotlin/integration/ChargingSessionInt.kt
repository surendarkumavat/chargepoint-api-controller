package com.suri.chargepoint.integration

import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPostRequest
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost202Response
import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPostDefaultResponse
import com.suri.chargepoint.configureRouting
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val worker: AsyncAuthServiceWorker = mockk(relaxed = true)
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

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val stationId = UUID.randomUUID()
        val driverId = "abc123_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionsPostRequest(stationId.toString(), driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        val results = response.body<ChargingSessionsPostDefaultResponse>()

        assertEquals(HttpStatusCode.BadRequest, response.status)

        assertEquals("Provided Input is invalid. Error: Invalid Driver Token", results.detail)
    }

    @Test
    fun testValidateStationId() = testApplication {
        val worker: AsyncAuthServiceWorker = mockk(relaxed = true)
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

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val stationId = "abc"
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionsPostRequest(stationId.toString(), driverId, callBackUrl)
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
        val worker: AsyncAuthServiceWorker = mockk(relaxed = true)
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

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val stationId = UUID.randomUUID().toString()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "abc"

        val dto = ChargingSessionsPostRequest(stationId.toString(), driverId, callBackUrl)
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
        val worker: AsyncAuthServiceWorker = mockk(relaxed = true)
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

        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        val stationId = UUID.randomUUID().toString()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionsPostRequest(stationId.toString(), driverId, callBackUrl)
        val response = client.post("/charging-sessions") {
            contentType(ContentType.Application.Json)
            setBody(dto)
        }
        val results = response.body<ChargingSessionsPost202Response>()

        assertEquals(HttpStatusCode.Accepted, response.status)

        assertEquals("accepted", results.status.toString())
    }
}