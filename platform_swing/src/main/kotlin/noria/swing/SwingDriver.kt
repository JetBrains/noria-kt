package noria.swing

import noria.*
import noria.swing.components.ActionEvent
import java.awt.event.*
import java.util.*
import javax.swing.*
import kotlin.reflect.full.*

class SwingDriver(val events: (EventInfo) -> Unit) : Host {
    private val roots = mutableMapOf<String?, JPanel>()
    private val nodes = mutableMapOf<Int, Any>()
    private val callbacks = mutableMapOf<Pair<Int, String>, EventListener>()

    override fun applyUpdates(updates: List<Update>) {
        for (u in updates) {
            println("Applying update: $u")
            when (u) {
                is Update.MakeNode -> {
                    if (nodes[u.node] != null) error("Update $u. Node already exists")
                    val newElement = when(u.type) {
                        "root" -> {
                            val id = u.parameters["id"] as? String
                            roots[id] ?: error("Root $id has not been registered")
                        }

                        else -> Class.forName(u.type).newInstance() // TODO Reflection cache
                    }

                    nodes[u.node] = newElement
                }

                is Update.SetAttr -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    node::class.memberFunctions.find { it.name == "set${u.attr.capitalize()}" }?.call(node, u.value)
                }

                is Update.SetNodeAttr -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val v = nodes[u.value]
                    node::class.memberFunctions.find { it.name == "set${u.attr.capitalize()}" }?.call(node, v)
                }

                is Update.SetCallback -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        error("Update $u. Callback is aready set")
                    }

                    val listener: ActionListener
                    when(u.attr) {
                        "actionListener" -> {
                            listener = ActionListener {
                                events(EventInfo(u.node, u.attr, ActionEvent()))
                            }
                            node::class.memberFunctions.find { it.name == "addActionListener" }?.call(node, listener)
                        }
                        else -> error("Unknown event")
                    }

                    callbacks[u.node to u.attr] = listener
                }

                is Update.RemoveCallback -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    callbacks[u.node to u.attr]?.let {
                        when(u.attr) {
                            "actionListner" -> {
                                node::class.memberFunctions.find { it.name == "removeActionListener" }?.call(node, it)
                            }
                            else -> error("Unknown event")
                        }
                    } ?: error("Update $u. Callback is not set")
                }

                is Update.Add -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    (node as JComponent).add(child as JComponent, u.index)
                }

                is Update.Remove -> {
                    val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                    val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")
                    (node as JComponent).remove(child as JComponent)
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
