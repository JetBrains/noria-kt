package org.jetbrains.noria

class BoxProps(val alignItems: String = "", val alignSelf: String = "") : ContainerProps()

class VBox : Container<BoxProps>() {
    override fun render(): View<*> = renderVBox(props)
}

inline fun vbox(alignItems: String = "", alignSelf: String = "", builder: BoxProps.() -> Unit): VBox = node(BoxProps(alignItems, alignSelf), builder)

class HBox : Container<BoxProps>() {
    override fun render(): View<*> = renderHBox(props)
}

inline fun hbox(builder: BoxProps.() -> Unit): HBox = node(BoxProps(), builder)
