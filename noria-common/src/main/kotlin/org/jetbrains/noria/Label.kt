package org.jetbrains.noria

data class LabelProps(val text: String) : Props()

fun RenderContext.label(text: String) = labelCT with LabelProps(text)
