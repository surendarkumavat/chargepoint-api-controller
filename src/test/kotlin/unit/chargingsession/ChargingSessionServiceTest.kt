package com.suri.chargepoint.unit.chargingsession

import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.service.ChargingSessionService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ChargingSessionServiceTest {
    private val chargingSessionRepository = mockk<ChargingSessionRepository>()
    private val apiWrapper = mockk<ChargingSessionAuthServiceApiWrapper>()
    private val chargingSessionService = ChargingSessionService(
        chargingSessionRepository,
        apiWrapper
    )

    @Test
    fun `Test charging session service ack`() = runTest {
        coEvery { chargingSessionRepository.sessionAuthRequestExists(any()) } returns false
        coEvery { chargingSessionRepository.updateSessionStatus(any()) } returns Unit
        coEvery { apiWrapper.authorize(any()) } returns "valid"
        val dto = ChargingSessionDto(UUID.randomUUID(), UUID.randomUUID(), "abc123_", "accepted")
        chargingSessionService.initiateSession(dto)
        assertEquals("valid", dto.status)
    }
}