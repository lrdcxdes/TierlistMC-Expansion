package com.tierlistmc.papi.expansion

import com.google.gson.Gson
import okhttp3.Callback
import okhttp3.OkHttpClient
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.CompletableFuture

@Suppress("UNCHECKED_CAST")
class Api(private val config: Config) {
    private val client: OkHttpClient = OkHttpClient().newBuilder().build()
    private val gson = Gson()
    private val baseURL = "https://tierlistmc.com/papi"

    private fun getJSON(
        url: String, vararg params: Pair<String, String>
    ): CompletableFuture<Map<String, Any>> {
        val request = okhttp3.Request.Builder()
            .get()
            .addHeader("User-Agent", "TierlistMC-PAPI")
            .addHeader("Authorization", "Bearer ${config.getApiKey()}")
            .addHeader("Accept", "application/json")
            .addHeader("Accept-Language", config.getLanguage() ?: "en")
            .url(builderUrl(url, params))
            .build()
        val future = CompletableFuture<Map<String, Any>>()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    println("Failed to get JSON from $url")
                    e.printStackTrace()
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        val json = response.body!!.string()
                        val map = gson.fromJson(json, Map::class.java)
                        future.complete(map as Map<String, Any>)
                    } catch (e: Exception) {
                        println("Failed to get JSON from $url")
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