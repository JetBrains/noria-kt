@file:Suppress("UNCHECKED_CAST")

package noria

import noria.utils.*


class RenderContextImpl() : RenderContext {
    internal val reifiedElements: MutableList<NElement.Reified<*>> = mutableListOf()
    val createdElements: MutableList<NElement<*>> = mutableListOf()

    override fun <T> reify(e: NElement<T>): NElement<T> {
        createdElements.remove(e)
        return NElement.Reified(Env.nextVar++, e).apply {
            reifiedElements.add(this)
        }
    }

    override fun <T> emit(e: NElement<T>) {
        createdElements.add(e)
    }
}

class Env(private val parent: Env?, private val vars: MutableMapLike<Int, Instance>) {
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
                        supply(Update.SetNodeAttr(r.referer.node!!, r.attr, newNode))
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
        reconcileImpl(null, e, Env(null, fastIntMap()))
        graph.driver.applyUpdates(updates)
    }

    //TODO remove, tests only
    fun reconcile(component: Instance?, e: NElement<*>): Pair<Instance?, List<Update>> {
        val instance = reconcileImpl(component, e, Env(null, fastIntMap()))
        return instance to updates
    }

    private fun reconcileImpl(component: Instance?, e: NElement<*>?, env: Env): Instance? =
            when {
                e == null -> null
                e is NElement.HostElement<*> -> reconcileHost(component as HostInstance?, e, env)
                (e is NElement.Class<*>) || (e is NElement.Fun<*>) -> reconcileUser(component as UserInstance?, e, env, false)
                e is NElement.PlatformDispatch<*> -> reconcileImpl(component, createViewElement(graph.platform.resolve(e.type as PlatformComponentType<Any?>), e.props, e.key), env)
                e is NElement.Reified<*> -> env.lookup(e.id)
                else -> throw IllegalArgumentException("don't know how to reconcile $e")
            }

    private fun reconcileByKeys(byKeys: MutableMapLike<String, Instance>, coll: Collection<NElement<*>>, env: Env): List<Instance> {
        val newKeysSet = fastStringMap<String>()
        for (e in coll) {
            newKeysSet[e.key!!] = e.key!!
        }

        val reusableGarbageByType = fastStringMap<MutableList<Instance>>()
        byKeys.forEach { _, v ->
            if (!newKeysSet.containsKey(v.element.key!!) && v.backrefs.size == 1) {
                var list = reusableGarbageByType[v.element.type.toString()]
                if (list == null) {
                    list = mutableListOf()
                    reusableGarbageByType[v.element.type.toString()] = list
                }
                list.add(v)
            }
        }

        return coll.mapNotNull {
            var target: Instance? = byKeys[it.key!!]
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
            if (element.key == null) {
                val typeStr = element.type.toString()
                val i = indices[typeStr]
                val index = if (i == null) {
                    indices[typeStr] = 0
                    0
                } else {
                    i
                }
                element.key = (element.type to index).toString()
                indices[typeStr] = index + 1
            }
        }
    }

    private fun reconcileUser(userComponent: UserInstance?, e: NElement<*>, env: Env, isForceUpate: Boolean): Instance {
        var view = when {
            userComponent == null -> null
            userComponent.element.type != e.type -> null
            else -> userComponent.view as View<Any?>?
        }
        val renderContext = RenderContextImpl()
        val substElement = when (e) {
            is NElement.Fun<*> -> (e as NElement.Fun<Any>).type(e.props)
            is NElement.Class<*> -> {
                var newView = false
                if (view == null) {
                    view = (e.type as Constructor<Any?>) (e.props)
                    newView = true
                }

                view.run {
                    if (newView || isForceUpate || shouldUpdate(e.props)) {
                        props = e.props
                        renderContext.render()
                        renderContext.createdElements.let { when(it.size) {
                            0 -> null
                            1 -> it.first()
                            else -> error("Single element expected from render function")
                        }}
                    } else {
                        props = e.props
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
        val newEnvMap = fastIntMap<Instance>()
        renderContext.reifiedElements.forEachIndexed {i, c ->
            newEnvMap[c.id] = newComponents[i]
        }
        val newEnv = Env(env, newEnvMap)
        val newSubst = reconcileImpl(userComponent?.subst, substElement, newEnv)
        val newByKeys = fastStringMap<Instance>()
        for (c in newComponents) {
            newByKeys[c.element.key!!] = c
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
                supply(Update.SetNodeAttr(node, attr, newComponent?.node))
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
        oldList.forEach { c ->
            if (!lcs.contains(c)) {
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
            componentsByKeys[c.element.key!!] = c
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

class GraphState(val platform: Platform, val driver: Host) {
    private var nextNode: Int = 0
    internal val callbacksTable: MutableMapLike<Int, MutableMapLike<String, CallbackInfo<*>>> = fastIntMap()

    private val updateQueue : MutableMapLike<Int, UserInstance> = fastIntMap()

    internal fun makeNode() = nextNode++

    fun forceUpdate(c: UserInstance) {
        c.node?.let {
            synchronized(updateQueue) {
                updateQueue[it] = c

                scheduleOnce {
                    drainUpdateQueue()
                }
            }
        }
    }

    fun drainUpdateQueue() {
        synchronized(updateQueue) {
            updateQueue.forEach { _, instance ->
                ReconciliationState(this).forceUpdate(instance)
            }
            updateQueue.clear()
        }
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
            createPlatformElement(Root, RootProps(id, element))
        }

        ReconciliationState(this).mountRoot(root)
    }
}


class EventInfo(var source: Int,
                val name: String,
                val event: Event)

typealias Handler<T> = (T) -> Unit

data class CallbackInfo<in T : Event>(val async: Boolean, val cb: Handler<T>)

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
