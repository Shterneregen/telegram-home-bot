package random.telegramhomebot.monitor.hosts

import random.telegramhomebot.db.model.Host

interface HostExplorerService {
    fun getCurrentHosts(): List<Host>
}
