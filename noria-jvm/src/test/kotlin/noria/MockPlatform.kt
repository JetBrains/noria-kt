package noria

import org.jetbrains.noria.BoxProps
import org.jetbrains.noria.ButtonProps
import org.jetbrains.noria.Container
import org.jetbrains.noria.NElement
import org.jetbrains.noria.Platform
import org.jetbrains.noria.RenderContext
import org.jetbrains.noria.View
import kotlin.reflect.KClass

class MockVBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = TODO()
}

class MockHBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = TODO()
}


object MockPlatform : Platform {
    override fun hbox() = MockHBox::class
    override fun vbox() = MockVBox::class
    override fun label() = TODO()
    override fun button() = TODO()
}
