package org.jetbrains.noria

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

actual fun <V> fastStringMap(): MutableMapLike<String, V> = JvmMapLike()
actual fun <V> fastIntMap(): MutableMapLike<Int, V> = JvmMapLike()

actual fun <T:Any> KClass<T>.instantiate(): T {
    return primaryConstructor?.call() ?: error("Primary constructor missing")
}

private class JvmMapLike<K: Any, V>() : MutableMapLike<K, V> {

    private val map: MutableMap<K, V> = mutableMapOf()
    override fun size() = map.size

    override fun remove(key: K) {
        map.remove(key)
    }

    override fun put(key: K, v: V) {
        map[key] = v
    }

    override fun forEach(handler: (K, V) -> Unit) {
        map.forEach {
            handler.invoke(it.key, it.value)
        }
    }

    override fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    override fun get(key: K): V? {
        return map[key]
    }

    override fun set(key: K, v: V) {
        map[key] = v
    }

    override fun toString(): String = map.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JvmMapLike<*, *>) return false

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }
}
