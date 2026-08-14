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

    suspend fun ping(): Result<String> = get("/v1/models")

    suspend fun models(): Result<String> = get("/v1/models")

    suspend fun resilience(): Result<String> = get("/api/resilience")

    suspend fun rateLimits(): Result<String> = get("/api/rate-limits")

    private suspend fun get(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBuilder = Request.Builder()
                .url(baseUrl.trimEnd('/') + path)
                .header("Accept", "application/json")
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
