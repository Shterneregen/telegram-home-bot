package random.telegramhomebot.services

import org.springframework.stereotype.Service
import random.telegramhomebot.model.HostState
import random.telegramhomebot.model.HostTimeLog
import random.telegramhomebot.model.TimeLogDto

@Service
class TimeLogConverter {
    fun convertToDto(timeLog: HostTimeLog) = TimeLogDto(getState(timeLog), timeLog.createdDate)

    private fun getState(timeLog: HostTimeLog) =
        if (timeLog.state !== HostState.FAILED) HostState.REACHABLE.toString() else timeLog.state.toString()
}