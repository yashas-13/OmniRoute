package online.omniroute.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.omniroute.mobile.gateway.OmniRouteClient
import online.omniroute.mobile.runtime.RuntimePolicy
import online.omniroute.mobile.runtime.RuntimeState
import online.omniroute.mobile.runtime.RuntimeStatus
import online.omniroute.mobile.runtime.RuntimeViewModel
import online.omniroute.mobile.runtime.RuntimeViewModelFactory
import online.omniroute.mobile.runtime.TermuxIntentCommandRunner
import online.omniroute.mobile.runtime.TermuxRuntimeManager

private enum class AppTab(val label: String) {
    HOME("Home"),
    PROVIDERS("Providers"),
    ROUTING("Routing"),
    USAGE("Usage"),
    SETTINGS("Settings"),
}

class MainActivity : ComponentActivity() {
    private val runtimeViewModel: RuntimeViewModel by viewModels {
        val policy = RuntimePolicy()
        RuntimeViewModelFactory(
            manager = TermuxRuntimeManager(
                runner = TermuxIntentCommandRunner(applicationContext),
                policy = policy,
            ),
            client = OmniRouteClient(
                baseUrl = "http://127.0.0.1:${policy.port}",
                apiKeyProvider = { null },
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniRouteApp(runtimeViewModel) }
    }
}

@Composable
private fun OmniRouteApp(viewModel: RuntimeViewModel) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                topBar = { TopAppBar(title = { Text("OmniRoute") }) },
                bottomBar = {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        AppTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Text(item.label.take(1)) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                },
            ) { padding ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(status, snapshot, viewModel, Modifier.padding(padding))
                    AppTab.PROVIDERS -> ProvidersScreen(snapshot?.models?.count, Modifier.padding(padding))
                    AppTab.ROUTING -> RoutingScreen(status, Modifier.padding(padding))
                    AppTab.USAGE -> UsageScreen(snapshot?.rateLimits, Modifier.padding(padding))
                    AppTab.SETTINGS -> SettingsScreen(status, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(status: RuntimeStatus, snapshot: online.omniroute.mobile.gateway.GatewaySnapshot?, viewModel: RuntimeViewModel, modifier: Modifier = Modifier) {
    val running = status.state == RuntimeState.RUNNING || status.state == RuntimeState.DEGRADED
    val busy = status.state == RuntimeState.INSTALLING || status.state == RuntimeState.STARTING || status.state == RuntimeState.STOPPING
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!running && status.state == RuntimeState.UNAVAILABLE) {
            OnboardingCard(viewModel::installAndStart)
        } else {
            GatewayCard(status, running, busy, viewModel)
        }
        HealthCard(status, snapshot, viewModel)
        RuntimeCard(status)
    }
}

@Composable
private fun OnboardingCard(onStart: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Run OmniRoute on your phone", style = MaterialTheme.typography.headlineSmall)
            Text("The native app manages a dedicated Termux runtime. You do not need to run Node.js, npm, or shell commands manually.", style = MaterialTheme.typography.bodyLarge)
            Text("The gateway is local-only by default: 127.0.0.1:20128")
            Button(onClick = onStart, Modifier.fillMaxWidth()) { Text("Get Started") }
        }
    }
}

@Composable
private fun GatewayCard(status: RuntimeStatus, running: Boolean, busy: Boolean, viewModel: RuntimeViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Gateway", style = MaterialTheme.typography.titleLarge)
            Text(gatewayLabel(status.state), style = MaterialTheme.typography.headlineSmall)
            Text(status.message ?: "No runtime message")
            Text("Endpoint: 127.0.0.1:${status.port}")
            status.pid?.let { Text("Process: $it") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy && !running, onClick = viewModel::installAndStart) { Text("Start") }
                OutlinedButton(enabled = !busy && running, onClick = viewModel::stop) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun HealthCard(status: RuntimeStatus, snapshot: online.omniroute.mobile.gateway.GatewaySnapshot?, viewModel: RuntimeViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Health", style = MaterialTheme.typography.titleLarge)
            Text("Gateway: ${if (snapshot != null) "Healthy" else "Not verified"}")
            Text("Models discovered: ${snapshot?.models?.count ?: "—"}")
            Text("Resilience: ${if (snapshot?.resilience != null) "Connected" else "Not loaded"}")
            Text("Rate limits: ${if (snapshot?.rateLimits != null) "Connected" else "Not loaded"}")
            OutlinedButton(onClick = viewModel::refresh) { Text("Refresh") }
        }
    }
}

@Composable
private fun RuntimeCard(status: RuntimeStatus) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Runtime", style = MaterialTheme.typography.titleLarge)
            Text("Termux-managed runtime")
            Text("Port: ${status.port}")
            Text("Security: dedicated workspace + loopback gateway")
            Text("Termux is a managed runtime, not VM/container-grade isolation.")
        }
    }
}

@Composable
private fun ProvidersScreen(modelCount: Int?, modifier: Modifier = Modifier) = PlaceholderScreen(modifier, "Providers", "Live model discovery is connected to /v1/models. Provider administration is next.", listOf("Discovered models: ${modelCount ?: "—"}", "Connection testing", "Secure key management"))

@Composable
private fun RoutingScreen(status: RuntimeStatus, modifier: Modifier = Modifier) = PlaceholderScreen(modifier, "Routing", "Gateway connectivity is live. Routing controls will be wired to the supported server configuration API.", listOf("Gateway state: ${status.state}", "Automatic failover", "Health scoring", "Circuit breaker"))

@Composable
private fun UsageScreen(rateLimits: String?, modifier: Modifier = Modifier) = PlaceholderScreen(modifier, "Usage", "Live rate-limit data is queried from /api/rate-limits. Full request/token analytics require a supported telemetry endpoint.", listOf("Rate limits: ${if (rateLimits != null) "Connected" else "—"}", "Requests: server telemetry required", "Tokens: server telemetry required"))

@Composable
private fun SettingsScreen(status: RuntimeStatus, modifier: Modifier = Modifier) = PlaceholderScreen(modifier, "Settings", "Runtime and security controls.", listOf("OmniRoute runtime: managed", "Version: 3.8.49", "Gateway: 127.0.0.1:${status.port}", "Credentials: Android Keystore"))

@Composable
private fun PlaceholderScreen(modifier: Modifier, title: String, description: String, rows: List<String>) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodyLarge)
        rows.forEach { row -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(row, style = MaterialTheme.typography.bodyLarge) } } }
        HorizontalDivider()
    }
}

private fun gatewayLabel(state: RuntimeState): String = when (state) {
    RuntimeState.RUNNING -> "● RUNNING"
    RuntimeState.DEGRADED -> "● DEGRADED"
    RuntimeState.INSTALLING -> "INSTALLING"
    RuntimeState.STARTING -> "STARTING"
    RuntimeState.STOPPING -> "STOPPING"
    RuntimeState.RECOVERING -> "RECOVERING"
    RuntimeState.FAILED -> "FAILED"
    RuntimeState.UNAVAILABLE -> "STOPPED"
}
