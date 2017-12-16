package noria

import org.jetbrains.noria.MyMacComponent
import org.jetbrains.noria.MyProps
import org.jetbrains.noria.Update
import org.jetbrains.noria.UserInstance
import org.jetbrains.noria.createInstance
import org.jetbrains.noria.reconcile
import org.jetbrains.noria.with
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class UpdatesTest {

    @Test fun `Reconciliation keeps the view and adds update for new subview`() {
        val (component, _) = createInstance(MyMacComponent::class with MyProps())
        val view = (component as UserInstance).view as? MyMacComponent ?: fail("View expected to be MyMacComponent")

        val (newComponent, updates) = reconcile(component, MyMacComponent::class with MyProps(x = 1))
        assertTrue((newComponent as UserInstance).view === view, "View is expected to be kept the same. It's just props shall be changed")
        assertEquals(1, view.props.x, "Props expected to be updated")
        assertTrue(updates.single() is Update.Add, "There should be single update of adding subview")
    }
}
