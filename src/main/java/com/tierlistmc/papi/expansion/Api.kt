package com.tierlistmc.papi.expansion

import kotlinx.serialization.json.Json
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

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

    private fun getJSON(
        url: String, vararg params: Pair<String, String>
    ): CompletableFuture<Map<String, Any?>> {
        val request = okhttp3.Request.Builder()
            .get()
            .url(builderUrl(url, params))
            .build()
        val future = CompletableFuture<Map<String, Any?>>()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    logger.warning("Failed to get JSON from $url")
                    e.printStackTrace()
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        val json = response.body?.string() ?: return
                        val map = Json.decodeFromString<Map<String, Any?>>(json)
                        future.complete(map)
                    } catch (e: Exception) {
                        logger.warning("Failed to parse JSON from $url")
                        e.printStackTrace()
                        future.completeExceptionally(e)
                    }
                }
            }
        )
        return future
    }

    private fun builderUrl(url: String, params: Array<out Pair<String, String>>): URL {
        val builder = StringBuilder(url)
        if (params.isNotEmpty()) {
            builder.append("?")
            params.forEachIndexed { index, pair ->
                builder.append(pair.first)
                builder.append("=")
                builder.append(URLEncoder.encode(pair.second, "UTF-8"))
                if (index != params.size - 1) {
                    builder.append("&")
                }
            }
        }
        return URL(builder.toString())
    }

    @Suppress("UNCHECKED_CAST")
    fun playerCurrentTierData(username: String, tierType: String): CompletableFuture<Map<String, Any?>?> {
        val future = getJSON("$baseURL/player/$username", "tier_type" to tierType)
        return future.thenApply { map ->
            val data = map["data"] as Map<String, Any>? ?: return@thenApply null

            data["tier"] as Map<String, Any?>? ?: return@thenApply null
        }
    }

    fun test(): Boolean {
        val future = getJSON("$baseURL/test")
        return try {
            val data = future.get()
            data["status"] as String == "success"
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}