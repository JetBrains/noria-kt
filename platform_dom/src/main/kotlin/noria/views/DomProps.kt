package noria.views

import noria.Event
import noria.NElement
import noria.HostProps
import noria.RenderContext

class DomEvent : Event()

class ChangeEvent(val newValue: Any?) : Event()

open class DomProps : HostProps(), RenderContext {
    var style: String by value()
    var children : MutableList<NElement<*>> by elementList()


    // Events
    var click by handler<DomEvent>()
    var input by handler<ChangeEvent>()
    var change by handler<ChangeEvent>()

    override fun <T> reify(e: NElement<T>): NElement<T> {
        error("Should only be called on top level in render function")
    }

    override fun <T> emit(e: NElement<T>) {
        children.add(e)
    }
}

class InputProps : DomProps() {
    var type: String by value()
    var value: String by value()
    var checked: Boolean by value()
    var disabled: Boolean by value()
}

/*
fun <T: DomProps> RenderContext.tag(t: HostComponentType<T>, build: T.() -> Unit) {
    t with
}*/
