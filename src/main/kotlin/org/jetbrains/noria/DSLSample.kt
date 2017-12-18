package org.jetbrains.noria

fun RenderContext.testNodes() : NElement<*> {

    ::link with LinkProps("")
//    "div" with LinkProps("")

    return vbox {
        +vbox {
            alignItems = Align.center
            
            +vbox {
                +hbox {  }
                +hbox {  }
            }
        }
    }
}


data class LinkProps(val href: String) : Props()

fun link(props: LinkProps) : NElement<*> {
    TODO()
}



class NSViewProps: PrimitiveProps() {
    val subviews: MutableList<NElement<NSViewProps>> by elementList()
}

class NSConstraint: PrimitiveProps() {
    var view1: NElement<NSViewProps> by element()
    var view2: NElement<NSViewProps> by element()
}
