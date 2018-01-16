package noria.demo

import noria.*
import noria.components.*

data class Item(val desc: String, val completed: Boolean)

class DemoAppProps
class DemoAppComponent(p: DemoAppProps) : View<DemoAppProps>(p) {
    var items: List<Item> by managedState(listOf(Item("first", true),Item("second", true),Item("third", false) ))
    var newItemText: String by managedState("")

    var counter: Int by managedState(10)
    var name: String by managedState("")
    var buttonsEnabled: Boolean by managedState(true)

    override fun RenderContext.render() {
        vbox {
            textField(::newItemText)

            for (item in items) {
                x(::ItemComponent, item)
            }

/*
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
*/
        }

    }
}

class ItemComponent(props: Item) : View<Item>(props) {
    var completed : Boolean by managedState(props.completed)

    override fun RenderContext.render() {
        println("Render $props. Completed = $completed")
        hbox {
            checkbox("", ::completed)
            label(props.desc)
        }
    }
}
