package random.telegramhomebot.model;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class TimeLogDto {
	private String state;
	private Timestamp createdDate;
}
