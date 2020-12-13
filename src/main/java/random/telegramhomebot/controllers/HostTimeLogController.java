package random.telegramhomebot.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.model.TimeLogDto;
import random.telegramhomebot.repository.HostTimeLogRepository;
import random.telegramhomebot.services.TimeLogConverter;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.DATE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAP_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_VIEW;

@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostTimeLogController {

	@Resource
	private HostTimeLogRepository hostTimeLogRepository;
	@Resource
	private TimeLogConverter convert;

	@RequestMapping(TIME_LOG_MAPPING)
	public String getTimeLog(
			@RequestParam(value = DATE_REQ_PARAM, defaultValue = "#{T(java.time.LocalDateTime).now()}")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Model model) {
		LocalDateTime startOfDate = date.atTime(LocalTime.MIN);
		LocalDateTime endOfDate = date.atTime(LocalTime.MAX);

		List<HostTimeLog> logs = hostTimeLogRepository
				.findByCreatedDateBetween(Timestamp.valueOf(startOfDate), Timestamp.valueOf(endOfDate));

		Map<String, List<TimeLogDto>> timeLogMap = logs.stream()
				.peek(log -> {
					if (!(log.getState() == HostState.FAILED)) {
						log.setState(HostState.REACHABLE);
					}
				})
				.collect(groupingBy(log -> log.getHost().getDeviceName() != null
								? log.getHost().getDeviceName() : log.getId().toString(),
						Collectors.mapping(log -> convert.convert(log), Collectors.toList())));

		model.addAttribute(TIME_LOG_MAP_MODEL_ATTR, timeLogMap);
		return TIME_LOG_VIEW;
	}

}
