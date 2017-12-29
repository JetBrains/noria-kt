package noria.swing.components

import noria.*
import java.util.*
import kotlin.reflect.*

class BeanHostProps<T: Any> : HostProps() {
    fun <V> set(settter: KFunction2<T, V, Unit>, value: V) {
        valuesMap[settter.name.removePrefix("set").decapitalize()] = value
    }

    fun <V: EventListener> listen(settter: KFunction2<T, V, Unit>, value: V) {
        valuesMap[settter.name.removePrefix("add").decapitalize()] = value
    }
}

inline fun <reified T: Any> beanHostCompnentType() = HostComponentType<BeanHostProps<T>>(T::class.qualifiedName!!)
