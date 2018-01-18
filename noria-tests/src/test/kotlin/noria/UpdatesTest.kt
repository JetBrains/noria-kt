package noria

import noria.components.*
import noria.utils.*
import noria.views.*
import kotlin.test.*

data class Click(val buttonNum: Int, val clickCount: Int) : Event()

class NSViewProps : HostProps() {
    val subviews: MutableList<NElement<NSViewProps>> by elementList()
    var onClick by handler<Click>()
}

class NSConstraint : HostProps() {
    var view1: NElement<NSViewProps> by element()
    var view2: NElement<NSViewProps> by element()
}

val NSView = HostComponentType<NSViewProps>("NSView")
val NSLayoutConstraint = HostComponentType<NSConstraint>("NSLayoutConstraint")

data class MyProps(val x: Int = 0)
class MyMacComponent(props: MyProps) : View<MyProps>(props) {
    override fun RenderContext.render() {
        val v1 = reify(createHostElement(NSView, NSViewProps().apply {
            subviews.add(createHostElement(NSView, NSViewProps()))
        }))

        val v2 = reify(createHostElement(NSView, NSViewProps().apply {
            subviews.add(createHostElement(NSView, NSViewProps()))
            onClick = CallbackInfo(true) { event ->
                println("hello")
            }
        }))

        reify(createHostElement(NSLayoutConstraint, NSConstraint().apply {
            view1 = v1
            view2 = v2
        }))

        x(NSView) {
            subviews.add(v1)
            if (props.x > 0) {
                subviews.add(v2)
            }
        }
    }
}

class TestProps1 : HostProps() {
    val children: MutableList<NElement<*>> by elementList()
}


class LabelProps {
    var text: String = ""
}

class Label(p: LabelProps) : View<LabelProps>(p) {
    override fun RenderContext.render() {
        text(props.text)
    }
}

data class SimpleContainerProps(val x: Int)
class SimpleContainer(p: SimpleContainerProps) : View<SimpleContainerProps>(p) {
    override fun RenderContext.render() {
        div {
            for (c in (0..props.x)) {
                x(::Label, c.toString()) {
                    text = c.toString()
                }
            }
        }
    }
}

data class SplitProps(val left: NElement<*>,
                      val right: NElement<*>)

class SplitView(c: SplitProps) : View<SplitProps>(c) {
    override fun RenderContext.render() {
        div {
            emit(props.left)
            emit(props.right)
        }
    }
}

data class HOProps(val x: NElement<*>,
                   val y: NElement<*>)

class HO(p: HOProps) : View<HOProps>(p) {
    override fun RenderContext.render() {
        val x1 = capture { emit(props.x) }
        val y1 = capture { emit(props.y) }

        x(::SplitView, (SplitProps(left = x1, right = y1)))
    }
}

class CapturingDriver : Host {
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

data class WrapperProps(val w: NElement<*>?)
class Wrapper(p: WrapperProps) : View<WrapperProps>(p) {
    override fun RenderContext.render() {
        div {
            val w = props.w
            if (w != null) {
                emit(w)
            }
        }
    }
}

data class ReifiesProps(val i: Int)
class Reifies(p: ReifiesProps) : View<ReifiesProps>(p) {
    override fun RenderContext.render() {
        val e = capture { label("some text") }
        div {
            if (props.i == 0) {
                x(::Wrapper, WrapperProps(e))
            } else {
                x(::Wrapper, WrapperProps(null))
            }
            if (props.i == 1) {
                x(::Wrapper, WrapperProps(e))
            } else {
                x(::Wrapper, WrapperProps(null))
            }
        }
    }
}

class RenderCounter(p: Any) : View<Any>(0) {
    var counter = 0
    override fun RenderContext.render() {
        x((if (counter % 2 == 0) Div else Span)) {
            click = CallbackInfo(true) {
                counter++
                forceUpdate()
            }
        }
    }
}

class SwitchingComponent(i: Int) : View<Int>(i) {
    override fun RenderContext.render() {
        if (props == 0) {
            label("x")
        } else {
            button(title = "x", action = {})
        }
    }
}

class UpdatesTest {
    @Test
    fun `component changes its subst`() {
        checkUpdates(listOf(
                div {
                    x(::SwitchingComponent, 0)
                } to null,
                div {
                    x(::SwitchingComponent, 1)
                } to listOf(
                        Update.MakeNode(node = 3, type = "input", parameters = fastStringMap()),
                        Update.SetAttr(node = 3, attr = "type", value = "button"),
                        Update.SetAttr(node = 3, attr = "value", value = "x"),
                        Update.SetCallback(node = 3, attr = "click", async = true),
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.Add(node = 0, attr = "children", value = 3, index = 0),
                        Update.DestroyNode(node = 1))))
    }

