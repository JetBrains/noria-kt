package noria.components

import noria.*

open class ContainerProps(val children: MutableList<NElement<*>> = mutableListOf()) : RenderContext {
    override fun <T> reify(e: NElement<T>): NElement<T> {
        error("Should only be called on top-level in render function")
    }

    override fun <T> emit(e: NElement<T>) {
        children += e
    }
}

abstract class Container<T: ContainerProps>(props: T) : View<T>(props)

