package noria.components

abstract class PlatformProps {
    val events = Events()
}

typealias EventHandler = () -> Unit

class Events {
    var onClick : EventHandler? = null
    var onMouseEntered: EventHandler? = null
    var onMouseExited: EventHandler? = null

    var onEnter: EventHandler? = null

    var onFocusLost: EventHandler? = null
    var onFocusGained: EventHandler? = null
}
