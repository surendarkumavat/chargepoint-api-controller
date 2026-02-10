package com.suri.chargepoint.domain.chargingsession.dto

import com.suri.chargepoint.apicontroller.server.chargingsession.models.ChargingSessionsPost200Response
import java.util.UUID

data class ChargingSessionDto(
    val correlationId: UUID,
    val stationId: UUID,
    val driverId: String,
    val callbackUrl: String,
    var status: String = ChargingSessionsPost200Response.Status.accepted.toString()
) {
}