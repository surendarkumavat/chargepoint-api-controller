package com.suri.chargepoint.domain.chargingsession.worker

import com.suri.chargepoint.domain.chargingsession.client.ChargingSessionAuthServiceApiWrapper
import com.suri.chargepoint.domain.chargingsession.dto.ChargingSessionDto
import com.suri.chargepoint.domain.chargingsession.repository.ChargingSessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

internal class AsyncAuthServiceWorker(
    private val repo: ChargingSessionRepository,
    private val apiWrapper: ChargingSessionAuthServiceApiWrapper,
    private val maxParallelRequests: Int = 100
) {
    val driverRegex = Regex("^[\\w-._~]{20,80}$")

    val queue = Channel<ChargingSessionDto>(Channel.UNLIMITED)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    fun start() {
        repeat(100) { // number of concurrent workers
            scope.launch {
                for (dto in queue) {
                    try {
                        processSessionAuthRequest(dto)
                    } catch (t: Throwable) {
                        log.error(t) { "Error calling internal Auth service" }
                    }
                }
            }
        }
    }

    suspend fun processSessionAuthRequest(dto: ChargingSessionDto) {
        if (validateRequest(dto))
            dto.status = apiWrapper.authorize(dto)
        else
            dto.status = "invalid"

        apiWrapper.triggerCallBack(dto)
        repo.updateSessionStatus(dto)
    }

    fun validateRequest(dto: ChargingSessionDto): Boolean {
        return driverRegex.matches(dto.driverId)
    }

    suspend fun enqueueChargingSession(dto: ChargingSessionDto) {
        queue.send(dto)
    }

    fun stop() {
        scope.cancel()
    }
}
