package noria.views

import noria.*
import noria.components.*

val labelCT = HostComponentType<InputProps>("label")

class CheckBox(p: CheckBoxProps) : View<CheckBoxProps>(p) {
    override fun RenderContext.render() {
        x(labelCT) {
            x(inputCT) {
                type = "checkbox"
                if (props.bind.get()) {
                    checked = true
                }

                if (props.disabled) {
                    disabled = true
                }

                change = CallbackInfo(true) {
                    println(it.newValue)
                    props.bind.set(it.newValue == true)
                }
            }

            text(props.text)
        }
    }
}
