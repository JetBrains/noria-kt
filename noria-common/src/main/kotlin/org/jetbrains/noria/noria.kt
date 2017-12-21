@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.noria

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class Props {
    var key: Any? = null
}

interface RenderContext {
    val platform: Platform
    fun <T : NElement<*>> emit(e: T): T
}

interface PlatformDriver {
    fun applyUpdates(updates: List<Update>)
}

abstract class View<T : Props> {
    val props: T get() = _props ?: error("Props are not initialized yet")
    internal var _props: T? = null
    lateinit var context: RenderContext

    open fun shouldUpdate(newProps: T): Boolean {
        return true // TODO props != newProps
    }

    fun forceUpdate() {
        (context as ReconciliationContext).updateFromRoot() // TODO!!! Update from this view downwards
    }


    abstract fun RenderContext.render(): NElement<*>
}

typealias Render<T> = (T) -> NElement<*>

var nextTempId: Int = 0

sealed class NElement<out T : Props>(val props: T, val type: Any) {
    val tempId: Int = nextTempId++

    internal class Fun<T : Props>(val f: Render<T>, props: T) : NElement<T>(props, f)
    internal class Class<out T : Props>(val kClass: KClass<*>, props: T) : NElement<T>(props, kClass)
    internal class Primitive<out T : PrimitiveProps>(type: String, props: T) : NElement<T>(props, type)
}

sealed class Update {
    data class MakeNode(val node: Int, val type: String, val parameters: Map<String, Any?>) : Update()
    data class SetAttr(val node: Int, val attr: String, val value: Any?) : Update()
    data class SetCallback(val node: Int, val attr: String, val async: Boolean) : Update()
    data class RemoveCallback(val node: Int, val attr: String) : Update()
    data class Add(val node: Int, val attr: String, val value: Any?, val index: Int) : Update()
    data class Remove(val node: Int, val attr: String, val value: Any?) : Update()
    data class DestroyNode(val node: Int) : Update()
}

class ReconciliationContext(override val platform: Platform, val driver: PlatformDriver) : RenderContext {
    private val updates: MutableList<Update> = mutableListOf()
    private var nextNode: Int = 0
    private val createdElements: MutableList<NElement<*>> = mutableListOf()
    private val byTempId: MutableMap<Int, Instance> = mutableMapOf()
    private val callbacksTable: MutableMap<Int, MutableMap<String, CallbackInfo<*>>> = mutableMapOf()
    private var root: Instance? = null
    private val garbage = hashSetOf<Int>()

    override fun <T : NElement<*>> emit(e: T): T {
        createdElements.add(e)
        return e
    }

    private fun supply(u: Update) {
        updates.add(u)
    }

    private fun makeNode() = nextNode++

    fun updateFromRoot() {
        reconcile(root!!.element)
    }

    fun reconcile(e: NElement<*>) {
        root = reconcileImpl(root, e)
        garbage.forEach { supply(Update.DestroyNode(it)) }
        val updatesCopy = updates.toCollection(mutableListOf())
        updates.clear()
        garbage.clear()
        byTempId.clear()

        driver.applyUpdates(updatesCopy)
    }

    private fun reconcileImpl(component: Instance?, e: NElement<*>?): Instance? {
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
        val newKeysSet = hashSetOf<Any>()
        coll.mapTo(newKeysSet) { it.props.key!! }
        val garbageByType = mutableMapOf<Any, MutableList<Instance>>()
        byKeys.values.filter { it.element.props.key !in newKeysSet }
                .groupByTo(garbageByType) { it.element.type }
        val newComponents = coll.mapNotNull {
            var target: Instance? = byKeys[it.props.key]
            if (target == null) {
                val g = garbageByType[it.type]
                if (g != null && !g.isEmpty()) {
                    target = g.last()
                    g.removeAt(g.size - 1)
                }
            }
            reconcileImpl(target, it)
        }
        garbageByType.values.flatten().filter{it !is InstanceRef}.forEach {
            val node = it.node
            if (node != null) {
                this.garbage.add(node)
            }
        }
        return newComponents
    }

