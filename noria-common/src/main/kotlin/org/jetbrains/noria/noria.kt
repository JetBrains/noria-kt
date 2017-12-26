@file:Suppress("UNCHECKED_CAST")

package org.jetbrains.noria

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class Props {
    var key: String? = null
}

interface RenderContext {
    infix fun <T: Props> Render<T>.with(props: T): NElement<T> = emit(NElement.Fun(this, props))
    infix fun <T: Props> Constructor<T>.with(props: T) : NElement<T> = emit(NElement.Class(this, props))
    infix fun <T: HostProps> HostComponentType<T>.with(props: T) : NElement<T> = emit(NElement.HostElement(this, props))
    infix fun <T: Props> PlatformComponentType<T>.with(props: T) : NElement<T> = emit(NElement.PlatformDispatch(this, props))

    fun <T : Props> reify(e: NElement<T>): NElement<T>
    fun <T: Props> emit(e: NElement<T>): NElement<T>
}

fun RenderContext.capture(build: RenderContext.() -> Unit) : NElement<*> {
    val capturingContext = RenderContextImpl()
    capturingContext.build()
    return reify(capturingContext.createdElements.single())
}

class RenderContextImpl() : RenderContext {
    internal val reifiedElements: MutableList<NElement.Reified<*>> = mutableListOf()
    val createdElements: MutableList<NElement<*>> = mutableListOf()

    override fun <T : Props> reify(e: NElement<T>): NElement<T> {
        createdElements.remove(e)
        return NElement.Reified(Env.nextVar++, e).apply {
            reifiedElements.add(this)
        }
    }

    override fun <T: Props> emit(e: NElement<T>): NElement<T> {
        createdElements.add(e)
        return e
    }
}

interface PlatformDriver {
    fun applyUpdates(updates: List<Update>)
}

abstract class View<T : Props> {
    val props: T get() = _props ?: error("Props are not initialized yet")
    internal var _props: T? = null
    internal lateinit var context: GraphState
    internal lateinit var instance: UserInstance

    open fun shouldUpdate(newProps: T): Boolean {
        return true // TODO props != newProps
    }

    fun forceUpdate() {
        context.forceUpdate(instance)
    }

    abstract fun RenderContext.render()
}

typealias Render<T> = (T) -> NElement<*>
typealias Constructor<T> = () -> View<T>

interface ComponentType<T : Props>
data class HostComponentType<T : HostProps>(val type: String) : ComponentType<T>
class PlatformComponentType<T : Props> : ComponentType<T>

sealed class NElement<T : Props>(val props: T, open val type: Any) {
    /*internal */class Fun<T : Props>(override val type: Render<T>, props: T) : NElement<T>(props, type)
    /*internal */class Class<T : Props>(override val type: Constructor<T>, props: T) : NElement<T>(props, type)
    /*internal */class HostElement<T : HostProps>(override val type: HostComponentType<T>, props: T) : NElement<T>(props, type)
    /*internal */class PlatformDispatch<T : Props>(override val type: PlatformComponentType<T>, props: T) : NElement<T>(props, type)
    /*internal */class Reified<T : Props>(val id: Int, val e: NElement<T>) : NElement<T>(e.props, e.type)
}

sealed class Update {
    data class MakeNode(val node: Int, val type: String, val parameters: MutableMapLike<String, Any?>) : Update()
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


class ReconciliationState(val graph: GraphState) {
    private val updates: MutableList<Update> = mutableListOf()

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
        graph.driver.applyUpdates(updates)
    }

    private fun supply(u: Update) {
        updates.add(u)
    }

    fun mountRoot(e: NElement<RootProps>) {
        reconcileImpl(null, e, Env(null, emptyMap()))
        graph.driver.applyUpdates(updates)
    }

    //TODO remove, tests only
    fun reconcile(component: Instance?, e: NElement<*>): Pair<Instance?, List<Update>> {
        val instance = reconcileImpl(component, e, Env(null, emptyMap()))
        return instance to updates
    }

