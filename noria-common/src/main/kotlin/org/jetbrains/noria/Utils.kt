package org.jetbrains.noria


private val map = mutableMapOf<String, String>()

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
