package noria.demo

import noria.*
import noria.components.*

class DemoAppProps
class DemoAppComponent : View<DemoAppProps>() {
    var counter: Int by managedState(10)
    var name: String by managedState("")
    var buttonsEnabled: Boolean by managedState(true)

    override fun RenderContext.render() {
        vbox {
            hbox {
                justifyContent = JustifyContent.center

                checkbox("Enable Buttons", ::buttonsEnabled)

                button("More", !buttonsEnabled) {
                    counter++
                }

                button("Less", !buttonsEnabled) {
                    counter--
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