    private fun assignKeys(elements: List<NElement<*>>) {
        val indices = mutableMapOf<Any, Int>()
        for (element in elements) {
            if (element.props.key == null) {
                val index = indices.getOrPut(element.type, { 0 })
                element.props.key = element.type to index
                indices[element.type] = index + 1
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
                    view.context = this
                }

                view.run {
                    if (view._props == null || shouldUpdate(e.props)) {
                        _props = e.props
                        render()
                    } else {
                        _props = e.props
                        userComponent!!.subst?.element
                    }
                }
            }

            is NElement.Primitive -> throw IllegalArgumentException("reconcile user with primitive!")
        }

        assignKeys(createdElements)
        val components = reconcileByKeys(userComponent?.byKeys ?: emptyMap(), createdElements)
        createdElements.clear()

        val newSubst = reconcileImpl(userComponent?.subst, substElement)

        return UserInstance(
                element = e,
                node = newSubst?.node,
                byKeys = components.associateBy { it.element.props.key!! },
                subst = newSubst,
                view = view)
    }


    private fun reconcilePrimitive(primitiveComponent: PrimitiveInstance?, e: NElement<*>): PrimitiveInstance {
        e as NElement.Primitive<*>
        if (primitiveComponent != null && primitiveComponent.element.type != e.type) {
            supply(Update.DestroyNode(primitiveComponent.node!!))
            return reconcilePrimitive(null, e)
        }
        val node: Int = primitiveComponent?.node ?: makeNode()
        if (primitiveComponent?.node == null) {
            supply(Update.MakeNode(node, e.type as String, e.props.constructorParameters))
        }

        val childrenMap = mutableMapOf<String, List<Instance>>()
        for ((attr, children) in e.props.childrenMap) {
            val childrenNotNull = (children as List<NElement<*>?>).filterNotNull()
            assignKeys(childrenNotNull)
            childrenMap[attr] = reconcileList(node, attr, primitiveComponent?.childrenProps?.get(attr), childrenNotNull)
        }

        val componentsMap = mutableMapOf<String, Instance?>()
        for ((attr, element) in e.props.componentsMap) {
            val oldComponent = primitiveComponent?.elementProps?.get(attr)
            val newComponent = reconcileImpl(oldComponent, element as NElement<*>)
            componentsMap[attr] = newComponent
            if (oldComponent?.node != newComponent?.node) {
                supply(Update.SetAttr(node, attr, newComponent?.node))
            }
        }

        val valuesMap = mutableMapOf<String, Any?>()
        for ((attr, value) in e.props.valuesMap) {
            valuesMap[attr] = value
            if (value != primitiveComponent?.valueProps?.get(attr)) {
                supply(Update.SetAttr(node, attr, value))
            }
        }

        val callbacks = callbacksTable[node] ?: mutableMapOf()
        for ((attr, _) in callbacks) {
            if (!e.props.callbacks.containsKey(attr)) {
                supply(Update.RemoveCallback(node, attr))
            }
        }

        for ((attr, value) in e.props.callbacks) {
            if (!callbacks.containsKey(attr)) {
                supply(Update.SetCallback(node, attr, value.async))
            }
            callbacks[attr] = value
        }
        callbacksTable[node] = callbacks

        return PrimitiveInstance(
                element = e,
                childrenProps = childrenMap,
                elementProps = componentsMap,
                valueProps = valuesMap,
                node = node)
    }

    private fun reconcileList(node: Int, attr: String, components: List<Instance>?, elements: List<NElement<*>>): List<Instance> {
        val componentsByKeys: Map<Any, Instance> = components?.map { it.element.props.key!! to it }?.toMap() ?: emptyMap()
        val reconciledList = reconcileByKeys(componentsByKeys, elements)
        val (removes, adds) = updateOrder(node, attr, components?.mapNotNull { it.node } ?: listOf(), reconciledList.mapNotNull { it.node })
        for (update in removes + adds) {
            supply(update)
        }
        return reconciledList
    }

    fun handleEvent(e: EventInfo) {
        val callbackInfo = callbacksTable[e.source]?.get(e.name)
        if (callbackInfo != null) {
            (callbackInfo as CallbackInfo<Event>).cb(e.event)
        }
    }
}

