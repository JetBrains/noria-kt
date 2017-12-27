package noria

import noria.views.*
import org.jetbrains.noria.*
import org.jetbrains.noria.components.*

object DOMPlatform : Platform() {
    init {
        register(Root, ::DOMRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)
        register(Label, ::Label)
        register(Button, ::Button)
    }
}
