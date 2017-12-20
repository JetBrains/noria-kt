package noria

import noria.views.Button
import noria.views.FlexBox
import noria.views.Label
import org.jetbrains.noria.Platform

object DOMPlatform : Platform {
    override fun hbox() = FlexBox::class
    override fun vbox() = FlexBox::class
    override fun label() = Label::class
    override fun button() = Button::class
}
