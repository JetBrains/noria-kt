@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.noria

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class Props {
    var key: Any? = null
}

interface RenderContext {
    fun <T: NElement<*>> emit(e: T): T
}

abstract class View<T : Props> {
    val props: T get() = _props ?: error("Props are not initialized yet")
    internal var _props: T? = null

    open fun shouldUpdate(newProps: T): Boolean {
        return props != newProps
    }
    abstract fun RenderContext.render(): NElement<*>
}

typealias Render<T> = (T) -> NElement<*>

var nextTempId: Int = 0

sealed class NElement<out T : Props>(val props: T) {
    val tempId: Int = nextTempId++

    internal class Fun<T : Props>(val f: Render<T>, props: T) : NElement<T>(props)
    internal class Class<out T : Props>(val kClass: KClass<*>, props: T) : NElement<T>(props)
    internal class Primitive<out T : PrimitiveProps>(val type: String, props: T) : NElement<T>(props)
}

sealed class Update {
    data class MakeNode(val node: Int, val type: String, val parameters: Map<KProperty<*>, Any?>) : Update()
    data class SetAttr(val node: Int, val attr: KProperty<*>, val value: Any?) : Update()
    data class Add(val node: Int, val attr: KProperty<*>, val child: Any?, val index: Int) : Update()
    data class Remove(val node: Int, val attr: KProperty<*>, val child: Any?) : Update()
    data class DestroyNode(val node: Int) : Update()
}

class ReconciliationContext : RenderContext {
    private val updates: MutableList<Update> = mutableListOf()
    private var nextNode: Int = 0
    private val createdElements: MutableList<NElement<*>> = mutableListOf()
    private val byTempId: MutableMap<Int, Instance> = mutableMapOf()

    override fun <T : NElement<*>> emit(e: T): T {
        createdElements.add(e)
        return e
    }

    private fun supply(u: Update) {
        updates.add(u)
    }

    fun updates(): List<Update> = updates

    private fun makeNode() = nextNode++

    fun reconcile(component: Instance?, e: NElement<*>?): Instance? {
        if (e == null) return null
        val alreadyReconciled = byTempId[e.tempId]
        if (alreadyReconciled != null)
            return InstanceRef(alreadyReconciled)
        val newComponent = when {
            e is NElement.Primitive<*> -> reconcilePrimitive(component as PrimitiveInstance?, e)
            (e is NElement.Class<*>) || (e is NElement.Fun<*>) -> reconcileUser(component as UserInstance?, e)
            else -> throw IllegalArgumentException("don't know how to reconcile $e")
        }
        byTempId[e.tempId] = newComponent

        return newComponent
    }

    private fun reconcileByKeys(byKeys: Map<Any, Instance>, coll: Collection<NElement<*>>): List<Instance> {
        return coll.mapNotNull { reconcile(byKeys[it.props.key], it) }
    }

    private fun assignKeys(elements: List<NElement<*>>) {
        val indices = mutableMapOf<KClass<*>, Int>()
        for (element in elements) {
            if (element.props.key == null) {
                val index = indices.getOrPut(element::class, { 0 })
                element.props.key = element::class to index
                indices[element::class] = index + 1
            }
        }
    }

    private fun reconcileUser(userComponent: UserInstance?, e: NElement<*>): Instance {
        var view = userComponent?.view as View<Props>?
        val substElement = when (e) {
            is NElement.Fun<*> -> (e as NElement.Fun<Props>).f(e.props)
            is NElement.Class<*> -> {
                if (view == null) {
                    view = (e.kClass as KClass<View<Props>>).instantiate()
                }

                view.run {
                    if (view._props == null || shouldUpdate(e.props)) {
                        _props = e.props
                        render()
                    } else {
                        _props = e.props
                        userComponent!!.element
                    }
                }
            }

            is NElement.Primitive -> throw IllegalArgumentException("reconcile user with primitive!")
        }

        assignKeys(createdElements)
        val components = reconcileByKeys(userComponent?.byKeys ?: emptyMap(), createdElements)
        createdElements.clear()

        val newSubst = reconcile(userComponent?.subst, substElement)

        return UserInstance(
                element = e,
                node = newSubst?.node,
                byKeys = components.associateBy { it.element.props.key!! },
                subst = newSubst,
                view = view)
    }