    @Test
    fun `test force update`() {
        val d = CapturingDriver()
        val c = GraphState(DOMPlatform, d)
        c.mount("root") {
            div {
                x(::RenderCounter) {}
            }
        }

        val updates0 = d.updates()
        c.handleEvent(EventInfo(2, "click", DomEvent()))
        c.drainUpdateQueue()
        val updates1 = d.updates()
        assertEquals(listOf(
                Update.MakeNode(node = 3, type = "span", parameters = fastStringMap()),
                Update.SetCallback(node = 3, attr = "click", async = true),
                Update.Remove(node = 1, attr = "children", value = 2),
                Update.Add(node = 1, attr = "children", value = 3, index = 0),
                Update.DestroyNode(node = 2)), updates1)
    }

    fun checkUpdates(testData: List<Pair<NElement<*>, List<Update>?>>): List<Update>? {
        val d = CapturingDriver()
        val c = GraphState(DOMPlatform, d)
        var i: Instance? = null
        var lastUpdates: List<Update>? = null
        for ((element, updates) in testData) {
            val (i1, actualUpdates) = ReconciliationState(c).reconcile(i, element)
            if (updates != null) {
                assertEquals(updates, actualUpdates)
            }
            lastUpdates = actualUpdates
            i = i1
        }
        return lastUpdates
    }

    inline fun div(build: DomProps.() -> Unit): NElement<*> {
        return createHostElement(Div, DomProps().apply(build))
    }


    @Test
    fun `test reify`() {
        checkUpdates(listOf(
                createViewElement(::Reifies, ReifiesProps(0)) to null,
                createViewElement(::Reifies, ReifiesProps(1)) to listOf(
                        Update.Remove(node = 3, attr = "children", value = 0),
                        Update.Add(node = 4, attr = "children", value = 0, index = 0)
                )))
    }

    @Test
    fun `recursive destroy`() {
        checkUpdates(listOf(
                div {
                    x(Span) {
                        x(Pre) {}
                    }
                } to null,
                createHostElement(Div, DomProps()) to listOf(
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.DestroyNode(node = 1),
                        Update.DestroyNode(node = 2))
        ))
    }

    @Test
    fun `testing high-order components`() {
        val Foo = HostComponentType<TestProps1>("foo")
        val Bar = HostComponentType<TestProps1>("bar")
        val Baz = HostComponentType<TestProps1>("baz")
        val Fizz = HostComponentType<TestProps1>("fizz")
        val Fuzz = HostComponentType<TestProps1>("fuzz")

        checkUpdates(listOf(
                createViewElement(::HO, HOProps(
                        x = createHostElement(Foo, TestProps1()),
                        y = createHostElement(Bar, TestProps1()))) to listOf(
                        Update.MakeNode(node = 0, type = "foo", parameters = fastStringMap()),
                        Update.MakeNode(node = 1, type = "bar", parameters = fastStringMap()),
                        Update.MakeNode(node = 2, type = "div", parameters = fastStringMap()),
                        Update.Add(node = 2, attr = "children", value = 0, index = 0),
                        Update.Add(node = 2, attr = "children", value = 1, index = 1)
                ),
                createViewElement(::HO, HOProps(
                        x = createHostElement(Foo, TestProps1()),
                        y = createHostElement(Baz, TestProps1()))) to listOf(
                        Update.MakeNode(node = 3, type = "baz", parameters = fastStringMap()),
                        Update.Remove(node = 2, attr = "children", value = 1),
                        Update.Add(node = 2, attr = "children", value = 3, index = 1),
                        Update.DestroyNode(node = 1)
                ),
                createViewElement(::HO, HOProps(
                        x = createHostElement(Fizz, TestProps1()),
                        y = createHostElement(Fuzz, TestProps1()))) to
                        listOf(
                                Update.MakeNode(node = 4, type = "fizz", parameters = fastStringMap()),
                                Update.MakeNode(node = 5, type = "fuzz", parameters = fastStringMap()),
                                Update.Remove(node = 2, attr = "children", value = 0),
                                Update.Remove(node = 2, attr = "children", value = 3),
                                Update.Add(node = 2, attr = "children", value = 4, index = 0),
                                Update.Add(node = 2, attr = "children", value = 5, index = 1),
                                Update.DestroyNode(node = 0),
                                Update.DestroyNode(node = 3)
                        )))
    }


