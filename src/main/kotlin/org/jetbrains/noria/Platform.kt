package org.jetbrains.noria

import kotlin.reflect.*

fun renderVBox(props: BoxProps): NElement<*> = TODO("not implemented")
fun renderHBox(props: BoxProps): NElement<*> = TODO("not implemented")

expect fun <T:View<*>> KClass<T>.instantiate(): T
