package noria.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.*

private val callback : AtomicReference<(() -> Unit)?> = AtomicReference(null)
private val pool = Executors.newSingleThreadExecutor() {
    Executors.defaultThreadFactory().newThread(it).apply {
        name = "Noria-Reconciler"
        isDaemon = true
    }
}

actual fun scheduleOnce(f: () -> Unit) {
    if (callback.compareAndSet(null, f)) {
        pool.submit {
            val c = callback.getAndSet(null)
            c?.invoke()
        }
    }
}
