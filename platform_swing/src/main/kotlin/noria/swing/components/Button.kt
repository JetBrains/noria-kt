package noria.swing.components

import noria.*
import noria.components.*
import java.awt.event.*

class nButtonProps : HostProps() {
    var text: String by value()
    var enabled: Boolean by value()
    var actionListener: ActionListener by value()
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
