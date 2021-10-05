package random.telegramhomebot.services.hosts

import random.telegramhomebot.db.model.Host

interface HostExplorerService {
    fun getCurrentHosts(): List<Host>
}