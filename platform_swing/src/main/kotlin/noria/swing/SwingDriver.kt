package noria.swing

import noria.*
import java.util.*
import javax.swing.*
import kotlin.reflect.full.*

class SwingDriver(val events: (EventInfo) -> Unit) : Host {
    private val roots = mutableMapOf<String?, JPanel>()
    private val nodes = mutableMapOf<Int, JComponent>()
    private val callbacks = mutableMapOf<Pair<Int, String>, EventListener>()

    override fun applyUpdates(updates: List<Update>) {
        for (u in updates) {
            println("Applying update: $u")
            when (u) {
                is Update.MakeNode -> {
                    if (nodes[u.node] != null) error("Update $u. Node already exists")
                    val newElement: JComponent = when(u.type) {
                        "root" -> {
                            val id = u.parameters["id"] as? String
                            roots[id] ?: error("Root $id has not been registered")
                        }

                        else -> Class.forName("javax.swing.${u.type}").newInstance() as JComponent // TODO Reflection cache
                    }

                    nodes[u.node] = newElement
                }

                is Update.SetAttr -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    node::class.declaredMemberFunctions.find { it.name == "set${u.attr.capitalize()}" }?.call(node, u.value)
                }

                is Update.SetCallback -> {
/*
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        error("Update $u. Callback is aready set")
                    }

                    val listener = object : EventListener {
                        override fun handleEvent(event: Event) {
                            events(EventInfo(u.node, u.attr, DomEvent() */
/*TODO*//*
))
                        }
                    }
                    (node as JComponent).addEventListener(u.attr, listener)
                    callbacks[u.node to u.attr] = listener
*/
                }

                is Update.RemoveCallback -> {
/*
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        (node as Element).removeEventListener(u.attr, it)
                    } ?: error("Update $u. Callback is not set")
*/
                }

                is Update.Add -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    node.add(child, u.index)
                }

                is Update.Remove -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    node.remove(child)
                }

                is Update.DestroyNode -> {
                    nodes[u.node] ?: error("Update $u. Cannot find node")
                    nodes.remove(u.node)
                }
            }
        }

    }

    fun registerRoot(id: String, root: JPanel) {
        roots[id] = root
    }
}
