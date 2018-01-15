package noria.views

import noria.*
import noria.components.*

val labelCT = HostComponentType<InputProps>("label")

class CheckBox : View<CheckBoxProps>() {
    override fun RenderContext.render() {
        x(labelCT) {
            x(inputCT) {
                type = "checkbox"
                checked = props.bind.get()

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
