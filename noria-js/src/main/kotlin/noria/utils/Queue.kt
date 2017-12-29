package noria.utils

import kotlin.browser.*

private var callback: (()->Unit)? = null
actual fun scheduleOnce(f: () -> Unit) {
    if (callback == null) {

        callback = f
        window.setTimeout({ // TODO, don't use settimeout
            val ff = callback
            callback = null
            ff?.invoke()
        }, 0)
    }
}
