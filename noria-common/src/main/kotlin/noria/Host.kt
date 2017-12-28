package noria

import noria.utils.*
import kotlin.properties.*
import kotlin.reflect.*

interface Host {
    fun applyUpdates(updates: List<Update>)
}

sealed class Update {
    data class MakeNode(val node: Int, val type: String, val parameters: MutableMapLike<String, Any?>) : Update()
    data class SetAttr(val node: Int, val attr: String, val value: Any?) : Update()
    data class SetNodeAttr(val node: Int, val attr: String, val value: Int?) : Update()
    data class SetCallback(val node: Int, val attr: String, val async: Boolean) : Update()
    data class RemoveCallback(val node: Int, val attr: String) : Update()
    data class Add(val node: Int, val attr: String, val value: Any?, val index: Int) : Update()
    data class Remove(val node: Int, val attr: String, val value: Any?) : Update()
    data class DestroyNode(val node: Int) : Update()
}

abstract class HostProps {
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
data class HostComponentType<T : HostProps>(val type: String) : ComponentType<T>
