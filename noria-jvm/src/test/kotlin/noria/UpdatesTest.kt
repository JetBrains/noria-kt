package noria

import org.jetbrains.noria.MyMacComponent
import org.jetbrains.noria.MyProps
import org.jetbrains.noria.NElement
import org.jetbrains.noria.ReconciliationContext
import org.jetbrains.noria.Update
import org.jetbrains.noria.UserInstance
import org.jetbrains.noria.with
import kotlin.test.Test
import kotlin.test.assertTrue

class UpdatesTest {

    @Test fun `Reconciliation keeps the view and adds update for new subview`() {
        val ctx = ReconciliationContext()
        val component = ctx.reconcile(null, MyMacComponent::class with MyProps())
        assertTrue((component?.element as? NElement.Class<*>)?.kClass == MyMacComponent::class)

        val view = (component as UserInstance).view

        val updateCtx = ReconciliationContext()
        val newComponent = updateCtx.reconcile(component, MyMacComponent::class with MyProps(x = 1))
        assertTrue((newComponent as UserInstance).view === view)

        val singleUpdate = updateCtx.updates().single()
        assertTrue(singleUpdate is Update.Add)
    }
}
