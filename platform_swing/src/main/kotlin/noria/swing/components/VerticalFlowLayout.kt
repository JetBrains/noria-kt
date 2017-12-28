package noria.swing.components

import org.intellij.lang.annotations.MagicConstant

import java.awt.*
import java.io.Serializable

class VerticalFlowLayout @JvmOverloads constructor(alignment: Int = TOP, private val hGap: Int = 5, private val vGap: Int = 5, var horizontalFill: Boolean = true, var verticalFill: Boolean = false) : FlowLayout(), Serializable {

    init {
        setAlignment(alignment)
    }

    override fun layoutContainer(container: Container) {
        val insets = container.insets
        val i = container.size.height - (insets.top + insets.bottom + vGap * 2)
        val j = container.size.width - (insets.left + insets.right + hGap * 2)
        val k = container.componentCount
        var l = insets.left + hGap
        var i1 = 0
        var j1 = 0
        var k1 = 0
        for (l1 in 0 until k) {
            val component = container.getComponent(l1)
            if (!component.isVisible) continue
            val dimension = component.preferredSize
            if (verticalFill && l1 == k - 1) {
                dimension.height = Math.max(i - i1, component.preferredSize.height)
            }
            if (horizontalFill) {
                component.setSize(j, dimension.height)
                dimension.width = j
            } else {
                component.setSize(dimension.width, dimension.height)
            }
            if (i1 + dimension.height > i) {
                a(container, l, insets.top + vGap, j1, i - i1, k1, l1)
                i1 = dimension.height
                l += hGap + j1
                j1 = dimension.width
                k1 = l1
                continue
            }
            if (i1 > 0) {
                i1 += vGap
            }
            i1 += dimension.height
            j1 = Math.max(j1, dimension.width)
        }

        a(container, l, insets.top + vGap, j1, i - i1, k1, k)
    }

    private fun a(container: Container, i: Int, j: Int, k: Int, l: Int, i1: Int, j1: Int) {
        var j = j
        val k1 = alignment
        if (k1 == 1) {
            j += l / 2
        }
        if (k1 == 2) {
            j += l
        }
        for (l1 in i1 until j1) {
            val component = container.getComponent(l1)
            val dimension = component.size
            if (component.isVisible) {
                val i2 = i + (k - dimension.width) / 2
                component.setLocation(i2, j)
                j += vGap + dimension.height
            }
        }
    }

    override fun minimumLayoutSize(container: Container): Dimension {
        val dimension = Dimension(0, 0)
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            if (!component.isVisible) continue
            val dimension1 = component.minimumSize
            dimension.width = Math.max(dimension.width, dimension1.width)
            if (i > 0) {
                dimension.height += vGap
            }
            dimension.height += dimension1.height
        }
        addInsets(dimension, container)
        return dimension
    }

    override fun preferredLayoutSize(container: Container): Dimension {
        val dimension = Dimension(0, 0)
        for (i in 0 until container.componentCount) {
            val component = container.getComponent(i)
            if (!component.isVisible) continue
            val dimension1 = component.preferredSize
            dimension.width = Math.max(dimension.width, dimension1.width)
            if (i > 0) {
                dimension.height += vGap
            }
            dimension.height += dimension1.height
        }
        addInsets(dimension, container)
        return dimension
    }

    private fun addInsets(dimension: Dimension, container: Container) {
        addTo(dimension, container.insets)
        dimension.width += hGap + hGap
        dimension.height += vGap + vGap
    }

    companion object {
        val BOTTOM = 2
        val MIDDLE = 1
        val TOP = 0

        fun addTo(dimension: Dimension, insets: Insets?) {
            if (insets != null) {
                dimension.width += insets.left + insets.right
                dimension.height += insets.top + insets.bottom
            }
        }
    }

}
