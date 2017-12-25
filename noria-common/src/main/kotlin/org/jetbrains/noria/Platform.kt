package org.jetbrains.noria

open class Platform {
    val registry: MutableMap<PlatformComponentType<*>, Constructor<*>> = mutableMapOf()
    fun<T: Props> resolve(platformComponentType: PlatformComponentType<T>): Constructor<T> =
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

