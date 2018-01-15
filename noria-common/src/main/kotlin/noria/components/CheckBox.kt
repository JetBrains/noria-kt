package noria.components

import noria.*
import kotlin.reflect.*

val CheckBox= PlatformComponentType<CheckBoxProps>()

data class CheckBoxProps(
        val text: String,
        val bind: KMutableProperty0<Boolean>,
        val disabled: Boolean
)

fun RenderContext.checkbox(text: String, bind: KMutableProperty0<Boolean>, disabled: Boolean = false) {
    x(CheckBox, CheckBoxProps(text, bind, disabled))
}
