package org.jetbrains.noria

fun testNodes() : View<*> {
    return vbox {
        +vbox(alignItems = "center") {
            +vbox {
                +hbox {  }
                +hbox {  }
            }
        }
    }
}
