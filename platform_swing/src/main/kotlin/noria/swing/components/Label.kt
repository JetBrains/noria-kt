package noria.swing.components

import noria.*
import noria.components.*
import javax.swing.*

val nLabel = beanHostCompnentType<JLabel>()

class Label : View<LabelProps>() {
    override fun RenderContext.render() {
        x(nLabel) {
            set(JLabel::setText, props.text)
        }
    }
}
