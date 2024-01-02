package com.tierlistmc.papi.expansion

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

@Serializable
data class Tier(
    val name: String,
    val id: Int? = null,
)

@Serializable
data class Player(
    val username: String,
    val tiers: Map<String, Tier>
)

@Serializable
data class BatchResponse(
    val status: String,
    val message: String? = null,
    val result: List<Player>? = null
)

@Serializable
data class Batch(val batch: List<String>)

class Api(private val config: Config, private val logger: Logger) {
    // TODO: Batch requests

    private val client: OkHttpClient =
        OkHttpClient().newBuilder()
            .readTimeout(config.getReadTimeout(), java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(config.isRetryOnConnectionFailure())
            .connectTimeout(config.getConnectTimeout(), java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(
                ConnectionPool(
                    config.getMaxIdleConnections(),
                    config.getKeepAliveDuration(),
                    java.util.concurrent.TimeUnit.SECONDS
                )
            )
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "TierlistMC-PAPI")
                    .addHeader("Authorization", "Bearer ${config.getApiKey()}")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", config.getLanguage() ?: "en")
                    .build()
                chain.proceed(request)
            }
            .build()
    private val baseURL = "https://tierlistmc.com/papi"

    fun batchRequest(batches: Batch): CompletableFuture<List<Player>> {
        val future = CompletableFuture<List<Player>>()
        val request = okhttp3.Request.Builder()
            .post(
                Json.encodeToString(batches)
                    .toRequestBody("application/json".toMediaTypeOrNull())
            )
            .url("$baseURL/batch")
            .build()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    logger.warning("Failed to get JSON from $baseURL/batch")
                    e.printStackTrace()
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        val json = response.body?.string() ?: return
                        val batch = Json.decodeFromString<BatchResponse>(json)
                        if (batch.status != "success") {
                            logger.warning("Failed to get JSON from $baseURL/batch")
                            logger.warning(batch.message ?: "Unknown error")
                            future.completeExceptionally(Exception(batch.message))
                            return
                        }
                        future.complete(batch.result!!)
                    } catch (e: Exception) {
                        logger.warning("Failed to parse JSON from $baseURL/batch")
                        e.printStackTrace()
                        future.completeExceptionally(e)
                    }
                }
            }
        )
        return future
    }

    fun test(): Boolean {
        val request = okhttp3.Request.Builder()
            .get()
            .url("$baseURL/test")
            .build()
        return try {
            val data = client.newCall(request).execute().body?.string() ?: return false
            data.contains("success")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}