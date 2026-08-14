package online.omniroute.mobile.runtime

import java.nio.file.Paths

/** Logical paths owned by the managed Termux runtime. They are not Android app-private paths. */
data class RuntimePolicy(
    val port: Int = 20128,
    val homeDirectory: String = "~/.omniroute-mobile",
    val workingDirectory: String = "~/.omniroute-mobile/runtime",
) {
    init {
        require(port in 1024..65535) { "Invalid runtime port" }
    }

    fun validateChildPath(candidate: String): String {
        val root = normalize(workingDirectory)
        val child = normalize(candidate)
        require(child == root || child.startsWith("$root/")) {
            "Runtime path escapes managed directory"
        }
        return child
    }

    private fun normalize(value: String): String =
        Paths.get(value).normalize().toString().replace('\\', '/')
}
