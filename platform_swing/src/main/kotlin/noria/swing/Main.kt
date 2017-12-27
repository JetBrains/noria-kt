package noria.swing

import noria.*
import noria.components.*
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
        label("Hello world")
    }

    frame.contentPane = content
    frame.pack()
    frame.isVisible = true
}
