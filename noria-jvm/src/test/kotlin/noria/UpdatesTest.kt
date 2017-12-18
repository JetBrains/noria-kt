package noria

import org.jetbrains.noria.NElement
import org.jetbrains.noria.NSConstraint
import org.jetbrains.noria.NSViewProps
import org.jetbrains.noria.Props
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.Update
import org.jetbrains.noria.UserInstance
import org.jetbrains.noria.View
import org.jetbrains.noria.createInstance
import org.jetbrains.noria.reconcile
import org.jetbrains.noria.with
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


data class MyProps(val x: Int = 0) : Props()
class MyMacComponent: View<MyProps>() {
    override fun RenderContext.render(): NElement<*> {
        val v1 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
        }
        val v2 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
        }

        emit("NSLayoutConstraint" with NSConstraint().apply {
            view1 = v1
            view2 = v2
        })

        return "NSView" with NSViewProps().apply {
            subviews.add(v1)
            if (props.x > 0) {
                subviews.add(v2)
            }
        }
    }
}

class UpdatesTest {

    @Test fun `Reconciliation keeps the view and adds update for new subview`() {
        val (component, _) = MockPlatform.createInstance(MyMacComponent::class with MyProps())
        val view = (component as UserInstance).view as? MyMacComponent ?: fail("View expected to be MyMacComponent")

        val (newComponent, updates) = MockPlatform.reconcile(component, MyMacComponent::class with MyProps(x = 1))

        assertTrue((newComponent as UserInstance).view === view, "View is expected to be kept the same. It's just props shall be changed")
        assertEquals(1, view.props.x, "Props expected to be updated")
        assertTrue(updates.single() is Update.Add, "There should be single update of adding subview")
    }
}
