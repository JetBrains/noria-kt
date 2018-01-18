package noria.swing.components

import noria.*
import kotlin.reflect.*

class BeanHostProps<T: Any> : HostProps() {
    fun <V> set(settter: KFunction2<T, V, Unit>, value: V) {
        valuesMap[settter.name.removePrefix("set").decapitalize()] = value
    }

    fun <V> set(p: KMutableProperty1<T, V>, value: V) {
        valuesMap[p.name] = value
    }

    var children by elementList<MutableList<NElement<*>>>()
}

class ManagedBeanView<B:Any, Props>(props: Props, val type: HostComponentType<BeanHostProps<B>>, val build: BeanHostProps<B>.(Props) -> Unit) : View<Props>(props) {
    override fun RenderContext.render() {
        val bhp = BeanHostProps<B>().apply {
            build(props)
        }

        x(type, bhp)
    }
}

