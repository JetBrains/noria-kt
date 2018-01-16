package noria.views

import noria.*
import noria.components.*

class TextNodeProps : HostProps() {
    var text: String by value()
}

val textNodeCT = HostComponentType<TextNodeProps>("textnode")

fun RenderContext.text(value: String, key: String? = null) = x(textNodeCT, key) {
    text = value
}

class Label(p: LabelProps) : View<LabelProps>(p) {
    override fun RenderContext.render() {
        text(props.text)
    }
}
