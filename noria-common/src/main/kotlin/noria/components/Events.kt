package noria.components

abstract class PlatformProps {
    val events = Events()
}

class Events {
    var onClick : (() -> Unit)? = null
}
