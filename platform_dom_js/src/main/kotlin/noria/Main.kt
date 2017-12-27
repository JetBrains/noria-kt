package noria

import org.jetbrains.noria.JustifyContent
import org.jetbrains.noria.GraphState
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.View
import org.jetbrains.noria.button
import org.jetbrains.noria.hbox
import org.jetbrains.noria.label
import org.jetbrains.noria.vbox
import org.jetbrains.noria.x
import kotlin.browser.document

class AppProps
class AppComponent : View<AppProps>() {
    var counter: Int = 10

    override fun RenderContext.render() {
        vbox {
            hbox {
                justifyContent = JustifyContent.center

                button("More") {
                    counter++
                    forceUpdate()
                }

                button("Less") {
                    counter--
                    forceUpdate()
                }

                button("This one you won't click", true) {
                    
                }
            }

            hbox {
                justifyContent = JustifyContent.center
                label("Counter = ${counter}")
            }

            repeat(counter) { n ->
                hbox {
                    label("Item #${(n + 1).toString().padStart(2)}")
                }
            }
        }

    }
}

fun main(args: Array<String>) {
    lateinit var c: GraphState
    val driver = JSDriver(events = {
        c.handleEvent(it)
    })

    driver.registerRoot("app", document.getElementById("app") ?: error("There should be an 'app' element in DOM"))
    c = GraphState(DOMPlatform, driver)

    c.mount("app") {
        x(::AppComponent, AppProps())
    }
}
