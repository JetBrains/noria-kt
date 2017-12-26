package noria.views

import org.jetbrains.noria.HostComponentType
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.RootProps
import org.jetbrains.noria.View

val Root = HostComponentType<DomProps>("root")

class DOMRootProps : DomProps() {
    var id: String by value(true)
}

class DOMRoot : View<RootProps>() {
    override fun RenderContext.render() {
        Root with DOMRootProps().apply {
            id = props.id
            children.add(props.child)
        }
    }
}
