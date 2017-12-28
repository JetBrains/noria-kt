package noria.swing.components

import noria.*
import noria.components.*

class ActionEvent() : Event()

class nButtonProps : HostProps() {
    var text: String by value()
    var actionListener by handler<ActionEvent>()
    var enabled: Boolean by value()
}

val nButton = HostComponentType<nButtonProps>("javax.swing.JButton")

class NJButton : View<ButtonProps>() {
    override fun RenderContext.render() {
        x(nButton) {
            text = props.title
            if (props.disabled) {
                enabled = false
            }

            actionListener = CallbackInfo(false) {
                props.action()
            }
        }
    }
}
