package noria

import noria.demo.*
import kotlin.browser.*

fun main(args: Array<String>) {
    lateinit var c: GraphState
    val driver = JSDriver(events = {
        c.handleEvent(it)
    })

    driver.registerRoot("app", document.getElementById("app") ?: error("There should be an 'app' element in DOM"))
    c = GraphState(DOMPlatform, driver)

    c.mount("app") {
        x(::DemoAppComponent, DemoAppProps())
    }
}
