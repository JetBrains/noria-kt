package noria.views

import noria.*
import noria.components.*

class TextField(p: TextFieldProps) : View<TextFieldProps>(p) {
    override fun RenderContext.render() {
        x(inputCT) {
            type = "text"
            value = props.bind.get()
            autoFocus = true

            if (props.disabled) {
                disabled = true
            }

            input = CallbackInfo(true) {
                props.bind.set(it.newValue as? String ?: "")
            }

            change = CallbackInfo(true) {
                props.events.onEnter?.invoke()
            }

            focus = CallbackInfo(true) {
                props.events.onFocusGained?.invoke()
            }

            blur = CallbackInfo(true) {
                props.events.onFocusLost?.invoke()
            }
        }
    }
}
