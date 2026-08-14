package online.omniroute.mobile.runtime

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TermuxCommandRunner {
    suspend fun run(command: List<String>): Result<Unit>
}

class TermuxIntentCommandRunner(private val context: Context) : TermuxCommandRunner {
    override suspend fun run(command: List<String>): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            require(command.isNotEmpty()) { "Empty command" }
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setPackage("com.termux")
                putExtra("com.termux.RUN_COMMAND_PATH", command.first())
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", command.drop(1).toTypedArray())
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", null as android.app.PendingIntent?)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startService(intent)
        }
    }
}

class TermuxRuntimeManager(
    private val runner: TermuxCommandRunner,
    private val policy: RuntimePolicy,
) {
    suspend fun bootstrap(): Result<Unit> {
        val script = """
            set -eu
            mkdir -p "$${'$'}HOME/.omniroute-runtime"
            cd "$${'$'}HOME/.omniroute-runtime"
            if ! command -v node >/dev/null 2>&1; then
              pkg update -y
              pkg install -y nodejs-lts
            fi
            if [ ! -f package.json ]; then
              printf '%s\\n' '{"private":true,"type":"module"}' > package.json
            fi
            npm install --no-save omniroute
        """.trimIndent()
        return runner.run(listOf("/data/data/com.termux/files/usr/bin/bash", "-lc", script))
    }

    suspend fun start(): Result<Unit> {
        val command = "cd \"${policy.workingDirectory.path}\" && export HOME=\"${policy.homeDirectory.path}\" && export OMNIROUTE_HOST=127.0.0.1 && export PORT=${policy.port} && exec omniroute"
        return runner.run(listOf("/data/data/com.termux/files/usr/bin/bash", "-lc", command))
    }

    suspend fun stop(): Result<Unit> = runner.run(
        listOf("/data/data/com.termux/files/usr/bin/pkill", "-f", "omniroute")
    )
}
