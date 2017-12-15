package org.jetbrains.noria

fun main(args: Array<String>) {
    val ctx = ReconciliationContext()
    val component = ctx.reconcile(null, MyMacComponent::class with MyProps())
    val newComponent = ctx.reconcile(component, MyMacComponent::class with MyProps(x = 1))
    println()
}