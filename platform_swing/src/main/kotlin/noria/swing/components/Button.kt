package noria.swing.components

import noria.*
import noria.components.*
import java.awt.event.*

class ActionEvent() : Event()

class nButtonProps : HostProps() {
    var text: String by value()
    var enabled: Boolean by value()
    var actionListener: ActionListener by value()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is nButtonProps) return false
        return text == other.text && enabled == other.enabled
    }
}

val nButton = HostComponentType<nButtonProps>("javax.swing.JButton")

class NJButton : View<ButtonProps>() {

    override fun RenderContext.render() {
        x(nButton) {
            text = props.title
            if (props.disabled) {
                enabled = false
            }

            actionListener = ActionListener {
                props.action()
            }
        }
    }
}
