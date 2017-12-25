package noria.views

import org.jetbrains.noria.HostComponentType
import org.jetbrains.noria.NElement
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.RootProps
import org.jetbrains.noria.View
import org.jetbrains.noria.with

val Root = HostComponentType<DomProps>("root")

class DOMRootProps : DomProps() {
    var id: String by value(true)
}

class DOMRoot : View<RootProps>() {
    override fun RenderContext.render(): NElement<*> {
        return Root with DOMRootProps().apply {
            id = props.id
            children.add(props.child)
        }
    }
}
