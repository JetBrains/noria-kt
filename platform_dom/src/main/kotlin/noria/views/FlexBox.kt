package noria.views

import org.jetbrains.noria.*

val Div = HostComponentType<DomProps>("div")

class FlexBox : Container<BoxProps>() {
    override fun RenderContext.render() {
        Div with DomProps().apply {
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
