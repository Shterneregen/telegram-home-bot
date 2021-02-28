package random.telegramhomebot.services;

import org.springframework.stereotype.Service;
import random.telegramhomebot.model.HostState;
import random.telegramhomebot.model.HostTimeLog;
import random.telegramhomebot.model.TimeLogDto;

@Service
public class TimeLogConverter {

	public TimeLogDto convertToDto(HostTimeLog timeLog) {
		return TimeLogDto.builder()
				.state(getState(timeLog))
				.createdDate(timeLog.getCreatedDate())
				.build();
	}

	private String getState(HostTimeLog timeLog) {
		return timeLog.getState() != HostState.FAILED
				? HostState.REACHABLE.toString()
				: timeLog.getState().toString();
	}
}
