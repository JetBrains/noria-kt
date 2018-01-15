package noria.swing

import noria.*
import javax.swing.*
import kotlin.reflect.full.*

class SwingDriver : Host {
    private val roots = mutableMapOf<String?, JPanel>()
    private val nodes = mutableMapOf<Int, Any>()
    private val callbacks = mutableMapOf<Pair<Int, String>, Any?>()

    override fun applyUpdates(updates: List<Update>) {
        SwingUtilities.invokeLater {
            for (u in updates) {
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
                        if (u.attr.endsWith("Listener")) {
                            val oldListener = callbacks[u.node to u.attr]
                            callbacks[u.node to u.attr] = u.value
                            if (oldListener != null) {
                                findFunction(node, "remove${u.attr.capitalize()}")?.call(node, oldListener)
                            }

                            if (u.value != null) {
                                findFunction(node, "add${u.attr.capitalize()}")?.call(node, u.value)
                            }
                        }
                        else {
                            findFunction(node, "set${u.attr.capitalize()}")?.call(node, u.value)
                        }
                    }

                    is Update.SetNodeAttr -> {
                        val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                        val v = nodes[u.value]
                        findFunction(node, "set${u.attr.capitalize()}") ?.call(node, v)
                    }

                    is Update.SetCallback -> {
                        error("Callback should not be used in Swing Driver")
                    }

                    is Update.RemoveCallback -> {
                        error("Callback should not be used in Swing Driver")
                    }

                    is Update.Add -> {
                        val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                        val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")

                        if (u.attr.endsWith("Listeners")) {
                            findFunction(node, "add${u.attr.dropLast(1).capitalize()}")?.call(node, child)
                        }
                        else {
                            (node as JComponent).add(child as JComponent, u.index)
                        }
                    }

                    is Update.Remove -> {
                        val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                        val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")

                        if (u.attr.endsWith("Listeners")) {
                            findFunction(node, "remove${u.attr.dropLast(1).capitalize()}")?.call(node, child)
                        }
                        else {
                            (node as JComponent).remove(child as JComponent)
                        }
                    }

                    is Update.DestroyNode -> {
                        nodes[u.node] ?: error("Update $u. Cannot find node")
                        nodes.remove(u.node)
                    }
                }
            }
        }
    }

    private fun findFunction(node: Any, name: String) = node::class.memberFunctions.find { it.name == name }

    fun registerRoot(id: String, root: JPanel) {
        roots[id] = root
    }
}
