package online.omniroute.mobile.runtime

import java.io.File

/** Central policy for the managed OmniRoute runtime. */
data class RuntimePolicy(
    val port: Int = 20128,
    val homeDirectory: File,
    val workingDirectory: File,
) {
    init {
        require(port in 1024..65535) { "Invalid runtime port" }
    }

    fun validateChildPath(candidate: File): File {
        val root = workingDirectory.canonicalFile
        val child = candidate.canonicalFile
        require(child == root || child.toPath().startsWith(root.toPath())) {
            "Runtime path escapes managed directory"
        }
        return child
    }
}
