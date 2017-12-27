package noria.swing.components

import noria.*
import noria.components.*

class LabelNodeProps : HostProps() {
    var text: String by value()
}

val labelNodeCT = HostComponentType<LabelNodeProps>("JLabel")

class Label : View<LabelProps>() {
    override fun RenderContext.render() {
        x(labelNodeCT) {
            text = props.text
        }
    }
}
