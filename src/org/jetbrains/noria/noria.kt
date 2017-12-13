package org.jetbrains.noria

import java.util.concurrent.Future
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class Element {
    init {
        currentContext.get().createdElements.add(this)
    }

    var key: Any? = null
    val tempId: Int = currentContext.get().nextTempId()
}

sealed class Update {
    data class MakeNode(val type: KClass<*>, val parameters: Map<KProperty<*>, Any?>) : Update()
    data class SetAttr(val node: Int, val attr: KProperty<*>, val value: Any?) : Update()
    data class AddChild(val node: Int, val attr: KProperty<*>, val child: Any?, val index: Int) : Update()
    data class RemoveChild(val node: Int, val attr: KProperty<*>, val child: Any?) : Update()
    data class DestroyNode(val node: Int) : Update()
}

data class ReconciliationContext(val updates: MutableList<Update> = mutableListOf(),
                                 var nextNode: Int = 0,
                                 var nextTempId: Int = 0,
                                 val createdElements: MutableList<Element> = mutableListOf(),
                                 val byTempId: MutableMap<Int, Component<*>> = mutableMapOf()) {
    fun supply(u: Update) {
        updates.add(u)
    }

    fun makeNode() = nextNode++
    fun nextTempId() = nextTempId++
}

val currentContext = ThreadLocal<ReconciliationContext>()

class MapProperty<T>(val m: MutableMap<KProperty<*>, Any?>,
                     val m2: MutableMap<KProperty<*>, Any?>?,
                     val default: () -> T) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T =
            m.getOrPut(property, default) as T

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        m[property] = value
        m2?.put(property, value)
    }
}

abstract class PrimitiveElement : Element() {
    val constructorParameters = mutableMapOf<KProperty<*>, Any?>()
    val componentsMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T : Element> element(constructor: Boolean = false) = MapProperty(componentsMap, if (constructor) constructorParameters else null, { null })

    val childrenMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T : List<Element>> children() = MapProperty(childrenMap, null, { mutableListOf<Element>() })

    val valuesMap = mutableMapOf<KProperty<*>, Any?>()
    fun <T> value(constructor: Boolean = false) = MapProperty(valuesMap, if (constructor) valuesMap else null, { null })
}

typealias Render<T> = (T) -> Element?

typealias ShouldComponentUpdate<T> = (old: T, new: T) -> Boolean

class ComponentSpec<T> {
    var render: Render<T> = { null }
    var shouldComponentUpdate: ShouldComponentUpdate<T> = { old, new -> old != new }
}

abstract class UserElement : Element() {
    var spec: ComponentSpec<*>? = null
}

abstract class Component<out T : Element>(val element: T,
                                          val node: Int?)

class ComponentRef<out T: Element>(c: Component<T>): Component<T>(c.element, c.node)

class UserComponent<out T : Element>(element: T, node: Int?,
                                     val byKeys: Map<Any, Component<*>> = mutableMapOf(),
                                     val subst: Component<Element>?) : Component<T>(element, node)

class PrimitiveComponent<out T : Element>(element: T, node: Int,
                                          val childrenProps: Map<KProperty<*>, List<Component<*>>>,
                                          val elementProps: Map<KProperty<*>, Component<*>?>,
                                          val valueProps: Map<KProperty<*>, *>) : Component<T>(element, node)

fun reconcile(component: Component<*>?, e: Element?): Component<*>? {
    if (e == null) return null
    val alreadyReconciled = currentContext.get().byTempId[e.tempId]
    if (alreadyReconciled != null)
        return ComponentRef(alreadyReconciled)
    val newComponent = when {
        e is PrimitiveElement -> reconcilePrimitive(component as PrimitiveComponent<*>?, e)
        e is UserElement -> reconcileUser(component as UserComponent<*>?, e)
        else -> throw IllegalArgumentException("don't know how to reconcile $e")
    }
    if (newComponent != null) {
        currentContext.get().byTempId[e.tempId] = newComponent
    }
    return newComponent
}

fun reconcileByKeys(byKeys: Map<Any, Component<*>>, coll: Collection<Element>): List<Component<*>> =
        coll.map { reconcile(byKeys[it.key], it) }.filterNotNull()

