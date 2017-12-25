package noria.views

import org.jetbrains.noria.Event
import org.jetbrains.noria.NElement
import org.jetbrains.noria.HostProps

class DomEvent : Event() {

}

open class DomProps : HostProps() {
    var style: String by value()
    var children : MutableList<NElement<*>> by elementList()


    // Events
    var click by handler<DomEvent>()
}

class InputProps : DomProps() {
    var type: String by value()
    var value: String by value()
    var disabled: String by value()
}
