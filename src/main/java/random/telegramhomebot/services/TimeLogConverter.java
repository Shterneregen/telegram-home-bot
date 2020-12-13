package random.telegramhomebot.services;

import org.springframework.stereotype.Service;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.model.TimeLogDto;

@Service
public class TimeLogConverter {

	public TimeLogDto convert(HostTimeLog timeLog) {
		return TimeLogDto.builder()
				.state(timeLog.getState().toString())
				.createdDate(timeLog.getCreatedDate())
				.build();
	}
}
