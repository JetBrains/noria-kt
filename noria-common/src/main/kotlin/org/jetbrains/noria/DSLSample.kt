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
