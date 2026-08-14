package online.omniroute.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.omniroute.mobile.runtime.RuntimePolicy
import online.omniroute.mobile.runtime.RuntimeState
import online.omniroute.mobile.runtime.RuntimeViewModel
import online.omniroute.mobile.runtime.RuntimeViewModelFactory
import online.omniroute.mobile.runtime.TermuxIntentCommandRunner
import online.omniroute.mobile.runtime.TermuxRuntimeManager

class MainActivity : ComponentActivity() {
    private val runtimeViewModel: RuntimeViewModel by viewModels {
        RuntimeViewModelFactory(
            TermuxRuntimeManager(
                runner = TermuxIntentCommandRunner(applicationContext),
                policy = RuntimePolicy(),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniRouteScreen(runtimeViewModel) }
    }
}

@Composable
private fun OmniRouteScreen(viewModel: RuntimeViewModel) {
    val status by viewModel.status.collectAsStateWithLifecycle()

    MaterialTheme {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("OmniRoute", style = MaterialTheme.typography.headlineMedium)
                Text("Native Android runtime", style = MaterialTheme.typography.bodyLarge)

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gateway", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(status.state.name)
                        status.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = status.state !in setOf(RuntimeState.INSTALLING, RuntimeState.STARTING, RuntimeState.STOPPING),
                                onClick = viewModel::installAndStart,
                            ) { Text("Start") }
                            Button(
                                enabled = status.state == RuntimeState.RUNNING || status.state == RuntimeState.DEGRADED,
                                onClick = viewModel::stop,
                            ) { Text("Stop") }
                        }
                    }
                }

                Text("Runtime: dedicated Termux workspace", style = MaterialTheme.typography.bodyMedium)
                Text("Gateway: 127.0.0.1:20128", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
