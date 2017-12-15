package org.jetbrains.noria

import kotlin.reflect.KClass


open class ContainerProps(val children: MutableList<NElement<*>> = mutableListOf()) : Props() {
    operator fun NElement<*>.unaryPlus() {
        children += this
    }
}

abstract class Container<T: ContainerProps> : View<T>()

infix fun <T: Props> Render<T>.with(props: T) : NElement<T> = NElement.Fun(this, props)
infix fun <V: View<T>, T: Props> KClass<V>.with(props: T) : NElement<T> = NElement.Class(this, props)
infix fun <T: PrimitiveProps> String.with(props: T) : NElement<T> = NElement.Primitive(this, props)
