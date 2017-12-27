package noria.swing.components

import noria.*

class SwingRootProps : HostProps() {
    var id: String by value(true)
    val children: MutableList<NElement<*>> by elementList()
}

class SwingRoot : View<RootProps>() {
    override fun RenderContext.render() {
        x(root) {
            id = props.id
            children.add(props.child)
        }
    }

    companion object {
        private val root = HostComponentType<SwingRootProps>("root")
    }
}
