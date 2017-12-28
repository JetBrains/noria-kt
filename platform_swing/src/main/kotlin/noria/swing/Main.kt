package noria.swing

import noria.*
import noria.demo.*
import javax.swing.*

fun main(args: Array<String>) {
    val content = JPanel()

    GraphState(SwingPlatform, SwingDriver().apply {
        registerRoot("app", content)
    }).apply {
        mount("app") {
            x(::DemoAppComponent, DemoAppProps())
        }
    }

    JFrame().apply {
        contentPane = content
        pack()
        isVisible = true
    }
}
