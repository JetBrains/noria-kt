package org.jetbrains.noria.components

import org.jetbrains.noria.*

val Button = PlatformComponentType<ButtonProps>()

data class ButtonProps(
        val title: String,
        val disabled: Boolean,
        val action : () -> Unit
)

fun RenderContext.button(title: String, disabled: Boolean = false, action : () -> Unit) {
    x(Button, ButtonProps(title, disabled, action))
}