fun assignKeys(elements: List<Element>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

fun reconcileUser(userComponent: UserComponent<*>?, e: UserElement): Component<*>? {
    val substElement = (e.spec!! as ComponentSpec<Element>).render(e)
    val newSubst = reconcile(userComponent?.subst, substElement)
    val createdElements = currentContext.get().createdElements
    assignKeys(createdElements)
    val components = reconcileByKeys(userComponent?.byKeys ?: emptyMap<Any, Component<*>>(), createdElements)
    currentContext.get().createdElements.clear()
    return UserComponent<Element>(
            element = e,
            node = newSubst?.node,
            byKeys = components.associateBy { it.element.key!! },
            subst = newSubst)
}


fun reconcilePrimitive(primitiveComponent: PrimitiveComponent<*>?, e: PrimitiveElement): PrimitiveComponent<*>? {
    if (primitiveComponent != null && primitiveComponent.element::class != e::class) {
        currentContext.get().supply(Update.DestroyNode(primitiveComponent.node!!))
        return reconcilePrimitive(null, e)
    }
    val node: Int = primitiveComponent?.node ?: currentContext.get().makeNode()
    if (primitiveComponent?.node == null) {
        currentContext.get().supply(Update.MakeNode(e::class, e.constructorParameters)) // TODO: supply constructor parameters
    }

    val childrenMap = mutableMapOf<KProperty<*>, List<Component<*>>>()
    for ((attr, children) in e.childrenMap) {
        val childrenNotNull = (children as List<Element?>).filterNotNull()
        assignKeys(childrenNotNull)
        childrenMap[attr] = reconcileList(node, attr, primitiveComponent?.childrenProps?.get(attr), childrenNotNull)
    }

    val componentsMap = mutableMapOf<KProperty<*>, Component<*>?>()
    for((attr, element) in e.componentsMap) {
        val oldComponent = primitiveComponent?.elementProps?.get(attr)
        val newComponent = reconcile(oldComponent, element as Element)
        componentsMap[attr] = newComponent
        if (oldComponent?.node != newComponent?.node) {
            currentContext.get().supply(Update.SetAttr(node, attr, newComponent?.node))
        }
    }

    val valuesMap = mutableMapOf<KProperty<*>, Any?>()
    for ((attr, value) in e.valuesMap) {
        valuesMap[attr] = value
        if (value != primitiveComponent?.valueProps?.get(value)) {
            currentContext.get().supply(Update.SetAttr(node, attr, value))
        }
    }

    return PrimitiveComponent(
            element = e,
            childrenProps = childrenMap,
            elementProps = componentsMap,
            valueProps = valuesMap,
            node = node)
}

fun reconcileList(node: Int, attr: KProperty<*>, components: List<Component<*>>?, elements: List<Element>): List<Component<*>> {
    val componentsByKeys: Map<Any, Component<*>> = components?.map { it.element.key!! to it }?.toMap() ?: emptyMap()
    val reconciledList = reconcileByKeys(componentsByKeys, elements)
    val (removes, adds) = updateOrder(node, attr, components?.map { it.node }?.filterNotNull() ?: listOf(), reconciledList.map { it.node }.filterNotNull())
    for (update in removes + adds) {
        currentContext.get().supply(update)
    }
    return reconciledList
}

fun updateOrder(node: Int, attr: KProperty<*>, oldList: List<Int>, newList: List<Int>): Pair<List<Update.RemoveChild>, List<Update.AddChild>> {
    val lcs = LCS.lcs(oldList.toIntArray(), newList.toIntArray()).toHashSet()
    val oldNodesSet = oldList.toHashSet()
    val removes = mutableListOf<Update.RemoveChild>()
    val adds = mutableListOf<Update.AddChild>()
    for (c in oldList + newList) {
        if (!lcs.contains(c) && oldNodesSet.contains(c)) {
            removes.add(Update.RemoveChild(node = node, child = c, attr = attr))
        }
    }
    newList.forEachIndexed {i, c ->
        if (!lcs.contains(c)) {
            adds.add(Update.AddChild(node = node, child = c, attr = attr, index = i))
        }
    }
    return removes to adds
}



////////////////////
//
//MyComponent {
//    render() {
////        var c1 = null
////        split {
////            left {
////                vbox {}
////                vbox {}
////            }
////            right {
////                vbox {}
////                vbox {}
////            }
////        }
////
//
//        var m = null
//        TableView {
//            model {
//                val m = capture {
//                    TableViewModel {
//
//                    }
//                }
//
//                left {
//                    + m
//                }
//
//                ref = {
//                    m.view = it
//                }
//
//            }
//        }
//
//
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//fun <T : Element> component(builder: ComponentSpec<T>.() -> Unit): (T.() -> Unit) -> Component<T>? {
//    val b = ComponentSpec<T>()
//    b.builder()
//    return {
//
//    }
//}
//
//data class Div(var attrs: MutableMap<String, Any?> = mutableMapOf(),
//               var children: MutableList<Component<Element>> = mutableListOf()) : PrimitiveElement()
//
//fun <T : PrimitiveElement> primitiveComponent(): (T.() -> Unit) -> Component<T>? {
//
//}
//
//
//val div = primitiveComponent<Div>()
//
//data class FooProps(var foo: String = "",
//                    var children: List<Component<Element>>) : Element()
//
//val foo = component<FooProps> {
//    shouldComponentUpdate = { old, new -> old.children != new.children }
//    render = { fp ->
//        div {
//
//        }
//    }
//}
//
//data class BarProps(var x: String) : Element()
//
//val bar = component<BarProps> {
//    shouldComponentUpdate { old, new -> old.x != new.x }
//    render { bp ->
//        foo {
//            foo = bp.x
//        }
//    }
//}