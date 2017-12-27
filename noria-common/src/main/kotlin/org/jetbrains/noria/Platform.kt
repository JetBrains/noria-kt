package org.jetbrains.noria

open class Platform {
    val registry: MutableMap<PlatformComponentType<*>, Constructor<*>> = mutableMapOf()
    fun<T> resolve(platformComponentType: PlatformComponentType<T>): Constructor<T> =
            (registry[platformComponentType] as? Constructor<T>)
                    ?: error("platform component type $platformComponentType is not implemented for $this platform")
    fun<T: Props> register(platformComponentType: PlatformComponentType<T>, c: Constructor<T>) {
        registry[platformComponentType] = c
    }
}

class RootProps(val id: String, val child: NElement<*>) : Props()
val rootCT = PlatformComponentType<RootProps>()
val hboxCT = PlatformComponentType<BoxProps>()
val vboxCT = PlatformComponentType<BoxProps>()
val labelCT = PlatformComponentType<LabelProps>()
val buttonCT = PlatformComponentType<ButtonProps>()

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

val <K: Any> MutableMapLike<K, *>.keys : Set<K> get() = mutableSetOf<K>().apply {
    forEach { k, _ -> add(k) }
}
