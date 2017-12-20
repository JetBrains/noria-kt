package org.jetbrains.noria

data class ButtonProps(
        val title: String,
        val disabled: Boolean,
        val action : () -> Unit
) : Props()

fun RenderContext.button(title: String, disabled: Boolean = false, action : () -> Unit) = platform.button() with ButtonProps(title, disabled, action)
