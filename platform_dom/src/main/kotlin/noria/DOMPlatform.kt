package noria

import noria.views.*
import noria.*
import noria.components.*

object DOMPlatform : Platform() {
    init {
        register(Root, ::DOMRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)
        register(Label, ::Label)
        register(Button, ::Button)
        register(TextField, ::TextField)
    }
}
