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
            require(command.first() == "/data/data/com.termux/files/usr/bin/bash") {
                "Only the managed Termux bash entrypoint is allowed"
            }
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setPackage("com.termux")
                putExtra("com.termux.RUN_COMMAND_PATH", command.first())
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", command.drop(1).toTypedArray())
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
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
    private val runtimeRoot = policy.validateChildPath(policy.workingDirectory)

    suspend fun bootstrap(): Result<Unit> {
        val home = shellQuote(policy.homeDirectory.path)
        val root = shellQuote(runtimeRoot.path)
        val script = """
            set -eu
            export HOME=$home
            mkdir -p $root
            cd $root
            if ! command -v node >/dev/null 2>&1; then
              pkg update -y
              pkg install -y nodejs-lts
            fi
            if [ ! -f package.json ]; then
              printf '%s\\n' '{"private":true,"type":"module"}' > package.json
            fi
            npm install --no-save omniroute
        """.trimIndent()
        return runner.run(bash(script))
    }

    suspend fun start(): Result<Unit> {
        val home = shellQuote(policy.homeDirectory.path)
        val root = shellQuote(runtimeRoot.path)
        val log = shellQuote(runtimeRoot.resolve("omniroute.log").path)
        val pid = shellQuote(runtimeRoot.resolve(".omniroute.pid").path)
        val command = "cd $root && export HOME=$home && export OMNIROUTE_HOST=127.0.0.1 && export PORT=${policy.port} && nohup omniroute >> $log 2>&1 & echo \$! > $pid"
        return runner.run(bash(command))
    }

    suspend fun stop(): Result<Unit> {
        val home = shellQuote(policy.homeDirectory.path)
        val root = shellQuote(runtimeRoot.path)
        val pid = shellQuote(runtimeRoot.resolve(".omniroute.pid").path)
        val command = "export HOME=$home; cd $root; if [ -f $pid ]; then kill \"\$(cat $pid)\" 2>/dev/null || true; rm -f $pid; fi"
        return runner.run(bash(command))
    }

    private fun bash(script: String): List<String> = listOf(
        "/data/data/com.termux/files/usr/bin/bash", "-lc", script
    )

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
