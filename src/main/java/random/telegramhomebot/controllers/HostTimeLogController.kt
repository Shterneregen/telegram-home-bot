package random.telegramhomebot.controllers

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import random.telegramhomebot.const.AppConstants.DEFAULT_DATE_VALUE_NOW
import random.telegramhomebot.const.AppConstants.END_DATE_REQ_PARAM
import random.telegramhomebot.const.AppConstants.HOSTS_MAPPING
import random.telegramhomebot.const.AppConstants.START_DATE_REQ_PARAM
import random.telegramhomebot.const.AppConstants.TIME_LOG_MAPPING
import random.telegramhomebot.const.AppConstants.TIME_LOG_VIEW
import random.telegramhomebot.db.dto.TimeLogDto
import random.telegramhomebot.db.model.HostTimeLog
import random.telegramhomebot.db.repository.HostTimeLogRepository
import random.telegramhomebot.services.hosts.TimeLogConverter
import random.telegramhomebot.utils.logger
import java.time.LocalDate

@Controller
@RequestMapping(HOSTS_MAPPING)
class HostTimeLogController(
    private val hostTimeLogRepository: HostTimeLogRepository,
    private val timeLogConverter: TimeLogConverter
) {
    val log = logger()

    @RequestMapping(TIME_LOG_MAPPING)
    fun getTimeLogForPeriod(
        @RequestParam(value = START_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam(value = END_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        model: Model
    ): String {
//        val startOfDate = startDate.atTime(LocalTime.MIN)
//        val endOfDate = endDate.atTime(LocalTime.MAX)
//        val logs = hostTimeLogRepository.findByCreatedDateBetween(valueOf(startOfDate), valueOf(endOfDate))
//        model.addAttribute(TIME_LOG_MAP_MODEL_ATTR, getTimeLogDtoMap(logs))
//        model.addAttribute(START_DATE_MODEL_ATTR, startDate)
//        model.addAttribute(END_DATE_MODEL_ATTR, endDate)
        return TIME_LOG_VIEW
    }

    private fun getTimeLogDtoMap(logs: List<HostTimeLog>): Map<String, List<TimeLogDto>> =
        logs.groupBy({ it.host.deviceName ?: it.host.mac ?: "" }, { timeLogConverter.convertToDto(it) }).toSortedMap()
}
