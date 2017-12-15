package org.jetbrains.noria

fun testNodes() : NElement<*> {

    ::link with LinkProps("")
//    "div" with LinkProps("")
    HBox::class with BoxProps()
    
    return vbox {
        +vbox(alignItems = "center") {
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


class MyMacComponent: View<Props>() {
    override fun RenderContext.render(): NElement<*> {
        val v1 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
        }
        val v2 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
        }

        emit("NSLayoutConstraint" with NSConstraint().apply {
            view1 = v1
            view2 = v2
        })

        return "NSView" with NSViewProps().apply {
            subviews.add(v1)
            subviews.add(v2)
        }
    }

}
