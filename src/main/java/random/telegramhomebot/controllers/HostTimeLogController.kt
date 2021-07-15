package random.telegramhomebot.controllers

import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import random.telegramhomebot.AppConstants.Hosts
import random.telegramhomebot.AppConstants.HostsTimeLog.*
import random.telegramhomebot.model.HostTimeLog
import random.telegramhomebot.model.TimeLogDto
import random.telegramhomebot.repository.HostTimeLogRepository
import random.telegramhomebot.services.TimeLogConverter
import java.sql.Timestamp.valueOf
import java.time.LocalDate
import java.time.LocalTime
import java.util.function.Function
import java.util.stream.Collectors

@Controller
@RequestMapping(Hosts.HOSTS_MAPPING)
class HostTimeLogController(
    private val hostTimeLogRepository: HostTimeLogRepository,
    private val timeLogConverter: TimeLogConverter
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @RequestMapping(TIME_LOG_MAPPING)
    fun getTimeLogForPeriod(
        @RequestParam(value = START_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam(value = END_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        model: Model
    ): String {
        val startOfDate = startDate.atTime(LocalTime.MIN)
        val endOfDate = endDate.atTime(LocalTime.MAX)
        val logs = hostTimeLogRepository.findByCreatedDateBetween(valueOf(startOfDate), valueOf(endOfDate))
        model.addAttribute(TIME_LOG_MAP_MODEL_ATTR, getTimeLogDtoMap(logs))
        model.addAttribute(START_DATE_MODEL_ATTR, startDate)
        model.addAttribute(END_DATE_MODEL_ATTR, endDate)
        return TIME_LOG_VIEW
    }

    private fun getTimeLogDtoMap(logs: List<HostTimeLog>): Map<String, List<TimeLogDto>> {
        return logs.stream().collect(
            Collectors.groupingBy(
                getDeviceName(),
                Collectors.mapping({ timeLog -> timeLogConverter.convertToDto(timeLog) }, Collectors.toList())
            )
        )
    }

    private fun getDeviceName(): Function<HostTimeLog, String?> =
        Function { log: HostTimeLog -> if (log.host.deviceName != null) log.host.deviceName else log.host.mac }

}
