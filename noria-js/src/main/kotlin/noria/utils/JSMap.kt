package noria.utils

import kotlin.reflect.KClass

actual fun <V> fastStringMap(): MutableMapLike<String, V> = JsPlainMapES6Impl()
actual fun <V> fastIntMap(): MutableMapLike<Int, V> = JsPlainMapES6Impl()

actual fun <T:Any> KClass<T>.instantiate(): T {
    val cons = js
    return js("new cons()") as T
}

private class JsPlainMapES6Impl<K : Any, V> : MutableMapLike<K, V> {

    override fun size(): Int {
        return storage.size
    }

    private val storage = js("new Map()")

    override fun containsKey(key: K): Boolean {
        return storage.has(key)
    }

    override fun remove(key: K) {
        storage.delete(key)
    }

    override fun get(key: K): V? {
        return storage.get(key)
    }

    override fun set(key: K, v: V) {
        storage.set(key, v)
    }

    override fun put(key: K, v: V) {
        storage.set(key, v)
    }

    override fun forEach(handler: (K, V) -> Unit) {
        storage.forEach { v: V, k: K ->
            handler(k, v)
        }
    }

    override fun clear() {
        storage.clear()
    }
}
