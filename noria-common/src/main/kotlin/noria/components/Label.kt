package noria.components

import noria.*

data class LabelProps(val text: String) : PlatformProps()

val Label = PlatformComponentType<LabelProps>()
fun RenderContext.label(text: String, b: LabelProps.() -> Unit = {}) {
    x(Label, LabelProps(text).apply(b))
}
