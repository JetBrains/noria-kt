package noria.views

import noria.*
import noria.components.*

val inputCT = HostComponentType<InputProps>("input")
class Button : View<ButtonProps>() {
    override fun RenderContext.render() {
        x(inputCT) {
            type = "button"
            value = props.title

            if (props.disabled) {
                disabled = true
            }

            click = CallbackInfo(true) {
                props.action()
            }
        }
    }
}
