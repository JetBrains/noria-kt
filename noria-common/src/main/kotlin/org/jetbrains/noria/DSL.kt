package org.jetbrains.noria

open class ContainerProps(val children: MutableList<NElement<*>> = mutableListOf()) : Props() {
    operator fun NElement<*>.unaryPlus() {
        children += this
    }
}

abstract class Container<T: ContainerProps> : View<T>()

infix fun <T: Props> Render<T>.with(props: T) : NElement<T> = NElement.Fun(this, props)
infix fun <T: Props> Constructor<T>.with(props: T) : NElement<T> = NElement.Class(this, props)
infix fun <T: HostProps> HostComponentType<T>.with(props: T) : NElement<T> = NElement.HostElement(this, props)
infix fun <T: Props> PlatformComponentType<T>.with(props: T) : NElement<T> = NElement.PlatformDispatch(this, props)
