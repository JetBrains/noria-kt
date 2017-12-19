package noria

import org.jetbrains.noria.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

data class Click(val buttonNum: Int, val clickCount: Int) : Event()

class NSViewProps : PrimitiveProps() {
    val subviews: MutableList<NElement<NSViewProps>> by elementList()
    var onClick by handler<Click>()
}

class NSConstraint : PrimitiveProps() {
    var view1: NElement<NSViewProps> by element()
    var view2: NElement<NSViewProps> by element()
}

data class MyProps(val x: Int = 0) : Props()
class MyMacComponent : View<MyProps>() {
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

class TestProps1 : PrimitiveProps() {
    val children: MutableList<NElement<*>> by elementList()
}

class UpdatesTest {

    @Test
    fun `recnciliation of sequences`() {
        val c = ReconciliationContext(MockPlatform)
        val e0 = "div" with TestProps1().apply {
            children.add("hey" with TestProps1().apply { key = "hey" })
            children.add("hoy" with TestProps1().apply { key = "hoy" })
        }
        val updates0 = c.reconcile(e0)
        assertEquals(listOf(
                Update.MakeNode(node = 0, type = "div", parameters = emptyMap()),
                Update.MakeNode(node = 1, type = "hey", parameters = emptyMap()),
                Update.MakeNode(node = 2, type = "hoy", parameters = emptyMap()),
                Update.Add(node = 0, attr = TestProps1::children, child = 1, index = 0),
                Update.Add(node = 0, attr = TestProps1::children, child = 2, index = 1)), updates0)
        val updates1 = c.reconcile(e0)
        assert(updates1.isEmpty())
        val e1 = "div" with TestProps1().apply {
            children.add("hiy" with TestProps1().apply { key = "hiy" })
            children.add("hoy" with TestProps1().apply { key = "hoy" })
            children.add("fu" with TestProps1().apply { key = "fu" })
        }
        val updates2 = c.reconcile(e1)
        assertEquals(listOf(
                Update.MakeNode(node = 3, type = "hiy", parameters = emptyMap()),
                Update.MakeNode(node = 4, type = "fu", parameters = emptyMap()),
                Update.Remove(node = 0, attr = TestProps1::children, child = 1),
                Update.Add(node = 0, attr = TestProps1::children, child = 3, index = 0),
                Update.Add(node = 0, attr = TestProps1::children, child = 4, index = 2),
                Update.DestroyNode(node = 1)), updates2)
        val e2 = "div" with TestProps1().apply {
            children.add("hoy" with TestProps1().apply { key = "hoy" })
            children.add("hiy" with TestProps1().apply { key = "hiy" })
            children.add("fu" with TestProps1().apply { key = "fu" })
        }
        val updates3 = c.reconcile(e2)
        assertEquals(listOf(
                Update.Remove(node = 0, attr =  TestProps1::children, child = 2),
                Update.Add(node = 0, attr =  TestProps1::children, child = 2, index = 0)), updates3)

    }


    @Test
    fun `Reconciliation keeps the view and adds update for new subview`() {
        val c = ReconciliationContext(MockPlatform)
        val updates0 = c.reconcile(MyMacComponent::class with MyProps())
        val updates = c.reconcile(MyMacComponent::class with MyProps(x = 1))
        assertTrue(updates.single() is Update.Add, "There should be single update of adding subview")
    }
}