    private fun reconcileImpl(component: Instance?, e: NElement<*>?, env: Env): Instance? =
            when {
                e == null -> null
                e is NElement.HostElement<*> -> reconcileHost(component as HostInstance?, e, env)
                (e is NElement.Class<*>) || (e is NElement.Fun<*>) -> reconcileUser(component as UserInstance?, e, env, false)
                e is NElement.PlatformDispatch<*> -> reconcileImpl(component, NElement.Class((graph.platform.resolve(e.type as PlatformComponentType<*>) as Constructor<Props>), e.props), env)
                e is NElement.Reified<*> -> env.lookup(e.id)
                else -> throw IllegalArgumentException("don't know how to reconcile $e")
            }

    private fun reconcileByKeys(byKeys: MutableMapLike<String, Instance>, coll: Collection<NElement<*>>, env: Env): List<Instance> {
        val newKeysSet = fastStringMap<String>()
        for (e in coll) {
            newKeysSet[e.props.key!!] = e.props.key!!
        }

        val reusableGarbageByType = fastStringMap<MutableList<Instance>>()
        byKeys.forEach { _, v ->
            if (!newKeysSet.containsKey(v.element.props.key!!) && v.backrefs.size == 1) {
                var list = reusableGarbageByType[v.element.type.toString()]
                if (list == null) {
                    list = mutableListOf()
                    reusableGarbageByType[v.element.type.toString()] = list
                }
                list.add(v)
            }
        }

        return coll.mapNotNull {
            var target: Instance? = byKeys[it.props.key!!]
            if (target == null) {
                val g = reusableGarbageByType[it.type.toString()]
                if (g != null && !g.isEmpty()) {
                    target = g.last()
                    g.removeAt(g.size - 1)
                }
            }
            reconcileImpl(target, it, env)
        }
    }

    private fun assignKeys(elements: List<NElement<*>>) {
        val indices = fastStringMap<Int>()
        for (element in elements) {
            if (element.props.key == null) {
                val typeStr = element.type.toString()
                val i = indices[typeStr]
                val index = if (i == null) {
                    indices[typeStr] = 0
                    0
                } else {
                    i
                }
                element.props.key = (element.type to index).toString()
                indices[typeStr] = index + 1
            }
        }
    }

