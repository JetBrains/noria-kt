package noria.swing

import noria.*
import javax.swing.*
import kotlin.jvm.Volatile
import kotlin.reflect.*
import kotlin.reflect.full.*

class SwingDriver : Host {
    companion object {
        @Volatile
        var listenersSuppressed = false
        fun suppressAllListeners(x: () -> Unit) {
            listenersSuppressed = true
            try {
                x()
            } finally {
                listenersSuppressed = false
            }
        }
    }
    private val roots = mutableMapOf<String?, JPanel>()
    private val nodes = mutableMapOf<Int, Any>()

    override fun applyUpdates(updates: List<Update>) {
        SwingUtilities.invokeLater {
            val dirtyComponents = mutableSetOf<JComponent>()
            println(updates)
            suppressAllListeners {
                for (u in updates) {
                    when (u) {
                        is Update.MakeNode -> {
                            if (nodes[u.node] != null) error("Update $u. Node already exists")
                            val newElement = when (u.type) {
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
                            findFunction(node, u.attr)?.call(node, u.value)
                        }

                        is Update.SetNodeAttr -> {
                            val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                            val v = nodes[u.value]
                            findFunction(node, u.attr)?.call(node, v)
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

                            (node as JComponent).apply {
                                add(child as JComponent, u.index)
                                dirtyComponents += this
                            }
                        }

                        is Update.Remove -> {
                            val node = nodes[u.node] ?: error("Update $u. Cannot find node")
                            val child = nodes[u.value as Int] ?: error("Update $u. Cannot find child")

                            (node as JComponent).apply {
                                remove(child as JComponent)
                                dirtyComponents += this
                            }
                        }

                        is Update.DestroyNode -> {
                            nodes[u.node] ?: error("Update $u. Cannot find node")
                            nodes.remove(u.node)
                        }
                    }
                }

                dirtyComponents.forEach {
                    it.revalidate()
                }
            }
        }
    }

    private fun findFunction(node: Any, attr: String): KFunction<Any?>? {
        // TODO Reflection cache

        val setterName = "set${attr.capitalize()}"
        return node::class.memberFunctions.find { it.name == setterName } ?: (node::class.memberProperties.find { it.name == attr } as? KMutableProperty<Any?>)?.setter
    }

    fun registerRoot(id: String, root: JPanel) {
        roots[id] = root
    }
}
