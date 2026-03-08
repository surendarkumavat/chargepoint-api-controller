package com.suri.chargepoint.authservice.domain.chargingsession.dto

import com.suri.chargepoint.authservice.server.chargingsession.models.ChargingSessionsPost200Response
import java.util.*

data class ChargingSessionDto(
    val correlationId: UUID,
    val stationId: UUID,
    val driverId: String,
    val callbackUrl: String,
    var status: String = ChargingSessionsPost200Response.Status.accepted.toString()
) {
}