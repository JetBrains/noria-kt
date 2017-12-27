package noria.views

import org.jetbrains.noria.*

val Root = HostComponentType<DOMRootProps>("root")

class DOMRootProps : DomProps() {
    var id: String by value(true)
}

class DOMRoot : View<RootProps>() {
    override fun RenderContext.render() {
        x(Root) {
            id = props.id
            children.add(props.child)
        }
    }
}
