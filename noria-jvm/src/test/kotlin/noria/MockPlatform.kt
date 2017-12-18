package noria

import org.jetbrains.noria.BoxProps
import org.jetbrains.noria.Container
import org.jetbrains.noria.NElement
import org.jetbrains.noria.Platform
import org.jetbrains.noria.RenderContext

class MockVBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = TODO()
}

class MockHBox : Container<BoxProps>() {
    override fun RenderContext.render(): NElement<*> = TODO()
}


object MockPlatform : Platform {
    override fun hbox() = MockHBox::class
    override fun vbox() = MockVBox::class
}
