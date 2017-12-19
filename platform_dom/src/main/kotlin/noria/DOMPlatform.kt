package noria

import noria.views.FlexBox
import org.jetbrains.noria.Platform

object DOMPlatform : Platform {
    override fun hbox() = FlexBox::class
    override fun vbox() = FlexBox::class

}
