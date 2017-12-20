package org.jetbrains.noria

data class ButtonProps(
        val title: String,
        val action : () -> Unit
) : Props()

fun RenderContext.button(title: String, action : () -> Unit) = platform.button() with ButtonProps(title, action)
