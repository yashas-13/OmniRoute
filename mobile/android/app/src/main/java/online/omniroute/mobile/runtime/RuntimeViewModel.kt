package online.omniroute.mobile.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import online.omniroute.mobile.gateway.GatewaySnapshot
import online.omniroute.mobile.gateway.OmniRouteClient

class RuntimeViewModel(
    private val manager: TermuxRuntimeManager,
    private val client: OmniRouteClient,
) : ViewModel() {
    private val _status = MutableStateFlow(RuntimeStatus())
    val status: StateFlow<RuntimeStatus> = _status.asStateFlow()

    private val _snapshot = MutableStateFlow<GatewaySnapshot?>(null)
    val snapshot: StateFlow<GatewaySnapshot?> = _snapshot.asStateFlow()

    fun installAndStart() {
        viewModelScope.launch {
            _status.value = _status.value.copy(state = RuntimeState.INSTALLING, message = "Preparing Termux runtime")
            manager.bootstrap().fold(
                onSuccess = {
                    _status.value = _status.value.copy(state = RuntimeState.STARTING, message = "Starting OmniRoute")
                    manager.start().fold(
                        onSuccess = { waitForGateway() },
                        onFailure = { error -> fail(error) },
                    )
                },
                onFailure = { error -> fail(error) },
            )
        }
    }

    fun stop() {
        viewModelScope.launch {
            _status.value = _status.value.copy(state = RuntimeState.STOPPING, message = "Stopping OmniRoute")
            manager.stop().fold(
                onSuccess = {
                    _snapshot.value = null
                    _status.value = _status.value.copy(state = RuntimeState.UNAVAILABLE, message = "OmniRoute stopped", pid = null)
                },
                onFailure = { error -> fail(error) },
            )
        }
    }

    private suspend fun waitForGateway() {
        repeat(20) {
            client.snapshot().fold(
                onSuccess = { snapshot ->
                    _snapshot.value = snapshot
                    _status.value = _status.value.copy(
                        state = RuntimeState.RUNNING,
                        message = "OmniRoute is healthy",
                    )
                    return
                },
                onFailure = { error ->
                    _status.value = _status.value.copy(
                        state = RuntimeState.STARTING,
                        message = "Waiting for gateway: ${error.message ?: "connection pending"}",
                    )
                },
            )
            delay(500)
        }
        fail(IllegalStateException("OmniRoute started but the local API did not become healthy"))
    }

    fun refresh() {
        viewModelScope.launch {
            client.snapshot().fold(
                onSuccess = { snapshot ->
                    _snapshot.value = snapshot
                    _status.value = _status.value.copy(state = RuntimeState.RUNNING, message = "Gateway healthy")
                },
                onFailure = { error ->
                    _status.value = _status.value.copy(state = RuntimeState.DEGRADED, message = error.message ?: "Gateway unavailable")
                },
            )
        }
    }

    private fun fail(error: Throwable) {
        _status.value = _status.value.copy(state = RuntimeState.FAILED, message = error.message ?: error::class.simpleName)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
