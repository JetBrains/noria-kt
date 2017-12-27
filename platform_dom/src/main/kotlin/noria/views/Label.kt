package noria.views

import org.jetbrains.noria.*

class TextNodeProps : HostProps() {
    var text: String by value()
}

val textNodeCT = HostComponentType<TextNodeProps>("textnode")

fun RenderContext.text(value: String, key: String? = null) = x(textNodeCT, key) {
    text = value
}

class Label : View<LabelProps>() {
    override fun RenderContext.render() {
        x(textNodeCT, TextNodeProps().apply {
            text = props.text
        })
    }
}
