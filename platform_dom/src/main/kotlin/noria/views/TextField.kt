package noria.views

import noria.*
import noria.components.*

class TextField : View<TextFieldProps>() {
    override fun RenderContext.render() {
        x(inputCT) {
            type = "text"
            value = props.bind.get()

            if (props.disabled) {
                disabled = "true"
            }

            change = CallbackInfo(true) {
                props.bind.set(it.newValue ?: "")
            }
        }
    }
}
