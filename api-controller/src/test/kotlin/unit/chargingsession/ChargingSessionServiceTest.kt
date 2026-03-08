package com.suri.chargepoint.unit.chargingsession

import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import com.suri.chargepoint.domain.chargingsession.worker.AsyncAuthServiceWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChargingSessionServiceTest {
    private val repo = mockk<ChargingSessionRepository>()

    @BeforeTest
    fun setup() {
        coEvery { repo.sessionAuthRequestExists(any()) } returns false
        coEvery { repo.updateSessionStatus(any()) } returns Unit
        coEvery { repo.add(any()) } returns Unit
    }

    @Test
    fun `Test charging session service invalid`() = runTest {
        val apiWrapper = mockk<ChargingSessionAuthServiceApiWrapper>()
        val worker = AsyncAuthServiceWorker(repo, apiWrapper)
        worker.start()
        val chargingSessionService = ChargingSessionService(
            repo,
            worker
        )
        coEvery { apiWrapper.authorize(any()) } returns "allowed"
        coEvery { apiWrapper.triggerCallBack(any()) } returns Unit

        val correlationId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val driverId = "abc123_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl)

        chargingSessionService.initiateSession(dto)

        val expectedDto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl, "invalid")
        coVerify(timeout = 3000) { apiWrapper.triggerCallBack(expectedDto) }
        worker.stop()
    }

    @Test
    fun `Test charging session service allowed`() = runTest {
        val apiWrapper = mockk<ChargingSessionAuthServiceApiWrapper>()
        val worker = AsyncAuthServiceWorker(repo, apiWrapper)
        worker.start()
        val chargingSessionService = ChargingSessionService(
            repo,
            worker
        )
        coEvery { apiWrapper.authorize(any()) } returns "allowed"
        coEvery { apiWrapper.triggerCallBack(any()) } returns Unit

        val correlationId = UUID.randomUUID()
        val stationId = UUID.randomUUID()
        val driverId = "SurendarKumavat1234_"
        val callBackUrl = "http://localhost:8080/callback"

        val dto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl)

        chargingSessionService.initiateSession(dto)

        val expectedDto = ChargingSessionDto(correlationId, stationId, driverId, callBackUrl, "allowed")
        coVerify(timeout = 3000) { apiWrapper.triggerCallBack(expectedDto) }
        worker.stop()
    }
}