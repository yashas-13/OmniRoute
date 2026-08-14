package online.omniroute.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmniRouteScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun OmniRouteScreen() {
    var status by remember { mutableStateOf("Runtime unavailable") }

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
                        Text(status)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { status = "Install/start integration pending runtime permission" }) {
                                Text("Start")
                            }
                            Button(onClick = { status = "Stop requested" }) {
                                Text("Stop")
                            }
                        }
                    }
                }

                Text("Runtime: Termux-managed OmniRoute", style = MaterialTheme.typography.bodyMedium)
                Text("Gateway: 127.0.0.1:20128", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
