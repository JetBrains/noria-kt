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
    fun <T : Props> reify(e: NElement<T>): NElement<T>
}

interface PlatformDriver {
    fun applyUpdates(updates: List<Update>)
}

abstract class View<T : Props> {
    val props: T get() = _props ?: error("Props are not initialized yet")
    internal var _props: T? = null
    internal lateinit var forceUpdateImpl: () -> Unit

    open fun shouldUpdate(newProps: T): Boolean {
        return true // TODO props != newProps
    }

    fun forceUpdate() {
        forceUpdateImpl()
    }


    abstract fun RenderContext.render(): NElement<*>
}

typealias Render<T> = (T) -> NElement<*>

sealed class NElement<out T : Props>(val props: T, val type: Any) {
    internal class Fun<T : Props>(val f: Render<T>, props: T) : NElement<T>(props, f)
    internal class Class<out T : Props>(val kClass: KClass<*>, props: T) : NElement<T>(props, kClass)
    internal class Primitive<out T : PrimitiveProps>(type: String, props: T) : NElement<T>(props, type)
    internal class Reified<out T : Props>(val id: Int, val e: NElement<T>) : NElement<T>(e.props, e.type)
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

class Env(private val parent: Env?, private val vars: Map<Int, Instance>) {
    companion object {
        var nextVar = 0
    }

    fun lookup(i: Int): Instance? = vars[i] ?: parent?.lookup(i)
}

class ReconciliationContext(val platform: Platform, val driver: PlatformDriver) {
    private val updates: MutableList<Update> = mutableListOf()
    private var nextNode: Int = 0

    private val callbacksTable: MutableMap<Int, MutableMap<String, CallbackInfo<*>>> = mutableMapOf()
    private var root: Instance? = null

    private fun supply(u: Update) {
        updates.add(u)
    }

    private fun makeNode() = nextNode++

    fun forceUpdate(c: UserInstance) {
        val refs = c.backrefs.toHashSet()
        val oldNode = c.node
        val newInstance = reconcileUser(c, c.element, c.env, true)
        val newNode = newInstance.node
        if (oldNode != newNode) {
            for (r in refs) {
                newInstance.backrefs.add(r)
                when (r) {
                    is ListReference -> {
                        supply(Update.Remove(r.referer.node!!, r.attr, oldNode))
                        supply(Update.Add(r.referer.node!!, r.attr, newNode, r.index))
                    }
                    is AttrReference -> {
                        supply(Update.SetAttr(r.referer.node!!, r.attr, newNode))
                    }
                }
            }
        }
        val updatesCopy = updates.toCollection(mutableListOf())
        updates.clear()
        driver.applyUpdates(updatesCopy)
    }

    fun reconcile(e: NElement<*>) {
        root = reconcileImpl(root, e, root?.env ?: Env(null, emptyMap()))
        val updatesCopy = updates.toCollection(mutableListOf())
        updates.clear()
        driver.applyUpdates(updatesCopy)
    }

    private fun reconcileImpl(component: Instance?, e: NElement<*>?, env: Env): Instance? =
            when {
                e == null -> null
                e is NElement.Primitive<*> -> reconcilePrimitive(component as PrimitiveInstance?, e, env)
                (e is NElement.Class<*>) || (e is NElement.Fun<*>) -> reconcileUser(component as UserInstance?, e, env, false)
                e is NElement.Reified<*> -> env.lookup(e.id)
                else -> throw IllegalArgumentException("don't know how to reconcile $e")
            }

