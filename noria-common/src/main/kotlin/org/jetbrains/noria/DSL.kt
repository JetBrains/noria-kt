package org.jetbrains.noria

open class ContainerProps(val children: MutableList<NElement<*>> = mutableListOf()) : Props(), RenderContext {
    override fun <T> reify(e: NElement<T>): NElement<T> {
        error("Should only be called on top-level in render function")
    }

    override fun <T> emit(e: NElement<T>) {
        children += e
    }
}

abstract class Container<T: ContainerProps> : View<T>()

