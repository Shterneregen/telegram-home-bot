package random.telegramhomebot.services;

import org.springframework.stereotype.Service;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.model.TimeLogDto;

@Service
public class TimeLogConverter {

	public TimeLogDto convert(HostTimeLog log) {
		return new TimeLogDto(String.valueOf(log.getCreatedDate().getHours()),
				String.valueOf(log.getCreatedDate().getMinutes()), log.getState().toString());
	}
}
