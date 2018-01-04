package noria.demo

import noria.*
import noria.components.*

class DemoAppProps
class DemoAppComponent : View<DemoAppProps>() {
    var counter: Int by managedState(10)
    var name: String by managedState("")

    override fun RenderContext.render() {
        vbox {
            hbox {
                justifyContent = JustifyContent.center

                button("More") {
                    counter++
                }

                button("Less") {
                    counter--
                }

                button("This one you won't click unless counter is odd", counter % 2 == 0) {

                }
            }

            hbox {
                label("Name?")
                textField(::name)
                if (name.isNotBlank()) {
                    label("Nice to meet you, $name")
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
