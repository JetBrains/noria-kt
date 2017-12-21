package noria

import noria.views.DomEvent
import org.jetbrains.noria.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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


class TextNodeProps : PrimitiveProps() {
    var text: String by value()
}

data class LabelProps(val text: String) : Props()
class Label : View<LabelProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "text-node" with TextNodeProps().apply { text = props.text }
    }
}

data class SimpleContainerProps(val x: Int) : Props()
class SimpleContainer : View<SimpleContainerProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "div" with TestProps1().apply {
            (0..props.x).mapTo(children) {
                Label::class with LabelProps("$it").apply {
                    key = it
                }
            }
        }
    }
}

data class SplitProps(val left: NElement<*>,
                      val right: NElement<*>) : Props()

class SplitView : View<SplitProps>() {
    override fun RenderContext.render(): NElement<*> {
        return "div" with TestProps1().apply {
            children.add(props.left)
            children.add(props.right)
        }
    }
}

data class HOProps(val x: NElement<*>,
                   val y: NElement<*>) : Props()

class HO : View<HOProps>() {
    override fun RenderContext.render(): NElement<*> {
        val x1 = emit(props.x)
        val y1 = emit(props.y)
        return SplitView::class with (SplitProps(left = x1, right = y1))
    }
}

data class AppProps(val counter: Int, val h: (Int) -> Unit) : Props()
class AppComponent : View<AppProps>() {
    override fun RenderContext.render(): NElement<*> {
        return vbox {
            +hbox {
                justifyContent = JustifyContent.center

                +button("Click Me Once") {
                    props.h(props.counter + 1)
                }

                +button("Click Me Twice") {
                    props.h(props.counter + 1)
                }
            }

            repeat(props.counter) { n ->
                +hbox {
                    +label("$n")
                }
            }
        }

    }
}

class CapturingDriver : PlatformDriver {
    val capturedUpdates = mutableListOf<Update>()
    override fun applyUpdates(updates: List<Update>) {
        capturedUpdates.addAll(updates)
    }

    fun updates(): List<Update> {
        val copy = mutableListOf<Update>()
        copy.addAll(capturedUpdates)

        capturedUpdates.clear()
        return copy
    }
}

class UpdatesTest {
    @Test
    fun `test app component`() {
        val d = CapturingDriver()
        val c = ReconciliationContext(DOMPlatform, d)
        fun h (cnt: Int) {
            c.reconcile(AppComponent::class with AppProps(cnt, ::h))
        }

        c.reconcile(AppComponent::class with AppProps(0, ::h))
        c.handleEvent(EventInfo(source = 2, name = "click", event = DomEvent()))
    }

    @Test
    fun `testing high-order components`() {
        val d = CapturingDriver()
        val c = ReconciliationContext(DOMPlatform, d)
        c.reconcile(HO::class with HOProps(
                x = "foo" with TestProps1(),
                y = "bar" with TestProps1()))
        assertEquals(listOf(
                Update.MakeNode(node = 0, type = "foo", parameters = emptyMap()),
                Update.MakeNode(node = 1, type = "bar", parameters = emptyMap()),
                Update.MakeNode(node = 2, type = "div", parameters = emptyMap()),
                Update.Add(node = 2, attr = "children", value = 0, index = 0),
                Update.Add(node = 2, attr = "children", value = 1, index = 1)
        ), d.updates())

        c.reconcile(HO::class with HOProps(
                x = "foo" with TestProps1(),
                y = "baz" with TestProps1()))
        assertEquals(listOf(
                Update.MakeNode(node = 3, type = "baz", parameters = emptyMap()),
                Update.Remove(node = 2, attr = "children", value = 1),
                Update.Add(node = 2, attr = "children", value = 3, index = 1),
                Update.DestroyNode(node = 1)
        ), d.updates())

        c.reconcile(HO::class with HOProps(
                x = "fizz" with TestProps1(),
                y = "fuzz" with TestProps1()))
        assertEquals(listOf(
                Update.MakeNode(node = 4, type = "fizz", parameters = emptyMap()),
                Update.MakeNode(node = 5, type = "fuzz", parameters = emptyMap()),
                Update.Remove(node = 2, attr = "children", value = 0),
                Update.Remove(node = 2, attr = "children", value = 3),
                Update.Add(node = 2, attr = "children", value = 4, index = 0),
                Update.Add(node = 2, attr = "children", value = 5, index = 1),
                Update.DestroyNode(node = 0),
                Update.DestroyNode(node = 3)
        ), d.updates())
    }


