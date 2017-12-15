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
