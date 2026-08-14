package online.omniroute.mobile.runtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RuntimeViewModel(
    private val manager: TermuxRuntimeManager,
) : ViewModel() {
    private val _status = MutableStateFlow(RuntimeStatus())
    val status: StateFlow<RuntimeStatus> = _status.asStateFlow()

    fun installAndStart() {
        viewModelScope.launch {
            _status.value = _status.value.copy(state = RuntimeState.INSTALLING, message = "Preparing Termux runtime")
            manager.bootstrap().fold(
                onSuccess = {
                    _status.value = _status.value.copy(state = RuntimeState.STARTING, message = "Starting OmniRoute")
                    manager.start().fold(
                        onSuccess = { _status.value = _status.value.copy(state = RuntimeState.RUNNING, message = "OmniRoute started") },
                        onFailure = { error -> _status.value = _status.value.copy(state = RuntimeState.FAILED, message = error.message) },
                    )
                },
                onFailure = { error -> _status.value = _status.value.copy(state = RuntimeState.FAILED, message = error.message) },
            )
        }
    }

    fun stop() {
        viewModelScope.launch {
            _status.value = _status.value.copy(state = RuntimeState.STOPPING, message = "Stopping OmniRoute")
            manager.stop().fold(
                onSuccess = { _status.value = _status.value.copy(state = RuntimeState.UNAVAILABLE, message = "OmniRoute stopped") },
                onFailure = { error -> _status.value = _status.value.copy(state = RuntimeState.FAILED, message = error.message) },
            )
        }
    }
}