    private fun reconcileByKeys(byKeys: Map<Any, Instance>, coll: Collection<NElement<*>>, env: Env): List<Instance> {
        val newKeysSet = hashSetOf<Any>()
        coll.mapTo(newKeysSet) { it.props.key!! }
        val reusableGarbageByType = mutableMapOf<Any, MutableList<Instance>>()
        byKeys.values
                .filter { it.element.props.key !in newKeysSet }
                .filter { it.backrefs.size == 1 }
                .groupByTo(reusableGarbageByType) { it.element.type }
        return coll.mapNotNull {
            var target: Instance? = byKeys[it.props.key]
            if (target == null) {
                val g = reusableGarbageByType[it.type]
                if (g != null && !g.isEmpty()) {
                    target = g.last()
                    g.removeAt(g.size - 1)
                }
            }
            reconcileImpl(target, it, env)
        }
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

    private class RenderContextImpl(val createdElements: MutableList<NElement.Reified<*>> = mutableListOf(),
                                    override val platform: Platform) : RenderContext {
        override fun <T : Props> reify(e: NElement<T>): NElement<T> {
            val reified = NElement.Reified(Env.nextVar++, e)
            createdElements.add(reified)
            return reified
        }
    }

    private fun reconcileUser(userComponent: UserInstance?, e: NElement<*>, env: Env, isForceUpate: Boolean): Instance {
        var view = when {
            userComponent == null -> null
            userComponent.element.type != e.type -> null
            else -> userComponent.view as View<Props>?
        }
        val renderContext = RenderContextImpl(platform = platform)
        val substElement = when (e) {
            is NElement.Fun<*> -> (e as NElement.Fun<Props>).f(e.props)
            is NElement.Class<*> -> {
                if (view == null) {
                    view = (e.kClass as KClass<View<Props>>).instantiate()
                }

                view.run {
                    if (_props == null || isForceUpate || shouldUpdate(e.props)) {
                        _props = e.props
                        renderContext.render()
                    } else {
                        _props = e.props
                        userComponent!!.subst?.element
                    }
                }
            }
            is NElement.Reified -> throw IllegalArgumentException("reified element is not a subject to reconcileUser")
            is NElement.Primitive -> throw IllegalArgumentException("reconcile user with primitive!")
        }
        view as View<*>

        assignKeys(renderContext.createdElements)
        val oldByKeys = userComponent?.byKeys ?: emptyMap()
        val newComponents = reconcileByKeys(oldByKeys, renderContext.createdElements.map { it.e }, env)
        val newEnv = Env(env, renderContext.createdElements.zip(newComponents) { reified, instance ->
            reified.id to instance
        }.toMap())
        val newSubst = reconcileImpl(userComponent?.subst, substElement, newEnv)
        val newByKeys = newComponents.associateBy { it.element.props.key!! }
        val result = userComponent?.apply {
            this.element = e
            this.node = newSubst?.node
            this.byKeys = newByKeys
            this.subst = newSubst
            this.view = view
            this.env = env
        } ?: UserInstance(
                element = e,
                node = newSubst?.node,
                byKeys = newByKeys,
                subst = newSubst,
                view = view,
                backrefs = hashSetOf(),
                env = env)
        view.forceUpdateImpl = { forceUpdate(result) }
        val oldComponents = oldByKeys.values
        val reference = UserReference(result)
        for (c in oldComponents) {
            c.backrefs.remove(reference)
        }
        for (c in newComponents) {
            c.backrefs.add(reference)
        }
        for (c in oldComponents) {
            if (c.backrefs.isEmpty()) {
                gc(c)
            }
        }
        return result
    }

    private fun gc(c: Instance) {
        val node = c.node
        if (node != null) {
            supply(Update.DestroyNode(node))
        }
        when (c) {
            is UserInstance -> c.byKeys.values.forEach {
                it.backrefs.remove(UserReference(c))
                if (it.backrefs.isEmpty()) {
                    gc(it)
                }
            }
            is PrimitiveInstance -> {
                c.componentProps.forEach { (attr, comp) ->
                    comp?.backrefs?.remove(AttrReference(c, attr))
                    if (comp?.backrefs?.isEmpty() == true) {
                        gc(comp)
                    }
                }
                c.childrenProps.forEach { (attr, children) ->
                    for (i in children.indices) {
                        val child = children[i]
                        child.backrefs.remove(ListReference(c, attr, i))
                        if (child.backrefs.isEmpty()) {
                            gc(child)
                        }
                    }
                }
            }
        }
    }


    private fun reconcilePrimitive(primitiveComponent: PrimitiveInstance?, e: NElement<*>, env: Env): PrimitiveInstance {
        e as NElement.Primitive<*>
        if (primitiveComponent != null && primitiveComponent.element.type != e.type) {
            supply(Update.DestroyNode(primitiveComponent.node!!))
            return reconcilePrimitive(null, e, env)
        }
        val node: Int = primitiveComponent?.node ?: makeNode()
        if (primitiveComponent?.node == null) {
            supply(Update.MakeNode(node, e.type as String, e.props.constructorParameters))
        }

        val childrenMap = mutableMapOf<String, List<Instance>>()
        val oldChildrenMap = primitiveComponent?.childrenProps
        val childrenKeySet = e.props.childrenMap.keys.union(oldChildrenMap?.keys ?: emptySet())
        for (attr in childrenKeySet) {
            val newChildren = e.props.childrenMap[attr]
            val newChildrenNotNull = newChildren?.filterNotNull() ?: emptyList()
            assignKeys(newChildrenNotNull)
            val oldChildren = oldChildrenMap?.get(attr) ?: emptyList()
            childrenMap[attr] = reconcileList(node, attr, oldChildren, newChildrenNotNull, env)
        }

        val componentsMap = mutableMapOf<String, Instance?>()
        val oldComponentsMap = primitiveComponent?.componentProps
        val componentKeySet = e.props.componentsMap.keys.union(oldComponentsMap?.keys ?: emptySet())
        for (attr in componentKeySet) {
            val element = e.props.componentsMap[attr]
            val oldComponent = oldComponentsMap?.get(attr)
            val newComponent = reconcileImpl(oldComponent, element, env)
            componentsMap[attr] = newComponent
            if (oldComponent?.node != newComponent?.node) {
                supply(Update.SetAttr(node, attr, newComponent?.node))
            }
        }

        val valuesMap = mutableMapOf<String, Any?>()
        val valuesKeySet = e.props.valuesMap.keys.union(primitiveComponent?.valueProps?.keys ?: emptySet())
        for (attr in valuesKeySet) {
            val value = e.props.valuesMap[attr]
            valuesMap[attr] = value
            val oldValue = primitiveComponent?.valueProps?.get(attr)
            if (value != oldValue) {
                supply(Update.SetAttr(node, attr, value))
            }
        }

        val callbacks = callbacksTable[node] ?: mutableMapOf()
        val callbacksKeySet = e.props.callbacks.keys.union(callbacks.keys)
        for (attr in callbacksKeySet) {
            val oldCB = callbacks[attr]
            val newCB = e.props.callbacks[attr]
            if (oldCB == null && newCB != null) {
                supply(Update.SetCallback(node, attr, newCB.async))
            } else if (oldCB != null && newCB == null) {
                supply(Update.RemoveCallback(node, attr))
            }
            if (newCB != null) {
                callbacks[attr] = newCB
            } else {
                callbacks.remove(attr)
            }
        }
        callbacksTable[node] = callbacks
        val result = primitiveComponent?.apply {
            this.element = e
            this.childrenProps = childrenMap
            this.componentProps = componentsMap
            this.valueProps = valuesMap
            this.env = env
            this.node = node
        } ?: PrimitiveInstance(
                element = e,
                childrenProps = childrenMap,
                componentProps = componentsMap,
                valueProps = valuesMap,
                node = node,
                backrefs = hashSetOf(),
                env = env)

        for (attr in childrenKeySet) {
            val newChildren = childrenMap[attr] ?: emptyList()
            val oldChildren = oldChildrenMap?.get(attr) ?: emptyList()
            for ((index, c) in oldChildren.withIndex()) {
                c.backrefs.remove(ListReference(result, attr, index))
            }
            for ((index, c) in newChildren.withIndex()) {
                c.backrefs.add(ListReference(result, attr, index))
            }
            for (c in oldChildren) {
                if (c.backrefs.isEmpty()) {
                    gc(c)
                }
            }
        }

        for (attr in componentKeySet) {
            val oldComponent = oldComponentsMap?.get(attr)
            val newComponent = componentsMap.get(attr)
            oldComponent?.backrefs?.remove(AttrReference(result, attr))
            newComponent?.backrefs?.add(AttrReference(result, attr))
            if (oldComponent?.backrefs?.isEmpty() == true) {
                gc(oldComponent)
            }
        }
        return result
    }

    private fun reconcileList(node: Int, attr: String, components: List<Instance>, elements: List<NElement<*>>, env: Env): List<Instance> {
        val componentsByKeys: Map<Any, Instance> = components.map { it.element.props.key!! to it }.toMap()
        val reconciledList = reconcileByKeys(componentsByKeys, elements, env)
        val (removes, adds) = updateOrder(node, attr, components.mapNotNull { it.node }, reconciledList.mapNotNull { it.node })
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

    val componentsMap = mutableMapOf<String, NElement<*>?>()
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

    val childrenMap = mutableMapOf<String, List<NElement<*>?>>()
    fun <T : List<NElement<*>>> elementList(): ReadWriteProperty<Any, T> =
            object : ReadWriteProperty<Any, T> {
                override fun getValue(thisRef: Any, property: KProperty<*>): T =
                        childrenMap.getOrPut(property.name, { mutableListOf() }) as T

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

interface Reference {
    val referer: Instance?
}

data class ListReference(override val referer: PrimitiveInstance, val attr: String, val index: Int) : Reference
data class AttrReference(override val referer: PrimitiveInstance, val attr: String) : Reference
data class UserReference(override val referer: UserInstance) : Reference

abstract class Instance(var element: NElement<*>,
                        var node: Int?,
                        val backrefs: MutableSet<Reference>,
                        var env: Env)

class UserInstance(element: NElement<*>,
                   node: Int?,
                   backrefs: MutableSet<Reference>,
                   env: Env,
                   var view: View<*>?,
                   var byKeys: Map<Any, Instance> = mutableMapOf(),
                   var subst: Instance?) : Instance(element, node, backrefs, env)

class PrimitiveInstance(element: NElement<*>,
                        node: Int,
                        backrefs: MutableSet<Reference>,
                        env: Env,
                        var childrenProps: Map<String, List<Instance>>,
                        var componentProps: Map<String, Instance?>,
                        var valueProps: Map<String, *>) : Instance(element, node, backrefs, env)


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
