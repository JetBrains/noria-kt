package noria

import noria.views.Button
import noria.views.FlexBox
import noria.views.Label
import org.jetbrains.noria.*

object DOMPlatform : Platform() {
    init {
        register(hboxCT, ::FlexBox)
        register(vboxCT, ::FlexBox)
        register(labelCT, ::Label)
        register(buttonCT, ::Button)
    }
}
