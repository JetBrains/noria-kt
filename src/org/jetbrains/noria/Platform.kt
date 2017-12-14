package org.jetbrains.noria

import kotlin.reflect.KClass

fun <N: View<P>, P: Props> instantiateNode(kClass: KClass<N>) : N = kClass.constructors.single().call()

fun renderVBox(props: BoxProps): VBox = TODO("not implemented")
fun renderHBox(props: BoxProps): HBox = TODO("not implemented")
