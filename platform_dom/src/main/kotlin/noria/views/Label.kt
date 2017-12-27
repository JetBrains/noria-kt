package noria.views

import org.jetbrains.noria.*
import org.jetbrains.noria.components.*

class TextNodeProps : HostProps() {
    var text: String by value()
}

val textNodeCT = HostComponentType<TextNodeProps>("textnode")

fun RenderContext.text(value: String, key: String? = null) = x(textNodeCT, key) {
    text = value
}

class Label : View<LabelProps>() {
    override fun RenderContext.render() {
        text(props.text)
    }
}