    private fun reconcilePrimitive(primitiveComponent: PrimitiveInstance?, e: NElement<*>): PrimitiveInstance {
        e as NElement.Primitive<*>
        if (primitiveComponent != null && primitiveComponent.element::class != e::class) {
            supply(Update.DestroyNode(primitiveComponent.node!!))
            return reconcilePrimitive(null, e)
        }
        val node: Int = primitiveComponent?.node ?: makeNode()
        if (primitiveComponent?.node == null) {
            supply(Update.MakeNode(node, e.type, e.props.constructorParameters))
        }

        val childrenMap = mutableMapOf<KProperty<*>, List<Instance>>()
        for ((attr, children) in e.props.childrenMap) {
            val childrenNotNull = (children as List<NElement<*>?>).filterNotNull()
            assignKeys(childrenNotNull)
            childrenMap[attr] = reconcileList(node, attr, primitiveComponent?.childrenProps?.get(attr), childrenNotNull)
        }

        val componentsMap = mutableMapOf<KProperty<*>, Instance?>()
        for ((attr, element) in e.props.componentsMap) {
            val oldComponent = primitiveComponent?.elementProps?.get(attr)
            val newComponent = reconcile(oldComponent, element as NElement<*>)
            componentsMap[attr] = newComponent
            if (oldComponent?.node != newComponent?.node) {
                supply(Update.SetAttr(node, attr, newComponent?.node))
            }
        }

        val valuesMap = mutableMapOf<KProperty<*>, Any?>()
        for ((attr, value) in e.props.valuesMap) {
            valuesMap[attr] = value
            if (value != primitiveComponent?.valueProps?.get(value)) {
                supply(Update.SetAttr(node, attr, value))
            }
        }

        return PrimitiveInstance(
                element = e,
                childrenProps = childrenMap,
                elementProps = componentsMap,
                valueProps = valuesMap,
                node = node)
    }

    private fun reconcileList(node: Int, attr: KProperty<*>, components: List<Instance>?, elements: List<NElement<*>>): List<Instance> {
        val componentsByKeys: Map<Any, Instance> = components?.map { it.element.props.key!! to it }?.toMap() ?: emptyMap()
        val reconciledList = reconcileByKeys(componentsByKeys, elements)
        val (removes, adds) = updateOrder(node, attr, components?.mapNotNull { it.node } ?: listOf(), reconciledList.mapNotNull { it.node })
        for (update in removes + adds) {
            supply(update)
        }
        return reconciledList
    }
}

class MapProperty<T>(private val m: MutableMap<KProperty<*>, Any?>,
                     private val m2: MutableMap<KProperty<*>, Any?>?,
                     private val default: () -> T?) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
            m.getOrPut(property, default) as T

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        m[property] = value
        m2?.put(property, value)
    }
}

abstract class PrimitiveProps : Props() {
    val constructorParameters = mutableMapOf<KProperty<*>, Any?>()
    val componentsMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T : NElement<*>> element(constructor: Boolean = false) =
            MapProperty<T>(componentsMap, if (constructor) constructorParameters else null, { null })

    val childrenMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T : List<NElement<*>>> elementList() =
            MapProperty<T>(childrenMap, null, { mutableListOf<NElement<*>>() as T })

    val valuesMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T> value(constructor: Boolean = false) =
            MapProperty<T>(valuesMap, if (constructor) valuesMap else null, { null })
}

abstract class Instance(val element: NElement<*>,
                        val node: Int?)

class InstanceRef(c: Instance) : Instance(c.element, c.node)

class UserInstance(element: NElement<*>, node: Int?,
                   val view: View<*>?,
                   val byKeys: Map<Any, Instance> = mutableMapOf(),
                   val subst: Instance?) : Instance(element, node)

class PrimitiveInstance(element: NElement<*>, node: Int,
                        val childrenProps: Map<KProperty<*>, List<Instance>>,
                        val elementProps: Map<KProperty<*>, Instance?>,
                        val valueProps: Map<KProperty<*>, *>) : Instance(element, node)


fun updateOrder(node: Int, attr: KProperty<*>, oldList: List<Int>, newList: List<Int>): Pair<List<Update.Remove>, List<Update.Add>> {
    val lcs = lcs(oldList.toIntArray(), newList.toIntArray()).toHashSet()
    val oldNodesSet = oldList.toHashSet()
    val removes = mutableListOf<Update.Remove>()
    val adds = mutableListOf<Update.Add>()
    for (c in oldList + newList) {
        if (!lcs.contains(c) && oldNodesSet.contains(c)) {
            removes.add(Update.Remove(node = node, child = c, attr = attr))
        }
    }
    newList.forEachIndexed { i, c ->
        if (!lcs.contains(c)) {
            adds.add(Update.Add(node = node, child = c, attr = attr, index = i))
        }
    }
    return removes to adds
}