    @Test
    fun `simple container test`() {
        val d = CapturingDriver()
        val c = ReconciliationContext(DOMPlatform, d)
        c.reconcile(SimpleContainer::class with SimpleContainerProps(x = 2))
        d.updates()
        c.reconcile(SimpleContainer::class with SimpleContainerProps(x = 2))
        assert(d.updates().isEmpty())
        c.reconcile(SimpleContainer::class with SimpleContainerProps(x = 1))
        assertEquals(listOf(
                Update.Remove(node = 0, attr = "children", value = 3),
                Update.DestroyNode(node = 3)), d.updates())
        c.reconcile(SimpleContainer::class with SimpleContainerProps(x = 3))
        assertEquals(listOf(
                Update.MakeNode(node = 4, type = "text-node", parameters = emptyMap()),
                Update.SetAttr(node = 4, attr = "text", value = "2"),
                Update.MakeNode(node = 5, type = "text-node", parameters = emptyMap()),
                Update.SetAttr(node = 5, attr = "text", value = "3"),
                Update.Add(node = 0, attr = "children", value = 4, index = 2),
                Update.Add(node = 0, attr = "children", value = 5, index = 3)
        ), d.updates())
        c.reconcile(SimpleContainer::class with SimpleContainerProps(x = 2))
        assertEquals(listOf(
                Update.Remove(node = 0, attr = "children", value = 5),
                Update.DestroyNode(node = 5)), d.updates())
    }

    @Test
    fun `recnciliation of sequences`() {
        val d = CapturingDriver()

        val c = ReconciliationContext(DOMPlatform, d)
        val e0 = "div" with TestProps1().apply {
            children.add("hey" with TestProps1().apply { key = "hey" })
            children.add("hoy" with TestProps1().apply { key = "hoy" })
        }
        c.reconcile(e0)
        assertEquals(listOf(
                Update.MakeNode(node = 0, type = "div", parameters = emptyMap()),
                Update.MakeNode(node = 1, type = "hey", parameters = emptyMap()),
                Update.MakeNode(node = 2, type = "hoy", parameters = emptyMap()),
                Update.Add(node = 0, attr = "children", value = 1, index = 0),
                Update.Add(node = 0, attr = "children", value = 2, index = 1)), d.updates())
        c.reconcile(e0)
        assert(d.updates().isEmpty())
        val e1 = "div" with TestProps1().apply {
            children.add("hiy" with TestProps1().apply { key = "hiy" })
            children.add("hoy" with TestProps1().apply { key = "hoy" })
            children.add("fu" with TestProps1().apply { key = "fu" })
        }
        c.reconcile(e1)
        assertEquals(listOf(
                Update.MakeNode(node = 3, type = "hiy", parameters = emptyMap()),
                Update.MakeNode(node = 4, type = "fu", parameters = emptyMap()),
                Update.Remove(node = 0, attr = "children", value = 1),
                Update.Add(node = 0, attr = "children", value = 3, index = 0),
                Update.Add(node = 0, attr = "children", value = 4, index = 2),
                Update.DestroyNode(node = 1)), d.updates())
        val e2 = "div" with TestProps1().apply {
            children.add("hoy" with TestProps1().apply { key = "hoy" })
            children.add("hiy" with TestProps1().apply { key = "hiy" })
            children.add("fu" with TestProps1().apply { key = "fu" })
        }
        c.reconcile(e2)
        assertEquals(listOf(
                Update.Remove(node = 0, attr = "children", value = 2),
                Update.Add(node = 0, attr = "children", value = 2, index = 0)), d.updates())

    }

    @Test
    fun `reuse with same type`() {
        val d = CapturingDriver()
        val c = ReconciliationContext(DOMPlatform, d)
        val e0 = "div" with TestProps1().apply {
            children.add("text-node" with TextNodeProps().apply {
                key = 1
                text = "1"
            })
            children.add("text-node" with TextNodeProps().apply {
                key = 2
                text = "2"
            })
        }
        c.reconcile(e0)
        d.updates()
        val e1 = "div" with TestProps1().apply {
            children.add("text-node" with TextNodeProps().apply {
                key = 3
                text = "3"
            })
            children.add("text-node" with TextNodeProps().apply {
                key = 1
                text = "1"
            })
        }
        c.reconcile(e1)
        assertEquals(listOf(
                Update.SetAttr(node = 2, attr = "text", value = "3"),
                Update.Remove(node = 0, attr = "children", value = 2),
                Update.Add(node = 0, attr = "children", value = 2, index = 0)), d.updates())
        val e2 = "div" with TestProps1().apply {
            children.add("text-node" with TextNodeProps().apply {
                key = 1
                text = "1"
            })
            children.add("text-node" with TextNodeProps().apply {
                key = 4
                text = "4"
            })
        }
        c.reconcile(e2)
        assertEquals(listOf(
                Update.SetAttr(node = 2, attr = "text", value = "4"),
                Update.Remove(node = 0, attr = "children", value = 1),
                Update.Add(node = 0, attr = "children", value = 1, index = 0)), d.updates())
    }

    @Test
    fun `Reconciliation keeps the view and adds update for new subview`() {
        val d = CapturingDriver()
        val c = ReconciliationContext(DOMPlatform, d)
        c.reconcile(MyMacComponent::class with MyProps())
        d.updates()
        c.reconcile(MyMacComponent::class with MyProps(x = 1))
        assertTrue(d.updates().single() is Update.Add, "There should be single update of adding subview")
    }
}
