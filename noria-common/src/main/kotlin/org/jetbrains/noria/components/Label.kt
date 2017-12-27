package org.jetbrains.noria.components

import org.jetbrains.noria.*

data class LabelProps(val text: String)

val Label = PlatformComponentType<LabelProps>()
fun RenderContext.label(text: String) {
    x(Label, LabelProps(text))
}
