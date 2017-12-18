package org.jetbrains.noria

enum class FlexDirection {
    row, column, rowReverse, columnReverse;

    override fun toString() = name.hyphenize()
}

enum class FlexWrap {
    nowrap, wrap, wrapReverse;

    override fun toString() = name.hyphenize()
}

class BoxProps() : ContainerProps() {
    var flexDirection: FlexDirection = FlexDirection.row
    var flexWrap: FlexWrap? = null
    var justifyContent: JustifyContent? = null

    var alignItems: Align? = null
    var alignContent: Align? = null
}

enum class Align {
    // Basic keywords
    auto,
    stretch,
    center,
    flexStart,
    flexEnd,
    baseline;

    override fun toString() = name.hyphenize()
}

enum class JustifyContent {
    center,
    start,
    end,
    flexStart,
    flexEnd,
    left,
    right,
    baseline,
    firstBaseline,
    lastBaseline,
    spaceBetween,
    spaceAround,
    spaceEvenly,
    stretch,
    safeCenter,
    unsafeCenter;

    override fun toString() = name.hyphenize()
}

class VBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = renderVBox(props)
}

inline fun vbox(builder: BoxProps.() -> Unit) = VBox::class.with(BoxProps().apply {
    flexDirection = FlexDirection.column
    builder()
})

class HBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = renderHBox(props)
}

inline fun hbox(builder: BoxProps.() -> Unit) = HBox::class.with(BoxProps().apply {
    flexDirection = FlexDirection.row
    builder()
})
