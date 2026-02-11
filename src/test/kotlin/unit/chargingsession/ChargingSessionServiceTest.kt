package com.suri.chargepoint.unit.chargingsession

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.apicontroller.client.authservice.models.ChargingSessionsPost200Response
import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargingSessionServiceTest {
    private val repo = mockk<ChargingSessionRepository>()

    @BeforeTest
    fun setup() {
        coEvery { repo.sessionAuthRequestExists(any()) } returns false
        coEvery { repo.updateSessionStatus(any()) } returns Unit
        coEvery { repo.add(any()) } returns Unit
    }

    @Test
    fun `Test charging session service valid`() = runTest {
        val apiWrapper = mockk<ChargingSessionAuthServiceApiWrapper>()
        val worker = AsyncAuthServiceWorker(repo, apiWrapper)
        worker.start()
        val chargingSessionService = ChargingSessionService(
            repo,
            worker
        )
        coEvery { apiWrapper.authorize(any()) } returns "valid"
        coEvery { apiWrapper.triggerCallBack(any()) } returns Unit

        val correlationId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val driverId = "abc123_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl)

        chargingSessionService.initiateSession(dto)

        val expectedDto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl, "valid")
        coVerify(timeout = 3000) { apiWrapper.triggerCallBack(expectedDto) }
        worker.stop()
    }

    @Test
    fun `Test charging session auth service error`() = runTest {
        val done = CompletableDeferred<Unit>()
        val api: AuthorizeChargingSessionApi = mockk(relaxed = true)
        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)

            val textContent = request.body as TextContent
            val jsonText = textContent.text
            val body = Json.decodeFromString<ChargingSessionsPost200Response>(jsonText)
            assertEquals("unknown", body.status.toString())
            done.complete(Unit)

            respond(
                content = ByteReadChannel("""{"success": true}"""),
                status = HttpStatusCode.OK
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json()
            }
        }
        val apiWrapper = ChargingSessionAuthServiceApiWrapper(api, client)

        val worker = AsyncAuthServiceWorker(repo, apiWrapper)
        worker.start()

        val chargingSessionService = ChargingSessionService(
            repo,
            worker
        )
        coEvery { api.chargingSessionsPost(any()) } throws Exception("test")
        //coEvery { apiWrapper.triggerCallBack(any()) } returns Unit

        val correlationId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val driverId = "abc123_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl)

        chargingSessionService.initiateSession(dto)

        done.await()
        worker.stop()
    }
}