    private fun reconcileUser(userComponent: UserInstance?, e: NElement<*>, env: Env, isForceUpate: Boolean): Instance {
        var view = when {
            userComponent == null -> null
            userComponent.element.type != e.type -> null
            else -> userComponent.view as View<Props>?
        }
        val renderContext = RenderContextImpl()
        val substElement = when (e) {
            is NElement.Fun<*> -> (e as NElement.Fun<Props>).type(e.props)
            is NElement.Class<*> -> {
                if (view == null) {
                    view = e.type() as View<Props>
                }

                view.run {
                    if (_props == null || isForceUpate || shouldUpdate(e.props)) {
                        _props = e.props
                        renderContext.render()
                        renderContext.createdElements.let { when(it.size) {
                            0 -> null
                            1 -> it.first()
                            else -> error("Single element expected from render function")
                        }}
                    } else {
                        _props = e.props
                        userComponent!!.subst?.element
                    }
                }
            }
            else -> {
                error("reconcileUser expects Class or Fun elementm got $e")
            }
        }
        assignKeys(renderContext.reifiedElements)
        val oldByKeys = userComponent?.byKeys ?: fastStringMap()
        val newComponents = reconcileByKeys(oldByKeys, renderContext.reifiedElements.map { it.e }, env)
        val newEnv = Env(env, renderContext.reifiedElements.zip(newComponents) { reified, instance ->
            reified.id to instance
        }.toMap())
        val newSubst = reconcileImpl(userComponent?.subst, substElement, newEnv)
        val newByKeys = fastStringMap<Instance>()
        for (c in newComponents) {
            newByKeys[c.element.props.key!!] = c
        }

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
        view?.context = graph
        view?.instance = result

        val reference = UserReference(result)
        oldByKeys.forEach { _, c ->
            c.backrefs.remove(reference)
        }
        newByKeys.forEach { _, c ->
            c.backrefs.add(reference)
        }
        oldByKeys.forEach { _, c ->
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
            is UserInstance -> c.byKeys.forEach { _, instance ->
                instance.backrefs.remove(UserReference(c))
                if (instance.backrefs.isEmpty()) {
                    gc(instance)
                }
            }
            is HostInstance -> {
                c.componentProps.forEach { attr, comp ->
                    comp?.backrefs?.remove(AttrReference(c, attr))
                    if (comp?.backrefs?.isEmpty() == true) {
                        gc(comp)
                    }
                }
                c.childrenProps.forEach { attr, children ->
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


    private fun reconcileHost(hostInstance: HostInstance?, e: NElement.HostElement<*>, env: Env): HostInstance {
        if (hostInstance != null && hostInstance.element.type != e.type) {
            supply(Update.DestroyNode(hostInstance.node!!))
            return reconcileHost(null, e, env)
        }
        val node: Int = hostInstance?.node ?: graph.makeNode()
        if (hostInstance?.node == null) {
            supply(Update.MakeNode(node, (e.type as HostComponentType<*>).type, e.props.constructorParameters))
        }

        val childrenMap = fastStringMap<List<Instance>>()
        val oldChildrenMap = hostInstance?.childrenProps
        val newChildrenMap = e.props.childrenMap
        forEachKey(newChildrenMap, oldChildrenMap) { attr ->
            val newChildren = e.props.childrenMap[attr]
            val newChildrenNotNull = newChildren?.filterNotNull() ?: emptyList()
            assignKeys(newChildrenNotNull)
            val oldChildren = oldChildrenMap?.get(attr) ?: emptyList()
            childrenMap[attr] = reconcileList(node, attr, oldChildren, newChildrenNotNull, env)
        }

        val componentsMap = fastStringMap<Instance?>()
        val oldComponentsMap = hostInstance?.componentProps
        val newComponentsMap = e.props.componentsMap
        forEachKey(newComponentsMap, oldComponentsMap) { attr ->
            val element = e.props.componentsMap[attr]
            val oldComponent = oldComponentsMap?.get(attr)
            val newComponent = reconcileImpl(oldComponent, element, env)
            componentsMap[attr] = newComponent
            if (oldComponent?.node != newComponent?.node) {
                supply(Update.SetAttr(node, attr, newComponent?.node))
            }
        }

        val valuesMap = fastStringMap<Any?>()
        val oldValuesMap = hostInstance?.valueProps
        val newValuesMap = e.props.valuesMap
        forEachKey(newValuesMap, oldValuesMap) { attr ->
            val value = e.props.valuesMap[attr]
            valuesMap[attr] = value
            val oldValue = oldValuesMap?.get(attr)
            if (value != oldValue) {
                supply(Update.SetAttr(node, attr, value))
            }
        }

        val oldCallbacks = graph.callbacksTable[node] ?: fastStringMap()
        val newCallbacks = e.props.callbacks
        forEachKey(oldCallbacks, newCallbacks) { attr ->
            val oldCB = oldCallbacks[attr]
            val newCB = e.props.callbacks[attr]
            if (oldCB == null && newCB != null) {
                supply(Update.SetCallback(node, attr, newCB.async))
            } else if (oldCB != null && newCB == null) {
                supply(Update.RemoveCallback(node, attr))
            }
            if (newCB != null) {
                oldCallbacks[attr] = newCB
            } else {
                oldCallbacks.remove(attr)
            }
        }
        graph.callbacksTable[node] = oldCallbacks
        val result = hostInstance?.apply {
            this.element = e
            this.childrenProps = childrenMap
            this.componentProps = componentsMap
            this.valueProps = valuesMap
            this.env = env
            this.node = node
        } ?: HostInstance(
                element = e,
                childrenProps = childrenMap,
                componentProps = componentsMap,
                valueProps = valuesMap,
                node = node,
                backrefs = hashSetOf(),
                env = env)

        forEachKey(oldChildrenMap, newChildrenMap) { attr ->
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
        forEachKey(oldComponentsMap, newComponentsMap) { attr ->
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

    private fun updateOrder(node: Int, attr: String, oldList: List<Int>, newList: List<Int>) {
        val lcs = lcs(oldList.toIntArray(), newList.toIntArray()).toHashSet()
        val oldNodesSet = fastIntMap<Int>()
        for (n in oldList) {
            oldNodesSet[n] = n
        }
        val allNodes = hashSetOf<Int>()
        oldList.toCollection(allNodes)
        newList.toCollection(allNodes)
        for (c in allNodes) {
            if (!lcs.contains(c) && oldNodesSet.containsKey(c)) {
                supply(Update.Remove(node = node, value = c, attr = attr))
            }
        }
        newList.forEachIndexed { i, c ->
            if (!lcs.contains(c)) {
                supply(Update.Add(node = node, value = c, attr = attr, index = i))
            }
        }
    }


    private fun reconcileList(node: Int, attr: String, components: List<Instance>, elements: List<NElement<*>>, env: Env): List<Instance> {
        val componentsByKeys = fastStringMap<Instance>()
        for (c in components) {
            componentsByKeys[c.element.props.key!!] = c
        }
        val reconciledList = reconcileByKeys(componentsByKeys, elements, env)
        updateOrder(node, attr, components.mapNotNull { it.node }, reconciledList.mapNotNull { it.node })
        return reconciledList
    }
}

inline fun <T1 : Any?, T2 : Any?> forEachKey(m1: MutableMapLike<String, T1>?, m2: MutableMapLike<String, T2>?, crossinline block: (String) -> Unit) {
    m1?.forEach { attr, _ ->
        block(attr)
    }
    m2?.forEach { attr, _ ->
        if (m1?.containsKey(attr) != true) {
            block(attr)
        }
    }
}

class GraphState(val platform: Platform, val driver: PlatformDriver) {
    private var nextNode: Int = 0
    internal val callbacksTable: MutableMapLike<Int, MutableMapLike<String, CallbackInfo<*>>> = fastIntMap()

    internal fun makeNode() = nextNode++

    fun forceUpdate(c: UserInstance) {
        ReconciliationState(this).forceUpdate(c)
    }

    fun handleEvent(e: EventInfo) {
        val callbackInfo = callbacksTable[e.source]?.get(e.name)
        if (callbackInfo != null) {
            (callbackInfo as CallbackInfo<Event>).cb(e.event)
        }
    }

    fun mount(id: String, buildRoot: RenderContext.() -> Unit) {
        val root = RenderContextImpl().run {
            buildRoot()
            val element = createdElements.single()
            rootCT with RootProps(id, element)
        }

        ReconciliationState(this).mountRoot(root)
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

abstract class HostProps : Props() {
    val constructorParameters = fastStringMap<Any?>()

    val componentsMap = fastStringMap<NElement<*>?>()
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

    val valuesMap = fastStringMap<Any?>()
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

    val childrenMap = fastStringMap<List<NElement<*>?>>()
    fun <T : List<NElement<*>>> elementList(): ReadWriteProperty<Any, T> =
            object : ReadWriteProperty<Any, T> {
                override fun getValue(thisRef: Any, property: KProperty<*>): T {
                    if (!childrenMap.containsKey(property.name)) {
                        childrenMap[property.name] = mutableListOf()
                    }
                    return childrenMap[property.name] as T
                }

                override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
                    childrenMap[property.name] = value
                }
            }

    val callbacks = fastStringMap<CallbackInfo<*>>()
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

data class ListReference(override val referer: HostInstance, val attr: String, val index: Int) : Reference
data class AttrReference(override val referer: HostInstance, val attr: String) : Reference
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
                   var byKeys: MutableMapLike<String, Instance> = fastStringMap(),
                   var subst: Instance?) : Instance(element, node, backrefs, env)

class HostInstance(element: NElement<*>,
                   node: Int,
                   backrefs: MutableSet<Reference>,
                   env: Env,
                   var childrenProps: MutableMapLike<String, List<Instance>>,
                   var componentProps: MutableMapLike<String, Instance?>,
                   var valueProps: MutableMapLike<String, Any?>) : Instance(element, node, backrefs, env)
