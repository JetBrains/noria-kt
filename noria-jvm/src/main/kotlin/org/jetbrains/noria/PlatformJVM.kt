package org.jetbrains.noria

import kotlin.reflect.KClass

actual fun <T : View<*>> KClass<T>.instantiate(): T {
    return constructors.single().call()
}
