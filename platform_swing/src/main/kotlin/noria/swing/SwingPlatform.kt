package noria.swing

import noria.*
import noria.components.*
import noria.swing.components.*
import java.awt.event.*
import javax.swing.*
import kotlin.reflect.*

object SwingPlatform : Platform() {
    init {
        register(Root, ::SwingRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)

        register(Label, JLabel::class) {
            set(JLabel::setText, it.text)
        }

        register(Button, JButton::class) { props ->
            set(JButton::setText, props.title)
            set(JButton::setEnabled, !props.disabled)

            listen<ActionListener>(JButton::addActionListener, ActionListener {
                props.action()
            })
        }
    }

    private fun<Props, Bean:Any> register(pct: PlatformComponentType<Props>, bean: KClass<Bean>, build: BeanHostProps<Bean>.(Props) -> Unit) {
        return register(pct) {
            beanView(bean, build)
        }
    }
}
