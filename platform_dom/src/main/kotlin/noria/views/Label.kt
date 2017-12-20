package noria.views

import org.jetbrains.noria.LabelProps
import org.jetbrains.noria.NElement
import org.jetbrains.noria.PrimitiveProps
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.View
import org.jetbrains.noria.with

class TextNodeProps : PrimitiveProps() {
    var text: String by value()
}

class Label : View<LabelProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "textnode" with TextNodeProps().apply {
            text = props.text
        }
    }
}
