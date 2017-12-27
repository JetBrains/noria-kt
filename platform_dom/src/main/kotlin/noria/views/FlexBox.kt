package noria.views

import org.jetbrains.noria.*

val Div = HostComponentType<DomProps>("div")
val Span = HostComponentType<DomProps>("span")
val Pre = HostComponentType<DomProps>("pre")

inline fun RenderContext.div(build: DomProps.() -> Unit) {
    Div with DomProps().apply(build)
}

inline fun RenderContext.span(build: DomProps.() -> Unit) {
    Span with DomProps().apply(build)
}

inline fun RenderContext.pre(build: DomProps.() -> Unit) {
    Pre with DomProps().apply(build)
}



class FlexBox : Container<BoxProps>() {
    override fun RenderContext.render() {
        div {
            style = buildString {
                append("display:flex;")
                append("flex-direction:${props.flexDirection};")

                props.flexWrap?.let {
                    append("flex-wrap:$it;")
                }

                props.justifyContent?.let {
                    append("justify-content:$it;")
                }

                props.alignItems?.let {
                    append("align-items:$it;")
                }

                props.alignContent?.let {
                    append("align-content:$it;")
                }
            }

            children = props.children
        }
    }
}
