package org.jetbrains.noria

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Props {
    var key: Any? = null
}

abstract class View<T : Props> {
    lateinit var props: T

    open fun shouldUpdate(newProps: T): Boolean = props != newProps
    abstract fun render(): NElement<*>
}

typealias Render<T> = (T) -> NElement<*>

sealed class NElement<out T : Props>(val props: T) {
    val tempId = currentContext.get().nextTempId()

    init {
        currentContext.get().createdElements.add(this)
    }

    internal class Fun<T : Props>(val f: Render<T>, props: T) : NElement<T>(props)
    internal class Class<out T : Props>(val kClass: KClass<*>, props: T) : NElement<T>(props)
    internal class Primitive<out T : PrimitiveProps>(val type: String, props: T) : NElement<T>(props)
}

sealed class Update {
    data class MakeNode(val type: String, val parameters: Map<KProperty<*>, Any?>) : Update()
    data class SetAttr(val node: Int, val attr: KProperty<*>, val value: Any?) : Update()
    data class Add(val node: Int, val attr: KProperty<*>, val child: Any?, val index: Int) : Update()
    data class Remove(val node: Int, val attr: KProperty<*>, val child: Any?) : Update()
    data class DestroyNode(val node: Int) : Update()
}

data class ReconciliationContext(val updates: MutableList<Update> = mutableListOf(),
                                 var nextNode: Int = 0,
                                 var nextTempId: Int = 0,
                                 val createdElements: MutableList<NElement<*>> = mutableListOf(),
                                 val byTempId: MutableMap<Int, Instance> = mutableMapOf()) {
    fun supply(u: Update) {
        updates.add(u)
    }

    fun makeNode() = nextNode++
    fun nextTempId() = nextTempId++
}

val currentContext = ThreadLocal<ReconciliationContext>()

class MapProperty<T>(val m: MutableMap<KProperty<*>, Any?>,
                     val m2: MutableMap<KProperty<*>, Any?>?,
                     val default: () -> T?) : ReadWriteProperty<Any, T> {
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
    fun <T : Props> element(constructor: Boolean = false) =
            MapProperty(componentsMap, if (constructor) constructorParameters else null, { null })

    val childrenMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T : List<NElement<*>>> elementList() =
            MapProperty(childrenMap, null, { mutableListOf<NElement<*>>() as T })

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

fun reconcile(component: Instance?, e: NElement<*>?): Instance? {
    if (e == null) return null
    val alreadyReconciled = currentContext.get().byTempId[e.tempId]
    if (alreadyReconciled != null)
        return InstanceRef(alreadyReconciled)
    val newComponent = when {
        e is NElement.Primitive<*> -> reconcilePrimitive(component as PrimitiveInstance?, e)
        (e is NElement.Class<*>) || (e is NElement.Fun<*>) -> reconcileUser(component as UserInstance?, e)
        else -> throw IllegalArgumentException("don't know how to reconcile $e")
    }
    currentContext.get().byTempId[e.tempId] = newComponent

    return newComponent
}

fun reconcileByKeys(byKeys: Map<Any, Instance>, coll: Collection<NElement<*>>): List<Instance> =
        coll.map { reconcile(byKeys[it.props.key], it) }.filterNotNull()

fun assignKeys(elements: List<NElement<*>>) {
    val indices = mutableMapOf<KClass<*>, Int>()
    for (element in elements) {
        if (element.props.key == null) {
            val index = indices.getOrPut(element::class, { 0 })
            element.props.key = element::class to index
            indices[element::class] = index + 1
        }
    }
}

fun reconcileUser(userComponent: UserInstance?, e: NElement<*>): Instance {
    var view = userComponent?.view as View<Props>?
    val substElement = when (e) {
        is NElement.Fun<*> -> (e as NElement.Fun<Props>).f(e.props)
        is NElement.Class<*> -> {
            if (view != null) {
                if (view.shouldUpdate(e.props))
                view.props = e.props
            } else {
                view = e.kClass.constructors.first().call() as View<Props>
            }
            view.render()
        }
        is NElement.Primitive -> throw IllegalArgumentException("reconcile user with primitive!")
    }
    val newSubst = reconcile(userComponent?.subst, substElement)
    val createdElements = currentContext.get().createdElements
    assignKeys(createdElements)
    val components = reconcileByKeys(userComponent?.byKeys ?: emptyMap<Any, Instance>(), createdElements)
    currentContext.get().createdElements.clear()
    return UserInstance(
            element = e,
            node = newSubst?.node,
            byKeys = components.associateBy { it.element.props.key!! },
            subst = newSubst,
            view = view)
}


fun reconcilePrimitive(primitiveComponent: PrimitiveInstance?, e: NElement<*>): PrimitiveInstance {
    e as NElement.Primitive<*>
    if (primitiveComponent != null && primitiveComponent.element::class != e::class) {
        currentContext.get().supply(Update.DestroyNode(primitiveComponent.node!!))
        return reconcilePrimitive(null, e)
    }
    val node: Int = primitiveComponent?.node ?: currentContext.get().makeNode()
    if (primitiveComponent?.node == null) {
        currentContext.get().supply(Update.MakeNode(e.type, e.props.constructorParameters)) // TODO: supply constructor parameters
    }

    val childrenMap = mutableMapOf<KProperty<*>, List<Instance>>()
    for ((attr, children) in e.props.childrenMap) {
        val childrenNotNull = (children as List<NElement<*>>).filterNotNull()
        assignKeys(childrenNotNull)
        childrenMap[attr] = reconcileList(node, attr, primitiveComponent?.childrenProps?.get(attr), childrenNotNull)
    }

    val componentsMap = mutableMapOf<KProperty<*>, Instance?>()
    for ((attr, element) in e.props.componentsMap) {
        val oldComponent = primitiveComponent?.elementProps?.get(attr)
        val newComponent = reconcile(oldComponent, element as NElement<*>)
        componentsMap[attr] = newComponent
        if (oldComponent?.node != newComponent?.node) {
            currentContext.get().supply(Update.SetAttr(node, attr, newComponent?.node))
        }
    }

    val valuesMap = mutableMapOf<KProperty<*>, Any?>()
    for ((attr, value) in e.props.valuesMap) {
        valuesMap[attr] = value
        if (value != primitiveComponent?.valueProps?.get(value)) {
            currentContext.get().supply(Update.SetAttr(node, attr, value))
        }
    }

    return PrimitiveInstance(
            element = e,
            childrenProps = childrenMap,
            elementProps = componentsMap,
            valueProps = valuesMap,
            node = node)
}

fun reconcileList(node: Int, attr: KProperty<*>, components: List<Instance>?, elements: List<NElement<*>>): List<Instance> {
    val componentsByKeys: Map<Any, Instance> = components?.map { it.element.props.key!! to it }?.toMap() ?: emptyMap()
    val reconciledList = reconcileByKeys(componentsByKeys, elements)
    val (removes, adds) = updateOrder(node, attr, components?.map { it.node }?.filterNotNull() ?: listOf(), reconciledList.map { it.node }.filterNotNull())
    for (update in removes + adds) {
        currentContext.get().supply(update)
    }
    return reconciledList
}

fun updateOrder(node: Int, attr: KProperty<*>, oldList: List<Int>, newList: List<Int>): Pair<List<Update.Remove>, List<Update.Add>> {
    val lcs = LCS.lcs(oldList.toIntArray(), newList.toIntArray()).toHashSet()
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
