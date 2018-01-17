package noria.components

import noria.*
import kotlin.reflect.*

val TextField = PlatformComponentType<TextFieldProps>()

data class TextFieldProps(
        val bind: KMutableProperty0<String>,
        val disabled: Boolean,
        val onEnter: () -> Unit
)

fun RenderContext.textField(bind: KMutableProperty0<String>, disabled: Boolean = false, onEnter: () -> Unit = {}) {
    x(TextField, TextFieldProps(bind, disabled, onEnter))
}
