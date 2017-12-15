package org.jetbrains.noria

class BoxProps(val alignItems: String = "", val alignSelf: String = "") : ContainerProps()

class VBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = renderVBox(props)
}

inline fun vbox(alignItems: String = "", alignSelf: String = "", builder: BoxProps.() -> Unit) = VBox::class.with(BoxProps(alignItems, alignSelf).apply(builder))

class HBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = renderHBox(props)
}

inline fun hbox(alignItems: String = "", alignSelf: String = "", builder: BoxProps.() -> Unit) = HBox::class.with(BoxProps(alignItems, alignSelf).apply(builder))
