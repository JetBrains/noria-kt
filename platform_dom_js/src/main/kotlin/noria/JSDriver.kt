package noria

import noria.EventInfo
import noria.Host
import noria.Update
import noria.views.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.events.Event
import kotlin.browser.document

private fun Element.insertChildAtIndex(child: Node, index: Int) {
    if (index >= children.length) {
        appendChild(child)
    }
    else {
        insertBefore(child, children[index])
    }
}

class JSDriver(val events: (EventInfo) -> Unit) : Host {
    private val nodes: dynamic = js("({})")
    private val callbacks = mutableMapOf<Pair<Int, String>, EventListener>()

    private val roots = mutableMapOf<String?, Element>()

    fun registerRoot(id: String, root: Element) {
        if (roots.containsKey(id)) error("Root $id is already registered")
        roots[id] = root
    }

    override fun applyUpdates(updates: List<Update>) {
        for (u in updates) {
            console.info("Applying update", u)
            when (u) {
                is Update.MakeNode -> {
                    if (nodes[u.node] != null) error("Update $u. Node already exists")
                    val newElement: Node = when(u.type) {
                        "root" -> {
                            val id = u.parameters["id"] as? String
                            roots[id] ?: error("Root $id has not been registered")
                        }
                        "textnode" -> document.createTextNode("")
                        else -> {
                            val element = document.createElement(u.type)
                            element.setAttribute("node-id", "${u.node}")
                            element
                        }
                    }

                    nodes[u.node] = newElement
                }

                is Update.SetAttr -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    when (node) {
                        is Text -> node.textContent = u.value as String
                        is Element -> {
                            node[u.attr] = u.value
                        }
                        else -> error("Unknown type of the node")
                    }

                }

                is Update.SetCallback -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        error("Update $u. Callback is aready set")
                    }

                    val listener = object : EventListener {
                        override fun handleEvent(event: Event) {
                            console.log(event)
                            events(EventInfo(u.node, u.attr, when(event.type) {
                                "input" -> {
                                    val target = event.target.asDynamic()
                                    ChangeEvent(target.value)
                                }

                                "change"-> {
                                    val target = event.target.asDynamic()
                                    val checked: Boolean? = target.checked
                                    if (checked != null) {
                                        ChangeEvent(checked)
                                    }
                                    else {
                                        ChangeEvent(target.value)
                                    }
                                }

                            /*TODO*/

                                else -> DomEvent()
                            }))
                        }
                    }
                    (node as Element).addEventListener(u.attr, listener)
                    callbacks[u.node to u.attr] = listener
                }

                is Update.RemoveCallback -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        (node as Element).removeEventListener(u.attr, it)
                    } ?: error("Update $u. Callback is not set")
                }

                is Update.Add -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child: Node = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    (node as Element).insertChildAtIndex(child, u.index)
                }

                is Update.Remove -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child: Node = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    (node as Element).removeChild(child)
                }

                is Update.DestroyNode -> {
                    nodes[u.node] ?: error("Update $u. Cannot find node")
                    nodes[u.node] = null
                }
            }
        }
    }
}
