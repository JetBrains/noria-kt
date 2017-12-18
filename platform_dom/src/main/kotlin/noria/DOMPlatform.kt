package noria

import org.jetbrains.noria.BoxProps
import org.jetbrains.noria.Container
import org.jetbrains.noria.Platform
import kotlin.reflect.KClass

object DOMPlatform : Platform {
    override fun hbox(): KClass<out Container<BoxProps>> {
        TODO("not implemented")
    }

    override fun vbox(): KClass<out Container<BoxProps>> {
        TODO("not implemented")
    }
}
