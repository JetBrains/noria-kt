package org.jetbrains.noria

import kotlin.reflect.*

interface Platform {
    fun hbox() : KClass<out Container<BoxProps>>
    fun vbox() : KClass<out Container<BoxProps>>
}

expect fun <T:View<*>> KClass<T>.instantiate(): T
