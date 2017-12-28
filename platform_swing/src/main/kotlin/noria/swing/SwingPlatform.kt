package noria.swing

import noria.*
import noria.components.*
import noria.swing.components.*

object SwingPlatform : Platform() {
    init {
        register(Root, ::SwingRoot)
        register(HBox, ::FlexBox)
        register(VBox, ::FlexBox)
        register(Label, ::Label)
        register(Button, ::NJButton)
    }
}
