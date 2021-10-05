package random.telegramhomebot.services.hosts

import org.springframework.stereotype.Service
import random.telegramhomebot.db.model.HostState
import random.telegramhomebot.db.model.HostTimeLog
import random.telegramhomebot.db.dto.TimeLogDto

@Service
class TimeLogConverter {
    fun convertToDto(timeLog: HostTimeLog) = TimeLogDto(getState(timeLog), timeLog.createdDate)

    private fun getState(timeLog: HostTimeLog) =
        if (timeLog.state !== HostState.FAILED) HostState.REACHABLE.toString() else timeLog.state.toString()
}