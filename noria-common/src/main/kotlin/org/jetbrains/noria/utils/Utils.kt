package org.jetbrains.noria.utils

import kotlin.reflect.*


private val map = fastStringMap<String>()

fun String.hyphenize(): String = map.getOrPut(this) {
    buildString {
        this@hyphenize.forEach {
            append(when (it) {
                in 'A'..'Z' -> "-${it.toLowerCase()}"
                else -> it
            })
        }
    }
}

expect fun <T:Any> KClass<T>.instantiate(): T
