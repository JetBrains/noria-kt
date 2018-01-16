package noria.views

import noria.*


class DOMRootProps : DomProps() {
    var id: String by value(true)
}

class DOMRoot(p: RootProps) : View<RootProps>(p) {
    override fun RenderContext.render() {
        x(root) {
            id = props.id
            children.add(props.child)
        }
    }

    companion object {
        private val root = HostComponentType<DOMRootProps>("root")
    }
}
