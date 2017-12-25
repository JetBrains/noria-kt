package noria

import noria.views.*
import org.jetbrains.noria.*

object DOMPlatform : Platform() {
    init {
        register(rootCT, ::DOMRoot)
        register(hboxCT, ::FlexBox)
        register(vboxCT, ::FlexBox)
        register(labelCT, ::Label)
        register(buttonCT, ::Button)
    }
}
