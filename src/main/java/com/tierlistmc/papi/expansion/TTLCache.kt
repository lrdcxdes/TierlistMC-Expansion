package com.tierlistmc.papi.expansion

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface TTLCache<K : Any, V : Any> {
    fun get(k: K): V?
    fun add(k: K, v: V): V? = add(k, v, ttl = null)
    fun add(k: K, v: V, ttl: Duration?): V?
    fun remove(k: K): V?
}

class TTLCacheImpl(
    deletePoolDelay: Duration = 5.seconds,
    deletePoolSize: Int = 1,
) : TTLCache<String, Map<String, Any?>> {
    private val cache = ConcurrentSkipListMap<String, Map<String, Any?>>()
    private val forDelete = ConcurrentSkipListMap<String, Long>()
    private val deletePool = Executors.newScheduledThreadPool(deletePoolSize)

    init {
        deletePool.scheduleAtFixedRate(
            ::processAccumulatedTtlRemovals,
            0, deletePoolDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS,
        )
    }

    override fun get(k: String): Map<String, Any?>? {
        return cache.computeIfPresent(k) { _, value ->
            forDelete[k]?.let { deleteTime ->
                if (shouldBeDeletedNow(deleteTime)) {
                    forDelete.remove(k)
                    null
                } else {
                    value
                }
            } ?: value
        }
    }

    override fun add(k: String, v: Map<String, Any?>, ttl: Duration?): Map<String, Any?>? {
        return cache.put(k, v).also {
            ttl?.inWholeMilliseconds?.let { ttlMillis ->
                forDelete[k] = System.currentTimeMillis() + ttlMillis
            }
        }
    }

    override fun remove(k: String): Map<String, Any?>? {
        forDelete.remove(k)
        return cache.remove(k)
    }

    private fun processAccumulatedTtlRemovals() {
        forDelete.forEach { (k, deleteTime) ->
            if (shouldBeDeletedNow(deleteTime)) {
                remove(k)
            }
        }
    }

    companion object {
        private fun shouldBeDeletedNow(deleteTime: Long): Boolean {
            return deleteTime < System.currentTimeMillis()
        }
    }
}