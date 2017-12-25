package noria

import org.jetbrains.noria.JustifyContent
import org.jetbrains.noria.NElement
import org.jetbrains.noria.Props
import org.jetbrains.noria.ReconciliationContext
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.View
import org.jetbrains.noria.button
import org.jetbrains.noria.hbox
import org.jetbrains.noria.label
import org.jetbrains.noria.vbox
import org.jetbrains.noria.with
import kotlin.browser.document

object AppProps : Props()
class AppComponent : View<AppProps>() {
    var counter: Int = 10

    override fun RenderContext.render(): NElement<*> {
        return vbox {
            +hbox {
                justifyContent = JustifyContent.center

                +button("More") {
                    counter++
                    forceUpdate()
                }

                +button("Less") {
                    counter--
                    forceUpdate()
                }

                +button("This one you won't click", true) {
                    
                }
            }

            +hbox {
                justifyContent = JustifyContent.center
                +label("Counter = ${counter}")
            }

            repeat(counter) { n ->
                +hbox {
                    +label("Item #${(n + 1).toString().padStart(2)}")
                }
            }
        }

    }
}

fun main(args: Array<String>) {
    lateinit var c: ReconciliationContext
    val driver = JSDriver(document.getElementById("app") ?: error("Check index.html. There has to be an id='app' node"),
            events = {
                c.handleEvent(it)
            })

    c = ReconciliationContext(DOMPlatform, driver)
    c.reconcile(::AppComponent with AppProps)
}
