package org.jetbrains.noria

import kotlin.reflect.*

interface Platform {
    fun hbox() : Constructor<BoxProps>
    fun vbox() : Constructor<BoxProps>
    fun label() : Constructor<LabelProps>
    fun button() : Constructor<ButtonProps>
}

