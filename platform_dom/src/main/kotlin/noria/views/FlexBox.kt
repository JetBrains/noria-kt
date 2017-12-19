package noria.views

import org.jetbrains.noria.BoxProps
import org.jetbrains.noria.Container
import org.jetbrains.noria.NElement
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.with


class FlexBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "div" with DomProps().apply {
            css = buildString {
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
