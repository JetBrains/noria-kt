package org.jetbrains.noria

import kotlin.reflect.*

class FBProps : Props()
class FB : View<FBProps>() {
    override fun RenderContext.render(): NElement<*> {
        TODO("not implemented")
    }
}

actual fun <T : View<*>> KClass<T>.instantiate(): T {
    val cons = js
    return js("new cons()")
}
