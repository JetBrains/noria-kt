package org.jetbrains.noria

open class ContainerProps(val children: MutableList<NElement<*>> = mutableListOf()) : Props(), RenderContext {
    override fun <T : Props> reify(e: NElement<T>): NElement<T> {
        error("Should only be called on top-level in render function")
    }

    override fun <T : Props> emit(e: NElement<T>): NElement<T> {
        children += e
        return e
    }
}

abstract class Container<T: ContainerProps> : View<T>()

