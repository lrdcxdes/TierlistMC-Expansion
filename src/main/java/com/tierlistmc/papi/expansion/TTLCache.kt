package com.tierlistmc.papi.expansion

import java.util.concurrent.ConcurrentHashMap
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

class TTLCacheImpl<K : Any, V : Any>(
    deletePoolDelay: Duration = 5.seconds,
    deletePoolSize: Int = 1,
) : TTLCache<K, V> {
    private val cache = ConcurrentHashMap<K, V>()
    private val forDelete = ConcurrentHashMap<K, Long>()
    private val deletePool = Executors.newScheduledThreadPool(deletePoolSize)

    init {
        deletePool.scheduleWithFixedDelay(
            ::processAccumulatedTtlRemovals,
            0, deletePoolDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS,
        )
    }

    override fun get(k: K): V? {
        return synchronized(k) {
            cache[k]?.let { value ->
                forDelete[k]?.let { deleteTime ->
                    if (shouldBeDeletedNow(deleteTime)) {
                        removeTtl(k)
                        null
                    } else {
                        value
                    }
                } ?: value
            }
        }
    }

    override fun add(k: K, v: V, ttl: Duration?): V? {
        return synchronized(k) {
            cache.put(k, v).also {
                ttl?.inWholeMilliseconds?.let { ttlMillis ->
                    forDelete[k] = System.currentTimeMillis() + ttlMillis
                }
            }
        }
    }

    override fun remove(k: K): V? {
        return synchronized(k) {
            cache.remove(k)?.also {
                forDelete.remove(k)
            }
        }
    }

    private fun processAccumulatedTtlRemovals() {
        forDelete.forEach { (k, deleteTime) ->
            synchronized(k) {
                if (shouldBeDeletedNow(deleteTime)) {
                    removeTtl(k)
                }
            }
        }
    }

    private fun removeTtl(k: K) {
        forDelete.remove(k)
        cache.remove(k)
    }

    companion object {
        private fun shouldBeDeletedNow(deleteTime: Long): Boolean {
            return deleteTime < System.currentTimeMillis()
        }
    }
}