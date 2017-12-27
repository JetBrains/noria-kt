package org.jetbrains.noria

data class LabelProps(val text: String)

fun RenderContext.label(text: String) {
    x(labelCT, LabelProps(text))
}
