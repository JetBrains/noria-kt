package org.jetbrains.noria

fun main(args: Array<String>) {
    val ctx = ReconciliationContext()
    ctx.reconcile(null, MyMacComponent::class with Props())
}