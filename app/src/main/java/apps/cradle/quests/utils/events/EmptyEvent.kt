package apps.cradle.quests.utils.events

class EmptyEvent {

    @Suppress("MemberVisibilityCanBePrivate")
    var hasBeenHandled = false
        private set

    fun getEventIfNotHandled(): EmptyEvent? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            this
        }
    }

    class Observer(
        private val onEventUnhandled: () -> Unit
    ) : androidx.lifecycle.Observer<EmptyEvent> {

        override fun onChanged(value: EmptyEvent) {
            value.getEventIfNotHandled()?.run {
                onEventUnhandled.invoke()
            }
        }

    }

}