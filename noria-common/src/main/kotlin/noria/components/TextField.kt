package noria.components

import noria.*
import kotlin.reflect.*

val TextField = PlatformComponentType<TextFieldProps>()

data class TextFieldProps(
        val bind: KMutableProperty0<String>,
        val disabled: Boolean
)

fun RenderContext.textField(bind: KMutableProperty0<String>, disabled: Boolean = false) {
    x(TextField, TextFieldProps(bind, disabled))
}
