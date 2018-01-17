package noria.components

import noria.*
import kotlin.reflect.*

val TextField = PlatformComponentType<TextFieldProps>()

data class TextFieldProps(
        val bind: KMutableProperty0<String>,
        val disabled: Boolean
) : PlatformProps()

fun RenderContext.textField(bind: KMutableProperty0<String>, disabled: Boolean = false, b: TextFieldProps.() -> Unit = {}) {
    x(TextField, TextFieldProps(bind, disabled).apply(b))
}
