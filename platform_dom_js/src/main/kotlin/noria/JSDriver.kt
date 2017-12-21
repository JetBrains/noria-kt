package noria

import noria.views.DomEvent
import org.jetbrains.noria.EventInfo
import org.jetbrains.noria.PlatformDriver
import org.jetbrains.noria.Update
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.get
import kotlin.browser.document

private fun Element.insertChildAtIndex(child: Node, index: Int) {
    if (index >= children.length) {
        appendChild(child)
    }
    else {
        insertBefore(child, children[index])
    }
}

class JSDriver(val mountPoint: Element, val events: (EventInfo) -> Unit) : PlatformDriver {
    private val nodes: dynamic = js("({})")
    private val callbacks = mutableMapOf<Pair<Int, String>, EventListener>()

    override fun applyUpdates(updates: List<Update>) {
        for (u in updates) {
            console.info("Applying update", u)
            when (u) {
                is Update.MakeNode -> {
                    if (nodes[u.node] != null) error("Update $u. Node already exists")
                    val newElement: Node = when(u.type) {
                        "textnode" -> document.createTextNode("")
                        else -> document.createElement(u.type)
                    }

                    nodes[u.node] = newElement

                    if (u.node == 0) {
                        mountPoint.appendChild(newElement)
                    }
                }

                is Update.SetAttr -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    when (node) {
                        is Text -> node.textContent = u.value as String
                        is Element -> node.setAttribute(u.attr.name, u.value as String)
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
                            events(EventInfo(u.node, u.attr, DomEvent() /*TODO*/))
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
                    val child: Node = nodes[u.child as Int] ?: error("Update $u. Cannot find child")
                    (node as Element).insertChildAtIndex(child, u.index)
                }

                is Update.Remove -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child: Node = nodes[u.child as Int] ?: error("Update $u. Cannot find child")
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
