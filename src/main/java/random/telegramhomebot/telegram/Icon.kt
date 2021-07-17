package random.telegramhomebot.telegram

import com.vdurmont.emoji.EmojiParser

enum class Icon(private val value: String) {
    PLUS(":heavy_plus_sign:"),
    MINUS(":heavy_minus_sign:"),
    CHECK(":white_check_mark:"),
    NOT(":x:");

    fun get(): String {
        return EmojiParser.parseToUnicode(value)
    }
}