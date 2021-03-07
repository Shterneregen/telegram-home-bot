package random.telegramhomebot.telegram;

import com.vdurmont.emoji.EmojiParser;

public enum Icon {
	PLUS(":heavy_plus_sign:"),
	MINUS(":heavy_minus_sign:"),
	CHECK(":white_check_mark:"),
	NOT(":x:");

	private String value;

	public String get() {
		return EmojiParser.parseToUnicode(value);
	}

	Icon(String value) {
		this.value = value;
	}
}
