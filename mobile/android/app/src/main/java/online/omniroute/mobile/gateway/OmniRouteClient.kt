package online.omniroute.mobile.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OmniRouteClient(
    private val baseUrl: String,
    private val apiKeyProvider: () -> String?,
    client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build(),
) {
    private val http = client

    suspend fun snapshot(): Result<GatewaySnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val models = get("/v1/models")
            val resilience = getOptional("/api/resilience")
            val rateLimits = getOptional("/api/rate-limits")
            GatewaySnapshot(
                models = parseModels(models),
                resilience = resilience,
                rateLimits = rateLimits,
            )
        }
    }

    suspend fun models(): Result<String> = get("/v1/models")

    suspend fun resilience(): Result<String> = get("/api/resilience")

    suspend fun rateLimits(): Result<String> = get("/api/rate-limits")

    private suspend fun getOptional(path: String): String? = runCatching { get(path).getOrThrow() }.getOrNull()

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

    private fun parseModels(body: String): ModelSummary {
        val json = JSONObject(body)
        val data = json.optJSONArray("data")
        var count = 0
        if (data != null) count = data.length()
        return ModelSummary(count = count)
    }
}

data class GatewaySnapshot(
    val models: ModelSummary,
    val resilience: String?,
    val rateLimits: String?,
)

data class ModelSummary(val count: Int)
