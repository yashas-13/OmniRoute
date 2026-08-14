package online.omniroute.mobile.runtime

enum class RuntimeState {
    UNAVAILABLE,
    INSTALLING,
    STARTING,
    RUNNING,
    DEGRADED,
    FAILED,
    RECOVERING,
    STOPPING,
}

data class RuntimeStatus(
    val state: RuntimeState = RuntimeState.UNAVAILABLE,
    val pid: Long? = null,
    val port: Int = 20128,
    val message: String? = null,
)
