package noria

import org.jetbrains.noria.MyMacComponent
import org.jetbrains.noria.MyProps
import org.jetbrains.noria.Update
import org.jetbrains.noria.UserInstance
import org.jetbrains.noria.createInstance
import org.jetbrains.noria.reconcile
import org.jetbrains.noria.with
import kotlin.test.Test
import kotlin.test.assertTrue

class UpdatesTest {

    @Test fun `Reconciliation keeps the view and adds update for new subview`() {
        val (component, _) = createInstance(MyMacComponent::class with MyProps())

        val view = (component as UserInstance).view
        assertTrue(view is MyMacComponent)

        val (newComponent, updates) = reconcile(component, MyMacComponent::class with MyProps(x = 1))
        assertTrue((newComponent as UserInstance).view === view)

        val singleUpdate = updates.single()
        assertTrue(singleUpdate is Update.Add)
    }
}
