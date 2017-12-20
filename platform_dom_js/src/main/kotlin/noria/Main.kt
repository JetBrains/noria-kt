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

data class AppProps(val counter: Int, val h: (Int) -> Unit) : Props()
class AppComponent : View<AppProps>() {
    override fun RenderContext.render(): NElement<*> {
        return vbox {
            +hbox {
                justifyContent = JustifyContent.center

                +button("More") {
                    console.info("1 Clicked!!!")
                    props.h(props.counter + 1)
                }

                +button("Less") {
                    console.info("2 Clicked!!!")
                    props.h(props.counter - 1)
                }
            }

            +hbox {
                justifyContent = JustifyContent.center
                +label("Counter = ${props.counter}")
            }

            repeat(props.counter) { n ->
                +hbox {
                    +label("Item #${(n + 1).toString().padStart(2)}")
                }
            }
        }

    }
}

fun main(args: Array<String>) {
    val c = ReconciliationContext(DOMPlatform)
    val driver = JSDriver(document.getElementById("app") ?: error("Check index.html. There has to be an id='app' node"),
            events = {
                c.handleEvent(it)
            })

    fun h (cnt: Int): Unit {
        driver.apply(c.reconcile(AppComponent::class with AppProps(cnt, ::h)))
    }

    val updates = c.reconcile(AppComponent::class with AppProps(10, ::h))
    driver.apply(updates)
}
