package noria.views

import org.jetbrains.noria.ButtonProps
import org.jetbrains.noria.CallbackInfo
import org.jetbrains.noria.NElement
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.View
import org.jetbrains.noria.with

class Button : View<ButtonProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "input" with InputProps().apply {
            type = "button"
            value = props.title

            if (props.disabled) {
                disabled = "true"
            }

            click = CallbackInfo(true) {
                props.action()
            }
        }
    }
}
