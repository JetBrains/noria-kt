package org.jetbrains.noria

data class ButtonProps(
        val title: String,
        val disabled: Boolean,
        val action : () -> Unit
)

fun RenderContext.button(title: String, disabled: Boolean = false, action : () -> Unit) {
    x(buttonCT, ButtonProps(title, disabled, action))
}
