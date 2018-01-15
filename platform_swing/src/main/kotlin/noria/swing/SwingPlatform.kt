package noria.swing

import noria.*
import noria.components.*
import noria.swing.components.*
import javax.swing.*
import javax.swing.event.*
import kotlin.reflect.*

object SwingPlatform : Platform() {
    init {
        register(Root, ::SwingRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)

        register(Label, JLabel::class) {
            set(JLabel::setText, it.text)
        }

        register(Button, NButton::class) { props ->
            set(NButton::setText, props.title)
            set(NButton::setEnabled, !props.disabled)
            set(NButton::onClick, props.action)
        }

        register(TextField, NTextField::class) { props ->
            set(NTextField::setText, props.bind.getter.call())
            set(NTextField::setEnabled, !props.disabled)
            set(NTextField::onTextChanged, {props.bind.set(it)})
        }
    }

    private fun<Props, Bean:Any> register(pct: PlatformComponentType<Props>, bean: KClass<Bean>, build: BeanHostProps<Bean>.(Props) -> Unit) {
        return register(pct) {
            beanView(bean, build)
        }
    }
}

class NButton : JButton() {
    var onClick: (() -> Unit)? = null

    init {
        addActionListener {
            onClick?.invoke()
        }
    }
}

class NTextField : JTextField() {
    var onTextChanged: ((newText: String) -> Unit)? = null
    val currentText: String get() = document.getText(0, document.length)

    init {
        document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent) {
            }

            override fun insertUpdate(e: DocumentEvent) {
                onTextChanged?.invoke(currentText)
            }

            override fun removeUpdate(e: DocumentEvent) {
                onTextChanged?.invoke(currentText)
            }
        })
    }
}
