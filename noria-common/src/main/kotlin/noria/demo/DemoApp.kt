package noria.demo

import noria.*
import noria.components.*
import kotlin.properties.*

data class Item(val key: String, val desc: String, val completed: Boolean, val editing: Boolean,
                val onStartEditing: (Item) -> Unit, val onDoneEditing: (Item, String) -> Unit, val onSetCompleted: (Item, Boolean) -> Unit)

var counter: Int = 0
fun nextId(): String = (++counter).toString()

class DemoAppProps
class DemoAppComponent(p: DemoAppProps) : View<DemoAppProps>(p) {
    val items = mutableMapOf<String, Item>()
    var newItemText: String by managedState("")


    fun onNewItem(desc: String) {
        val key = nextId()
        items[key] = Item(key, desc, false, false, ::updateStarted, ::updateDescription, ::updateCompleted)
        forceUpdate()
    }

    fun updateStarted(item: Item) {
        items.values.filter { it.editing }.forEach {
            items[it.key] = it.copy(editing = false)
        }

        items[item.key] = item.copy(editing = true)
        forceUpdate()
    }

    fun updateCompleted(item: Item, completed: Boolean) {
        items[item.key] = item.copy(completed = completed)
        forceUpdate()
    }

    fun updateDescription(item: Item, desc: String) {
        items[item.key] = item.copy(desc = desc, editing = false)
        forceUpdate()
    }

    init {
        onNewItem("Clean dishes")
        onNewItem("Workout")
        onNewItem("Read fiction")
    }

    override fun RenderContext.render() {
        vbox {
            this.alignItems = Align.center
            
            vbox {
                this.justifyContent = JustifyContent.center
                textField(::newItemText) {
                    events.onEnter = {
                        onNewItem(newItemText)
                        newItemText = ""
                    }
                }

                for (item in items.values.reversed()) {
                    x(::ItemComponent, item, item.key)
                }

                label("Total items: ${items.size}, completed: ${items.count { it.value.completed }}")
            }
        }
    }
}

class ItemComponent(p: Item) : View<Item>(p) {
    var editText: String by managedState(props.desc)
    var completed: Boolean by Delegates.observable(p.completed) { _, _, v ->
        props.onSetCompleted(props, v)
    }

    override fun RenderContext.render() {
        hbox {
            checkbox("", ::completed, props.editing)

            if (props.editing) {
                textField(::editText) {
                    events.onEnter = {
                        props.onDoneEditing(props, editText)
                    }

                    events.onFocusLost = {
                        props.onDoneEditing(props, editText)
                    }
                }
            } else {
                label(props.desc) {
                    events.onClick = {
                        props.onStartEditing(props)
                    }
                }
            }
        }
    }
}
