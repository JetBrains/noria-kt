package org.jetbrains.noria

fun RenderContext.testNodes() {

    x(::link, LinkProps(""))
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

data class LinkProps(val href: String)

fun link(props: LinkProps) : NElement<*> {
    TODO()
}
