package random.telegramhomebot.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.model.TimeLogDto;
import random.telegramhomebot.repository.HostTimeLogRepository;
import random.telegramhomebot.services.TimeLogConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.sql.Timestamp.valueOf;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.DEFAULT_DATE_VALUE_NOW;
import static random.telegramhomebot.AppConstants.HostsTimeLog.END_DATE_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.HostsTimeLog.END_DATE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsTimeLog.START_DATE_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.HostsTimeLog.START_DATE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAP_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_VIEW;

@RequiredArgsConstructor
@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostTimeLogController {

	private final HostTimeLogRepository hostTimeLogRepository;
	private final TimeLogConverter timeLogConverter;

	@RequestMapping(TIME_LOG_MAPPING)
	public String getTimeLogForPeriod(
			@RequestParam(value = START_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = END_DATE_REQ_PARAM, defaultValue = DEFAULT_DATE_VALUE_NOW)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			Model model) {
		LocalDateTime startOfDate = startDate.atTime(LocalTime.MIN);
		LocalDateTime endOfDate = endDate.atTime(LocalTime.MAX);

		List<HostTimeLog> logs = hostTimeLogRepository.findByCreatedDateBetween(valueOf(startOfDate), valueOf(endOfDate));

		model.addAttribute(TIME_LOG_MAP_MODEL_ATTR, getTimeLogDtoMap(logs));
		model.addAttribute(START_DATE_MODEL_ATTR, startDate);
		model.addAttribute(END_DATE_MODEL_ATTR, endDate);
		return TIME_LOG_VIEW;
	}

	private Map<String, List<TimeLogDto>> getTimeLogDtoMap(List<HostTimeLog> logs) {
		return logs.stream().collect(groupingBy(getDeviceName(), mapping(timeLogConverter::convertToDto, toList())));
	}

	private Function<HostTimeLog, String> getDeviceName() {
		return log -> log.getHost().getDeviceName() != null ? log.getHost().getDeviceName() : log.getHost().getMac();
	}
}