    @Test
    fun `simple container test`() {
        checkUpdates(listOf(
                createViewElement(::SimpleContainer, SimpleContainerProps(x = 2)) to null,
                createViewElement(::SimpleContainer, SimpleContainerProps(x = 2)) to emptyList<Update>(),
                createViewElement(::SimpleContainer, SimpleContainerProps(x = 1)) to listOf(
                        Update.Remove(node = 0, attr = "children", value = 3),
                        Update.DestroyNode(node = 3)),
                createViewElement(::SimpleContainer, SimpleContainerProps(x = 3)) to listOf(
                        Update.MakeNode(node = 4, type = "textnode", parameters = fastStringMap()),
                        Update.SetAttr(node = 4, attr = "text", value = "2"),
                        Update.MakeNode(node = 5, type = "textnode", parameters = fastStringMap()),
                        Update.SetAttr(node = 5, attr = "text", value = "3"),
                        Update.Add(node = 0, attr = "children", value = 4, index = 2),
                        Update.Add(node = 0, attr = "children", value = 5, index = 3)
                ),
                createViewElement(::SimpleContainer, SimpleContainerProps(x = 2)) to listOf(
                        Update.Remove(node = 0, attr = "children", value = 5),
                        Update.DestroyNode(node = 5))))
    }

    @Test
    fun `recnciliation of sequences`() {
        val d = CapturingDriver()
        val hey = HostComponentType<TestProps1>("hey")
        val hoy = HostComponentType<TestProps1>("hoy")
        val hiy = HostComponentType<TestProps1>("hiy")
        val fu = HostComponentType<TestProps1>("fu")

        val e0 = div {
            x(hey, "hey") {}
            x(hoy, "hoy") {}
        }
        checkUpdates(listOf(
                e0 to listOf(
                        Update.MakeNode(node = 0, type = "div", parameters = fastStringMap()),
                        Update.MakeNode(node = 1, type = "hey", parameters = fastStringMap()),
                        Update.MakeNode(node = 2, type = "hoy", parameters = fastStringMap()),
                        Update.Add(node = 0, attr = "children", value = 1, index = 0),
                        Update.Add(node = 0, attr = "children", value = 2, index = 1)),
                e0 to emptyList(),
                div {
                    x(hiy, "hiy") {}
                    x(hoy, "hoy") {}
                    x(fu, "fu") {}
                } to listOf(
                        Update.MakeNode(node = 3, type = "hiy", parameters = fastStringMap()),
                        Update.MakeNode(node = 4, type = "fu", parameters = fastStringMap()),
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.Add(node = 0, attr = "children", value = 3, index = 0),
                        Update.Add(node = 0, attr = "children", value = 4, index = 2),
                        Update.DestroyNode(node = 1)),
                div {
                    x(hoy, "hoy") {}
                    x(hiy, "hiy") {}
                    x(fu, "fu") {}
                } to listOf(
                        Update.Remove(node = 0, attr = "children", value = 2),
                        Update.Add(node = 0, attr = "children", value = 2, index = 0))))
    }

    @Test
    fun `reuse with same type`() {
        checkUpdates(listOf(
                div {
                    text("1", "1")
                    text("2", "2")
                } to null,
                div {
                    text("3", "3")
                    text("1", "1")
                } to listOf(
                        Update.SetAttr(node = 2, attr = "text", value = "3"),
                        Update.Remove(node = 0, attr = "children", value = 2),
                        Update.Add(node = 0, attr = "children", value = 2, index = 0)),
                div {
                    text("1", "1")
                    text("4", "4")
                } to listOf(
                        Update.SetAttr(node = 2, attr = "text", value = "4"),
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.Add(node = 0, attr = "children", value = 1, index = 0))
        ))
    }

    @Test
    fun `Reconciliation keeps the view and adds update for new subview`() {
        checkUpdates(listOf(
                createViewElement(::MyMacComponent, MyProps()) to null,
                createViewElement(::MyMacComponent, MyProps(x = 1)) to listOf(Update.Add(node = 5, attr = "subviews", value = 2, index = 1))))
    }
}
