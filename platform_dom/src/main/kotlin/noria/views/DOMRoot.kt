package noria.views

import org.jetbrains.noria.HostComponentType
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.RootProps
import org.jetbrains.noria.View
import org.jetbrains.noria.x

val Root = HostComponentType<DomProps>("root")

class DOMRootProps : DomProps() {
    var id: String by value(true)
}

class DOMRoot : View<RootProps>() {
    override fun RenderContext.render() {
        x(Root, DOMRootProps().apply {
            id = props.id
            children.add(props.child)
        })
    }
}
