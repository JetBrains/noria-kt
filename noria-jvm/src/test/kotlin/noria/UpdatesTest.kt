package noria

import org.jetbrains.noria.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

data class Click(val buttonNum: Int, val clickCount: Int) : Event()

class NSViewProps: PrimitiveProps() {
    val subviews: MutableList<NElement<NSViewProps>> by elementList()
    var onClick by handler<Click>()
}

class NSConstraint: PrimitiveProps() {
    var view1: NElement<NSViewProps> by element()
    var view2: NElement<NSViewProps> by element()
}

data class MyProps(val x: Int = 0) : Props()
class MyMacComponent: View<MyProps>() {
    override fun RenderContext.render(): NElement<*> {
        val v1 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
        }
        val v2 = "NSView" with NSViewProps().apply {
            subviews.add("NSView" with NSViewProps())
            onClick = CallbackInfo(true) { event ->
                println("hello")
            }
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
        val c = ReconciliationContext(MockPlatform)
        val updates0 = c.reconcile(MyMacComponent::class with MyProps())
        val updates = c.reconcile(MyMacComponent::class with MyProps(x = 1))
        assertTrue(updates.single() is Update.Add, "There should be single update of adding subview")
    }
}
