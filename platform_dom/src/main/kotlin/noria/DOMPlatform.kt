package noria

import noria.views.Button
import noria.views.FlexBox
import noria.views.Label
import org.jetbrains.noria.Platform

object DOMPlatform : Platform {
    override fun hbox() = ::FlexBox
    override fun vbox() = ::FlexBox
    override fun label() = ::Label
    override fun button() = ::Button
}
