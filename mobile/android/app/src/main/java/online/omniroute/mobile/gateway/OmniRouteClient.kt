package online.omniroute.mobile.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class OmniRouteClient(
    private val baseUrl: String,
    private val apiKeyProvider: () -> String?,
    client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val http = client

    suspend fun health(): Result<String> = get("/api/health")

    suspend fun capabilities(): Result<String> = get("/api/capabilities")

    suspend fun providers(): Result<String> = get("/api/providers")

    suspend fun models(): Result<String> = get("/v1/models")

    private suspend fun get(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder()
                .url(baseUrl.trimEnd('/') + path)
                .get()
            apiKeyProvider()?.takeIf { it.isNotBlank() }?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            http.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Gateway returned HTTP ${response.code}")
                response.body?.string() ?: ""
            }
        }
    }
}
