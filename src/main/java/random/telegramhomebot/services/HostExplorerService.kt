package random.telegramhomebot.services

import random.telegramhomebot.model.Host

interface HostExplorerService {
    fun getCurrentHosts(): List<Host>
}