abstract class Event {
    var propagate: Boolean = true
    var preventDefault: Boolean = false

    fun stopPropagation() {
        propagate = false
    }

    fun sudoPreventDefault() {
        preventDefault = true
    }
}

class EventInfo(var source: Int,
                val name: String,
                val event: Event)

typealias Handler<T> = (T) -> Unit

data class CallbackInfo<in T : Event>(val async: Boolean, val cb: Handler<T>)

abstract class PrimitiveProps : Props() {
    val constructorParameters = mutableMapOf<String, Any?>()

    val componentsMap = mutableMapOf<String, Any?>()
    fun <T : NElement<*>> element(constructor: Boolean = false): ReadWriteProperty<Any, T> =
            object : ReadWriteProperty<Any, T> {
                override fun getValue(thisRef: Any, property: KProperty<*>): T =
                        componentsMap[property.name] as T

                override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                    if (constructor) {
                        constructorParameters[property.name] = value
                    }
                    componentsMap[property.name] = value
                }
            }

    val valuesMap = mutableMapOf<String, Any?>()
    fun <T> value(constructor: Boolean = false): ReadWriteProperty<Any, T> =
            object : ReadWriteProperty<Any, T> {
                override fun getValue(thisRef: Any, property: KProperty<*>): T =
                        valuesMap[property.name] as T

                override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                    if (constructor) {
                        constructorParameters[property.name] = value
                    }
                    valuesMap[property.name] = value
                }
            }

    val childrenMap = mutableMapOf<String, Any?>()
    fun <T : List<NElement<*>>> elementList(): ReadWriteProperty<Any, T> =
            object : ReadWriteProperty<Any, T> {
                override fun getValue(thisRef: Any, property: KProperty<*>): T =
                        childrenMap.getOrPut(property.name, { mutableListOf<Any?>() }) as T

                override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                    childrenMap[property.name] = value
                }
            }

    val callbacks = mutableMapOf<String, CallbackInfo<*>>()
    fun <T : Event> handler(): ReadWriteProperty<Any, CallbackInfo<T>> =
            object : ReadWriteProperty<Any, CallbackInfo<T>> {
                override fun getValue(thisRef: Any, property: KProperty<*>): CallbackInfo<T> =
                        callbacks[property.name] as CallbackInfo<T>

                override fun setValue(thisRef: Any, property: KProperty<*>, value: CallbackInfo<T>) {
                    callbacks[property.name] = value
                }
            }
}

abstract class Instance(val element: NElement<*>,
                        val node: Int?)

class InstanceRef(c: Instance) : Instance(c.element, c.node)

class UserInstance(element: NElement<*>, node: Int?,
                   val view: View<*>?,
                   val byKeys: Map<Any, Instance> = mutableMapOf(),
                   val subst: Instance?) : Instance(element, node)

class PrimitiveInstance(element: NElement<*>, node: Int,
                        val childrenProps: Map<String, List<Instance>>,
                        val elementProps: Map<String, Instance?>,
                        val valueProps: Map<String, *>) : Instance(element, node)


fun updateOrder(node: Int, attr: String, oldList: List<Int>, newList: List<Int>): Pair<List<Update.Remove>, List<Update.Add>> {
    val lcs = lcs(oldList.toIntArray(), newList.toIntArray()).toHashSet()
    val oldNodesSet = oldList.toHashSet()
    val removes = mutableListOf<Update.Remove>()
    val adds = mutableListOf<Update.Add>()
    val allNodes = (oldList + newList).toHashSet()
    for (c in allNodes) {
        if (!lcs.contains(c) && oldNodesSet.contains(c)) {
            removes.add(Update.Remove(node = node, value = c, attr = attr))
        }
    }
    newList.forEachIndexed { i, c ->
        if (!lcs.contains(c)) {
            adds.add(Update.Add(node = node, value = c, attr = attr, index = i))
        }
    }
    return removes to adds
}
