package noria

import kotlin.reflect.KClass

class PlatformComponentType<T> : ComponentType<T>

open class Platform {
    val registry: MutableMap<PlatformComponentType<*>, Constructor<*>> = mutableMapOf()
    fun<T> resolve(platformComponentType: PlatformComponentType<T>): Constructor<T> =
            (registry[platformComponentType] as? Constructor<T>)
                    ?: error("platform component type $platformComponentType is not implemented for $this platform")
    fun<T> register(platformComponentType: PlatformComponentType<T>, c: Constructor<T>) {
        registry[platformComponentType] = c
    }
}

class RootProps(val id: String, val child: NElement<*>)
val Root = PlatformComponentType<RootProps>()
