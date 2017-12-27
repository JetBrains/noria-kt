package noria

import noria.views.Div
import noria.views.DomEvent
import noria.views.DomProps
import noria.views.Pre
import noria.views.Span
import noria.views.TextNodeProps
import noria.views.div
import noria.views.textNodeCT
import org.jetbrains.noria.CallbackInfo
import org.jetbrains.noria.Event
import org.jetbrains.noria.EventInfo
import org.jetbrains.noria.GraphState
import org.jetbrains.noria.HostComponentType
import org.jetbrains.noria.HostProps
import org.jetbrains.noria.Instance
import org.jetbrains.noria.JustifyContent
import org.jetbrains.noria.NElement
import org.jetbrains.noria.PlatformDriver
import org.jetbrains.noria.Props
import org.jetbrains.noria.ReconciliationState
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.Update
import org.jetbrains.noria.View
import org.jetbrains.noria.button
import org.jetbrains.noria.capture
import org.jetbrains.noria.createElement
import org.jetbrains.noria.fastStringMap
import org.jetbrains.noria.hbox
import org.jetbrains.noria.label
import org.jetbrains.noria.vbox
import kotlin.test.Test
import kotlin.test.assertEquals

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

data class MyProps(val x: Int = 0) : Props()
class MyMacComponent : View<MyProps>() {
    override fun RenderContext.render() {
        val v1 = NSView.createElement(NSViewProps().apply {
            subviews.add(NSView.createElement(NSViewProps()))
        })

        val v2 = NSView.createElement(NSViewProps().apply {
            subviews.add(NSView.createElement(NSViewProps()))
            onClick = CallbackInfo(true) { event ->
                println("hello")
            }
        })

        capture {NSLayoutConstraint with NSConstraint().apply {
            view1 = v1
            view2 = v2
        }}

        NSView with NSViewProps().apply {
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


data class LabelProps(val text: String) : Props()
class Label : View<LabelProps>() {
    override fun RenderContext.render() {
        textNodeCT with noria.views.TextNodeProps().apply { text = props.text }
    }
}

data class SimpleContainerProps(val x: Int) : Props()
class SimpleContainer : View<SimpleContainerProps>() {
    override fun RenderContext.render() {
        div {
            for (c in (0..props.x)) {
                ::Label with LabelProps("$c").apply {
                    key = c.toString()
                }
            }
        }
    }
}

data class SplitProps(val left: NElement<*>,
                      val right: NElement<*>) : Props()

class SplitView : View<SplitProps>() {
    override fun RenderContext.render() {
        div {
            emit(props.left)
            emit(props.right)
        }
    }
}

data class HOProps(val x: NElement<*>,
                   val y: NElement<*>) : Props()

class HO : View<HOProps>() {
    override fun RenderContext.render() {
        val x1 = capture {  emit(props.x) }
        val y1 = capture { emit(props.y) }

        ::SplitView with (SplitProps(left = x1, right = y1))
    }
}

data class AppProps(val counter: Int, val h: (Int) -> Unit) : Props()
class AppComponent : View<AppProps>() {
    override fun RenderContext.render() {
        vbox {
            hbox {
                justifyContent = JustifyContent.center

                button("Click Me Once") {
                    props.h(props.counter + 1)
                }

                button("Click Me Twice") {
                    props.h(props.counter + 1)
                }
            }

            repeat(props.counter) { n ->
                hbox {
                    label("$n")
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

data class WrapperProps(val w: NElement<*>?) : Props()
class Wrapper : View<WrapperProps>() {
    override fun RenderContext.render() {
        div {
            val w = props.w
            if (w != null) {
                emit(w)
            }
        }
    }
}

data class ReifiesProps(val i: Int) : Props()
class Reifies : View<ReifiesProps>() {
    override fun RenderContext.render() {
        val e = capture {label("some text")}
        div {
            if (props.i == 0) {
                ::Wrapper with WrapperProps(e)
            } else {
                ::Wrapper with WrapperProps(null)
            }
            if (props.i == 1) {
                ::Wrapper with WrapperProps(e)
            } else {
                ::Wrapper with WrapperProps(null)
            }
        }
    }
}

class RenderCounter : View<Props>() {
    var counter = 0
    override fun RenderContext.render() {
        (if (counter % 2 == 0) Div else Span) with DomProps().apply {
            click = CallbackInfo(true) {
                counter++
                forceUpdate()
            }
        }
    }

}

class UpdatesTest {

    @Test
    fun `test force update`() {
        val d = CapturingDriver()
        val c = GraphState(DOMPlatform, d)
        c.mount("root") {
            div {
                ::RenderCounter with Props()
            }
        }

        val updates0 = d.updates()
        c.handleEvent(EventInfo(2, "click", DomEvent()))
        val updates1 = d.updates()
        assertEquals(listOf(
                Update.DestroyNode(node = 2),
                Update.MakeNode(node = 3, type = "span", parameters = fastStringMap()),
                Update.SetCallback(node = 3, attr = "click", async = true),
                Update.Remove(node = 1, attr = "children", value = 2),
                Update.Add(node = 1, attr = "children", value = 3, index = 0)), updates1)
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
        return Div.createElement(DomProps().apply(build))
    }


    @Test
    fun `test reify`() {
        checkUpdates(listOf(
                ::Reifies.createElement(ReifiesProps(0)) to null,
                ::Reifies.createElement(ReifiesProps(1)) to listOf(
                        Update.Remove(node = 2, attr = "children", value = 0),
                        Update.Add(node = 3, attr = "children", value = 0, index = 0)
                )))
    }

    @Test
    fun `recursive destroy`() {
        checkUpdates(listOf(
                div {
                    Span with DomProps().apply {
                        Pre with DomProps()
                    }
                } to null,
                Div.createElement(DomProps()) to listOf(
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
                ::HO.createElement(HOProps(
                        x = Foo.createElement(TestProps1()),
                        y = Bar.createElement(TestProps1()))) to listOf(
                        Update.MakeNode(node = 0, type = "foo", parameters = fastStringMap()),
                        Update.MakeNode(node = 1, type = "bar", parameters = fastStringMap()),
                        Update.MakeNode(node = 2, type = "div", parameters = fastStringMap()),
                        Update.Add(node = 2, attr = "children", value = 0, index = 0),
                        Update.Add(node = 2, attr = "children", value = 1, index = 1)
                ),
                ::HO.createElement(HOProps(
                        x = Foo.createElement(TestProps1()),
                        y = Baz.createElement(TestProps1()))) to listOf(
                        Update.MakeNode(node = 3, type = "baz", parameters = fastStringMap()),
                        Update.Remove(node = 2, attr = "children", value = 1),
                        Update.Add(node = 2, attr = "children", value = 3, index = 1),
                        Update.DestroyNode(node = 1)
                ),
                ::HO.createElement(HOProps(
                        x = Fizz.createElement(TestProps1()),
                        y = Fuzz.createElement(TestProps1()))) to
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
                ::SimpleContainer.createElement(SimpleContainerProps(x = 2)) to null,
                ::SimpleContainer.createElement(SimpleContainerProps(x = 2)) to emptyList<Update>(),
                ::SimpleContainer.createElement(SimpleContainerProps(x = 1)) to listOf(
                        Update.Remove(node = 0, attr = "children", value = 3),
                        Update.DestroyNode(node = 3)),
                ::SimpleContainer.createElement(SimpleContainerProps(x = 3)) to listOf(
                        Update.MakeNode(node = 4, type = "textnode", parameters = fastStringMap()),
                        Update.SetAttr(node = 4, attr = "text", value = "2"),
                        Update.MakeNode(node = 5, type = "textnode", parameters = fastStringMap()),
                        Update.SetAttr(node = 5, attr = "text", value = "3"),
                        Update.Add(node = 0, attr = "children", value = 4, index = 2),
                        Update.Add(node = 0, attr = "children", value = 5, index = 3)
                ),
                ::SimpleContainer.createElement(SimpleContainerProps(x = 2)) to listOf(
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
            hey with TestProps1().apply { key = "hey" }
            hoy with TestProps1().apply { key = "hoy" }
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
                    hiy with TestProps1().apply { key = "hiy" }
                    hoy with TestProps1().apply { key = "hoy" }
                    fu with TestProps1().apply { key = "fu" }
                } to listOf(
                        Update.MakeNode(node = 3, type = "hiy", parameters = fastStringMap()),
                        Update.MakeNode(node = 4, type = "fu", parameters = fastStringMap()),
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.Add(node = 0, attr = "children", value = 3, index = 0),
                        Update.Add(node = 0, attr = "children", value = 4, index = 2),
                        Update.DestroyNode(node = 1)),
                div {
                    hoy with TestProps1().apply { key = "hoy" }
                    hiy with TestProps1().apply { key = "hiy" }
                    fu with TestProps1().apply { key = "fu" }
                } to listOf(
                        Update.Remove(node = 0, attr = "children", value = 2),
                        Update.Add(node = 0, attr = "children", value = 2, index = 0))))
    }

    @Test
    fun `reuse with same type`() {
        checkUpdates(listOf(
                div {
                    textNodeCT with TextNodeProps().apply {
                        key = "1"
                        text = "1"
                    }
                    textNodeCT with TextNodeProps().apply {
                        key = "2"
                        text = "2"
                    }
                } to null,
                div {
                    textNodeCT with TextNodeProps().apply {
                        key = "3"
                        text = "3"
                    }
                    textNodeCT with TextNodeProps().apply {
                        key = "1"
                        text = "1"
                    }
                } to listOf(
                        Update.SetAttr(node = 2, attr = "text", value = "3"),
                        Update.Remove(node = 0, attr = "children", value = 2),
                        Update.Add(node = 0, attr = "children", value = 2, index = 0)),
                div {
                    textNodeCT with TextNodeProps().apply {
                        key = "1"
                        text = "1"
                    }
                    textNodeCT with TextNodeProps().apply {
                        key = "4"
                        text = "4"
                    }
                } to listOf(
                        Update.SetAttr(node = 2, attr = "text", value = "4"),
                        Update.Remove(node = 0, attr = "children", value = 1),
                        Update.Add(node = 0, attr = "children", value = 1, index = 0))
        ))
    }

    @Test
    fun `Reconciliation keeps the view and adds update for new subview`() {
        checkUpdates(listOf(
                ::MyMacComponent.createElement(MyProps()) to null,
                ::MyMacComponent.createElement(MyProps(x = 1)) to listOf(Update.Add(node = 5, attr = "subviews", value = 2, index = 1))))
    }
}
