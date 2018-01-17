package noria.swing

import noria.*
import noria.components.*
import noria.swing.components.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.*
import kotlin.reflect.*

object SwingPlatform : Platform() {
    init {
        register(Root, ::SwingRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)

        register(Label, NLabel::class) {
            set(NLabel::setText, it.text)
            set(NLabel::events, it.events)
        }

        register(Button, NButton::class) { props ->
            set(NButton::setText, props.title)
            set(NButton::setEnabled, !props.disabled)
            set(NButton::_action, props.action)
        }

        register(TextField, NTextField::class) { props ->
            set(NTextField::setText, props.bind.getter.call())
            set(NTextField::setEnabled, !props.disabled)
            set(NTextField::onTextChanged, props.bind::set)
            set(NTextField::events, props.events)
        }

        register(CheckBox, NCheckBox::class) { props ->
            set(NCheckBox::setText, props.text)
            set(NCheckBox::setSelected, props.bind.getter.call())
            set(NCheckBox::setEnabled, !props.disabled)
            set(NCheckBox::onChange, props.bind::set)
        }
    }

    private fun<Props, Bean:Any> register(pct: PlatformComponentType<Props>, bean: KClass<Bean>, build: BeanHostProps<Bean>.(Props) -> Unit) {
        return register(pct) { props ->
            ManagedBeanView(props, HostComponentType(bean.qualifiedName!!), build)
        }
    }
}


interface ControlWithEvents {
    var events: Events?

    fun setupListeners() {
        this as JComponent

        addMouseListener(object : MouseListener {
            override fun mouseReleased(e: MouseEvent?) {
            }

            override fun mouseEntered(e: MouseEvent?) {
                events?.onMouseEntered?.invoke()
            }

            override fun mouseClicked(e: MouseEvent?) {
                events?.onClick?.invoke()
            }

            override fun mouseExited(e: MouseEvent?) {
                events?.onMouseExited?.invoke()
            }

            override fun mousePressed(e: MouseEvent?) {
            }
        })

        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
            }

            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    events?.onEnter?.invoke()
                }
            }

            override fun keyReleased(e: KeyEvent) {
            }
        })

        addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent) {
                events?.onFocusLost?.invoke()
            }

            override fun focusGained(e: FocusEvent) {
                events?.onFocusGained?.invoke()
            }
        })
    }
}

class NLabel : JLabel(), ControlWithEvents {
    override var events: Events? = null

    init {
        setupListeners()
    }
}

class NButton : JButton(), ControlWithEvents {
    override var events: Events? = null

    var _action: (() -> Unit)? = null

    init {
        addActionListener {
            _action?.invoke()
        }

        setupListeners()
    }
}

class NTextField : JTextField(), ControlWithEvents {
    override var events: Events? = null
    var onTextChanged: ((newText: String) -> Unit)? = null
    val currentText: String get() = document.getText(0, document.length)
    override fun setText(t: String?) {
        if (t != currentText) {
            super.setText(t)
        }
    }

    init {
        document.addDocumentListener(object: DocumentListener {
            override fun changedUpdate(e: DocumentEvent) {

            }

            override fun insertUpdate(e: DocumentEvent) {
                if (!SwingDriver.listenersSuppressed) {
                    onTextChanged?.invoke(currentText)
                }
            }

            override fun removeUpdate(e: DocumentEvent) {
                if (!SwingDriver.listenersSuppressed) {
                    onTextChanged?.invoke(currentText)
                }
            }
        })

        setupListeners()
    }
}


class NCheckBox : JCheckBox(), ControlWithEvents {
    override var events: Events? = null

    var onChange: ((v:  Boolean) -> Unit)? = null

    init {
        addChangeListener {
            if (!SwingDriver.listenersSuppressed) {
                onChange?.invoke(isSelected)
            }
        }

        setupListeners()
    }
}
