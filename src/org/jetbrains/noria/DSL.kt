package org.jetbrains.noria

import kotlin.reflect.KClass

abstract class Props {
    var key: Any? = null
}

abstract class View<T: Props> {
    lateinit var props: T

    open fun shouldUpdate(newProps: T): Boolean = props != newProps
    abstract fun render(): NElement<*>
}

typealias Render<T> = (T) -> NElement<*>

sealed class NElement<T: Props>(val props: T) {
    internal class Fun<T: Props>(val f: Render<T>, props: T) : NElement<T>(props)
    internal class Class<T: Props>(val kClass: KClass<*>, props: T) : NElement<T>(props)
    internal class Primitive<T: Props>(val name: String, props: T) : NElement<T>(props)
}

open class ContainerProps : Props() {
    val children = mutableListOf<NElement<*>>()

    operator fun NElement<*>.unaryPlus() {
        children += this
    }
}

abstract class Container<T: ContainerProps> : View<T>()

infix fun <T: Props> Render<T>.with(props: T) : NElement<T> = NElement.Fun(this, props)
infix fun <V: View<T>, T: Props> KClass<V>.with(props: T) : NElement<T> = NElement.Class(this, props)
infix fun <T: Props> String.with(props: T) : NElement<T> = NElement.Primitive(this, props)
