package org.jetbrains.noria

fun RenderContext.testNodes() {

    ::link with LinkProps("")
//    "div" with LinkProps("")

    vbox {
        vbox {
            alignItems = Align.center
            
            vbox {
                hbox {  }
                hbox {  }
            }
        }
    }
}

data class LinkProps(val href: String) : Props()

fun link(props: LinkProps) : NElement<*> {
    TODO()
}
