package noria.swing

import noria.*
import noria.components.*
import noria.demo.*
import javax.swing.*

fun main(args: Array<String>) {
    val frame = JFrame()

    lateinit var c: GraphState
    val driver = SwingDriver(events = {
        c.handleEvent(it)
    })

    val content = JPanel()

    driver.registerRoot("app", content)
    c = GraphState(SwingPlatform, driver)

    c.mount("app") {
        x(::DemoAppComponent, DemoAppProps())

/*
        vbox {
            hbox {
                label("L1")
                label("L2")
                label("L3")
            }
            hbox {
                label("K1")
                label("K2")
                label("K3")
            }

            button("Click me!") {
                println("Clicked!!")
            }
        }
*/
    }

    frame.contentPane = content
    frame.pack()
    frame.isVisible = true
}
