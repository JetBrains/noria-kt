package noria.swing.components

import noria.*
import noria.components.*
import noria.components.Container
import java.awt.*

class PanelProps : HostProps() {
    var layout by value<LayoutManager>(true)
    var children by elementList<MutableList<NElement<*>>>()
}
val Panel = HostComponentType<PanelProps>("JPanel")

class FlexBox : Container<BoxProps>() {
    override fun RenderContext.render() {
        x(Panel) {
            if (props.flexDirection == FlexDirection.column) {
                layout = VerticalFlowLayout().apply {
                    when(props.justifyContent ?: JustifyContent.start) {
                        JustifyContent.center -> {
                            alignment = VerticalFlowLayout.MIDDLE
                        }

                        JustifyContent.start -> {
                            alignment = VerticalFlowLayout.TOP
                        }

                        JustifyContent.end -> {
                            alignment = VerticalFlowLayout.BOTTOM
                        }

                        JustifyContent.flexStart -> TODO()
                        JustifyContent.flexEnd -> TODO()
                        JustifyContent.left -> TODO()
                        JustifyContent.right -> TODO()
                        JustifyContent.baseline -> TODO()
                        JustifyContent.firstBaseline -> TODO()
                        JustifyContent.lastBaseline -> TODO()
                        JustifyContent.spaceBetween -> TODO()
                        JustifyContent.spaceAround -> TODO()
                        JustifyContent.spaceEvenly -> TODO()
                        JustifyContent.stretch -> TODO()
                        JustifyContent.safeCenter -> TODO()
                        JustifyContent.unsafeCenter -> TODO()
                    }
                }
            }
            else {
                layout = FlowLayout().apply {
                    when(props.justifyContent ?: JustifyContent.start) {
                        JustifyContent.center -> {
                            alignment = FlowLayout.CENTER
                        }

                        JustifyContent.start -> {
                            alignment = FlowLayout.LEADING
                        }

                        JustifyContent.end -> {
                            alignment = FlowLayout.TRAILING
                        }

                        JustifyContent.flexStart -> TODO()
                        JustifyContent.flexEnd -> TODO()
                        JustifyContent.left -> TODO()
                        JustifyContent.right -> TODO()
                        JustifyContent.baseline -> TODO()
                        JustifyContent.firstBaseline -> TODO()
                        JustifyContent.lastBaseline -> TODO()
                        JustifyContent.spaceBetween -> TODO()
                        JustifyContent.spaceAround -> TODO()
                        JustifyContent.spaceEvenly -> TODO()
                        JustifyContent.stretch -> TODO()
                        JustifyContent.safeCenter -> TODO()
                        JustifyContent.unsafeCenter -> TODO()
                    }
                }
            }

            children.addAll(props.children)
        }
    }
}
