package com.suri.chargepoint.authservice.domain.chargingsession.service

import com.suri.chargepoint.authservice.domain.chargingsession.controller.DRIVER_TOKEN_REGEX
import com.suri.chargepoint.authservice.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPost200Response.Status
import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.delay
import java.net.URI

internal class ChargingSessionService(
) {
    suspend fun authorizeSession(dto: ChargingSessionDto): Status {
        try {
            val uri = URI(dto.callbackUrl)

            if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrBlank())
                throw InvalidCallbackUrlException()
        } catch (e: Exception) {
            throw InvalidCallbackUrlException()
        }

        if (!DRIVER_TOKEN_REGEX.matches(dto.driverId))
            throw InvalidDriverTokenException()

        if (dto.driverId == "validDriverToken123")
            return Status.allowed

        if (dto.driverId == "timeoutDriverToken123") {
            delay(6000)
            return Status.allowed
        }

        return Status.not_allowed
    }
}

sealed class AuthException(message: String) : Exception(message)
class InvalidDriverTokenException : AuthException("Invalid Driver Token")
class InvalidCallbackUrlException : AuthException("Invalid Callback URL")