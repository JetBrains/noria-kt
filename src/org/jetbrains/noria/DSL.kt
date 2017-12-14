package org.jetbrains.noria

interface Props

abstract class View<T: Props> {
    lateinit var props: T

    open fun shouldUpdate(newProps: T): Boolean = props != newProps
    abstract fun render(): View<*>
}

open class ContainerProps : Props {
    val children = mutableListOf<View<*>>()

    operator fun View<*>.unaryPlus() {
        children += this
    }
}

abstract class Container<T: ContainerProps> : View<T>()

inline fun <reified N : View<P>, P: Props> node(props: P) : N {
    return instantiateNode(N::class).apply {
        this.props = props
    }
}

inline fun <reified N : View<P>, P: Props> node(props: P, builder: P.() -> Unit) : N {
    return instantiateNode(N::class).apply {
        props.builder()
        this.props = props
    }
}
