package noria.swing.components

import noria.*
import noria.components.*
import java.awt.event.*
import javax.swing.*

val nButton = beanHostCompnentType<JButton>()
class NJButton : View<ButtonProps>() {
    override fun RenderContext.render() {
        x(nButton) {
            set(JButton::setText, props.title)

            if (props.disabled) {
                set(JButton::setEnabled, false)
            }

            listen<ActionListener>(JButton::addActionListener, ActionListener {
                props.action()
            })
        }
    }
}
