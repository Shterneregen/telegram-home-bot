package random.telegramhomebot.model;

public class TimeLogDto {

	private String hours;
	private String minutes;
	private String state;

	public TimeLogDto(String hours, String minutes, String state) {
		this.hours = hours;
		this.minutes = minutes;
		this.state = state;
	}

	public String getHours() {
		return hours;
	}

	public String getMinutes() {
		return minutes;
	}

	public String getState() {
		return state;
	}
}
