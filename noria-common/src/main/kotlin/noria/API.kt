package noria

import noria.utils.*
import kotlin.properties.*
import kotlin.reflect.*

interface RenderContext {
    fun <T> reify(e: NElement<T>): NElement<T>
    fun <T> emit(e: NElement<T>)
}

fun <T> RenderContext.x(cons: Constructor<T>, props: T, key: String? = null) {
    emit(createViewElement(cons, props, key))
}

inline fun <reified T:Any> RenderContext.x(noinline cons: Constructor<T>, key: String? = null, build: T.() -> Unit) {
    x(cons, T::class.instantiate().apply(build), key)
}

fun <T> RenderContext.fx(render: Render<T>, props: T, key: String? = null) {
    emit(createFnElement(render, props, key))
}

inline fun <reified T:Any> RenderContext.fx(noinline render: Render<T>, key: String? = null, build: T.() -> Unit) {
    fx(render, T::class.instantiate().apply(build), key)
}

fun <T:HostProps> RenderContext.x(host: HostComponentType<T>, props: T, key: String? = null) {
    emit(createHostElement(host, props, key))
}

inline fun <reified T:HostProps> RenderContext.x(host: HostComponentType<T>, key: String? = null, build: T.() -> Unit) {
    x(host, T::class.instantiate().apply(build), key)
}

fun <T> RenderContext.x(host: PlatformComponentType<T>, props: T, key: String? = null) {
    emit(createPlatformElement(host, props, key))
}

inline fun <reified T:Any> RenderContext.x(host: PlatformComponentType<T>, key: String? = null, build: T.() -> Unit) {
    x(host, T::class.instantiate().apply(build), key)
}

fun <T> createFnElement(render: Render<T>, props: T, key: String? = null): NElement<T> = NElement.Fun(render, props, key)
fun <T> createViewElement(cons: Constructor<T>, props: T, key: String? = null) : NElement<T> = NElement.Class(cons, props, key)
fun <T: HostProps> createHostElement(hct: HostComponentType<T>, props: T, key: String? = null) : NElement<T> = NElement.HostElement(hct, props, key)
fun <T> createPlatformElement(pct: PlatformComponentType<T>, props: T, key: String? = null) : NElement<T> = NElement.PlatformDispatch(pct, props, key)

fun RenderContext.capture(build: RenderContext.() -> Unit) : NElement<*> {
    val capturingContext = RenderContextImpl()
    capturingContext.build()
    return reify(capturingContext.createdElements.single())
}

abstract class View<T>(var props: T) {
    internal lateinit var context: GraphState
    internal lateinit var instance: UserInstance

    open fun shouldUpdate(newProps: T): Boolean {
        return props != newProps
    }

    fun forceUpdate() {
        context.forceUpdate(instance)
    }

    abstract fun RenderContext.render()

    fun <V> managedState(initial: V) : ReadWriteProperty<View<T>, V> = ManagedProperty(initial)
    private class ManagedProperty<T, V>(initial: V) : ReadWriteProperty<View<T>, V> {
        private var v: V = initial

        override fun getValue(thisRef: View<T>, property: KProperty<*>): V {
            return v
        }

        override fun setValue(thisRef: View<T>, property: KProperty<*>, value: V) {
            v = value
            thisRef.forceUpdate()
        }
    }
}

typealias Render<T> = (T) -> NElement<*>
typealias Constructor<T> = (T) -> View<T>

interface ComponentType<T>

sealed class NElement<T>(val props: T, open val type: Any, open var key: String?) {
    internal class Fun<T>(override val type: Render<T>, props: T, key: String?) : NElement<T>(props, type, key)
    internal class Class<T>(override val type: Constructor<T>, props: T, key: String?) : NElement<T>(props, type, key)
    internal class HostElement<T : HostProps>(override val type: HostComponentType<T>, props: T, key: String?) : NElement<T>(props, type, key)
    internal class PlatformDispatch<T>(override val type: PlatformComponentType<T>, props: T, key: String?) : NElement<T>(props, type, key)
    internal class Reified<T>(val id: Int, val e: NElement<T>) : NElement<T>(e.props, e.type, null) {
        override var key: String?
            get() = e.key
            set(value) {e.key = value}
    }
}

abstract class Event {
    var propagate: Boolean = true
    var preventDefault: Boolean = false

    fun stopPropagation() {
        propagate = false
    }

    fun sudoPreventDefault() {
        preventDefault = true
    }
}
