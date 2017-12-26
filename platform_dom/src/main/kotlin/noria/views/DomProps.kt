package noria.views

import org.jetbrains.noria.Event
import org.jetbrains.noria.HostComponentType
import org.jetbrains.noria.NElement
import org.jetbrains.noria.HostProps
import org.jetbrains.noria.Props
import org.jetbrains.noria.RenderContext

class DomEvent : Event() {

}

open class DomProps : HostProps(), RenderContext {
    var style: String by value()
    var children : MutableList<NElement<*>> by elementList()


    // Events
    var click by handler<DomEvent>()

    override fun <T : Props> reify(e: NElement<T>): NElement<T> {
        error("Should only be called on top level in render function")
    }

    override fun <T : Props> emit(e: NElement<T>): NElement<T> {
        children.add(e)
        return e
    }
}

class InputProps : DomProps() {
    var type: String by value()
    var value: String by value()
    var disabled: String by value()
}

/*
fun <T: DomProps> RenderContext.tag(t: HostComponentType<T>, build: T.() -> Unit) {
    t with
}*/
