package noria.views

import org.jetbrains.noria.*

class TextNodeProps : HostProps() {
    var text: String by value()
}

val textNodeCT = HostComponentType<TextNodeProps>("textnode")

class Label : View<LabelProps>() {
    override fun RenderContext.render(): NElement<*> {
        return textNodeCT with TextNodeProps().apply {
            text = props.text
        }
    }
}
