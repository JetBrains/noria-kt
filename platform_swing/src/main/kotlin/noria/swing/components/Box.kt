package noria.swing.components

import noria.*
import noria.components.*
import noria.components.Container
import noria.swing.*
import java.awt.*

class PanelProps : HostProps() {
    var flex by value<BoxProps>()
    var children by elementList<MutableList<NElement<*>>>()
}

val Panel = HostComponentType<PanelProps>(NPanel::class.qualifiedName!!)

class FlexBox(props: BoxProps) : Container<BoxProps>(props) {
    override fun RenderContext.render() {
        x(Panel) {
            flex = props
            children.addAll(props.children)
        }
    }
}
