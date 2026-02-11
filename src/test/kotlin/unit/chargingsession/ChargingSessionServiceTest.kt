package com.suri.chargepoint.unit.chargingsession

import com.suri.chargepoint.apicontroller.client.authservice.apis.AuthorizeChargingSessionApi
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargingSessionServiceTest {
    private val chargingSessionRepository = mockk<ChargingSessionRepository>()

    @BeforeTest
    fun setup() {
        coEvery { chargingSessionRepository.sessionAuthRequestExists(any()) } returns false
        coEvery { chargingSessionRepository.updateSessionStatus(any()) } returns Unit
    }

    @Test
    fun `Test charging session service valid`() = runTest {
        val apiWrapper = mockk<ChargingSessionAuthServiceApiWrapper>()
        val chargingSessionService = ChargingSessionService(
            chargingSessionRepository,
            apiWrapper
        )
        coEvery { apiWrapper.authorize(any()) } returns "valid"

        val dto = ChargingSessionDto(UUID.randomUUID(), UUID.randomUUID(), "abc123_", "accepted")
        chargingSessionService.initiateSession(dto)

        assertEquals("valid", dto.status)
    }

    @Test
    fun `Test charging session auth service error`() = runTest {
        coEvery { chargingSessionRepository.sessionAuthRequestExists(any()) } returns false
        coEvery { chargingSessionRepository.updateSessionStatus(any()) } returns Unit

        val api: AuthorizeChargingSessionApi = mockk<AuthorizeChargingSessionApi>()
        val apiWrapper = ChargingSessionAuthServiceApiWrapper(api)
        val chargingSessionService = ChargingSessionService(
            chargingSessionRepository,
            apiWrapper
        )
        coEvery { api.chargingSessionsPost(any()) } throws Exception("test")

        val dto = ChargingSessionDto(UUID.randomUUID(), UUID.randomUUID(), "abc123_", "accepted")
        chargingSessionService.initiateSession(dto)

        assertEquals("unknown", dto.status)
    }
}