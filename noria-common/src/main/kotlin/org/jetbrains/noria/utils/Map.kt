package org.jetbrains.noria.utils


interface MutableMapLike<K : Any, V> {
    operator fun get(key: K): V?
    fun put(key: K, v: V)
    operator fun set(key: K, v: V)
    fun remove(key: K)
    fun containsKey(key: K): Boolean
    fun forEach(handler: (K, V) -> Unit)
    fun size(): Int
}

expect fun <V> fastStringMap(): MutableMapLike<String, V>
expect fun <V> fastIntMap(): MutableMapLike<Int, V>

inline fun <K : Any, V> MutableMapLike<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        value
    }
}
