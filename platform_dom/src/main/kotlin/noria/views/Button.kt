package noria.views

import noria.*
import noria.components.*

val inputCT = HostComponentType<InputProps>("input")
class Button(p: ButtonProps) : View<ButtonProps>(p) {
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
