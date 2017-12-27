package noria.views

import org.jetbrains.noria.*
import org.jetbrains.noria.components.*

val Div = HostComponentType<DomProps>("div")
val Span = HostComponentType<DomProps>("span")
val Pre = HostComponentType<DomProps>("pre")

inline fun RenderContext.div(build: DomProps.() -> Unit) {
    x(Div, null, build)
}

inline fun RenderContext.span(build: DomProps.() -> Unit) {
    x(Span, DomProps().apply(build))
}

inline fun RenderContext.pre(build: DomProps.() -> Unit) {
    x(Pre, DomProps().apply(build))
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
