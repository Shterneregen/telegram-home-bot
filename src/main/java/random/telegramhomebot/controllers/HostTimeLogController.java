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
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static random.telegramhomebot.AppConstants.Hosts.HOSTS_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.END_DATE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsTimeLog.START_DATE_REQ_PARAM;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAPPING;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_MAP_MODEL_ATTR;
import static random.telegramhomebot.AppConstants.HostsTimeLog.TIME_LOG_VIEW;

@Controller
@RequestMapping(HOSTS_MAPPING)
public class HostTimeLogController {

	@Resource
	private HostTimeLogRepository hostTimeLogRepository;
	@Resource
	private TimeLogConverter converter;

	@RequestMapping(TIME_LOG_MAPPING)
	public String getTimeLogForPeriod(
			@RequestParam(value = START_DATE_REQ_PARAM, defaultValue = "#{T(java.time.LocalDateTime).now()}")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = END_DATE_REQ_PARAM, defaultValue = "#{T(java.time.LocalDateTime).now()}")
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			Model model) {
		LocalDateTime startOfDate = startDate.atTime(LocalTime.MIN);
		LocalDateTime endOfDate = endDate.atTime(LocalTime.MAX);

		List<HostTimeLog> logs = hostTimeLogRepository
				.findByCreatedDateBetween(Timestamp.valueOf(startOfDate), Timestamp.valueOf(endOfDate));

		Function<HostTimeLog, String> hostGroupingBy = log -> log.getHost().getDeviceName() != null
				? log.getHost().getDeviceName()
				: log.getId().toString();

		Map<String, List<TimeLogDto>> timeLogMap = logs.stream()
				.peek(log -> {
					if (!(log.getState() == HostState.FAILED)) {
						log.setState(HostState.REACHABLE);
					}
				})
				.collect(groupingBy(hostGroupingBy, mapping(log -> converter.convert(log), toList())));

		model.addAttribute(TIME_LOG_MAP_MODEL_ATTR, timeLogMap);
		return TIME_LOG_VIEW;
	}

}
