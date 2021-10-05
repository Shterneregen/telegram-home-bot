package random.telegramhomebot.db.model

import random.telegramhomebot.telegram.Icon

enum class HostState {
    PERMANENT, NOARP, STALE, REACHABLE, NONE, INCOMPLETE, DELAY, PROBE, FAILED;

    fun getIcon() = getIcon(this)

    private fun getIcon(state: HostState) = when (state) {
        REACHABLE -> Icon.GREEN_CIRCLE.get()
        STALE -> Icon.YELLOW_CIRCLE.get()
        FAILED -> Icon.RED_CIRCLE.get()
        else -> Icon.WHITE_CIRCLE.get()
    }
}
