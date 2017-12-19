package noria.views

import org.jetbrains.noria.NElement
import org.jetbrains.noria.PrimitiveProps

class DomProps : PrimitiveProps() {
    var style: String by value()
    var children : MutableList<NElement<*>> by elementList()
}
