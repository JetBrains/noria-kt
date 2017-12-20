package org.jetbrains.noria

import kotlin.reflect.*

interface Platform {
    fun hbox() : KClass<out Container<BoxProps>>
    fun vbox() : KClass<out Container<BoxProps>>
    fun label() : KClass<out View<LabelProps>>
    fun button() : KClass<out View<ButtonProps>>
}

expect fun <T:View<*>> KClass<T>.instantiate(): T
