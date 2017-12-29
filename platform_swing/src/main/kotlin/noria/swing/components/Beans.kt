package noria.swing.components

import noria.*
import kotlin.reflect.*

class BeanHostProps<T: Any> : HostProps() {
    fun <V> set(settter: KFunction2<T, V, Unit>, value: V) {
        valuesMap[settter.name.removePrefix("add").removePrefix("set").decapitalize()] = value
    }
}

inline fun <reified T: Any> beanHostCompnentType() = HostComponentType<BeanHostProps<T>>(T::class.qualifiedName!!)
