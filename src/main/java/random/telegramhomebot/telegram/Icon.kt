package random.telegramhomebot.telegram

import emoji4j.EmojiUtils

enum class Icon(private val value: String) {
    PLUS("heavy_plus_sign"),
    MINUS("heavy_minus_sign"),
    CHECK("white_check_mark"),
    NOT("x"),
    REFRESH("arrows_counterclockwise"),
    SCROLL("scroll"),
    DESKTOP_COMPUTER("desktop_computer"),
    HAMMER("hammer"),
    GREEN_CIRCLE("green_circle"),
    YELLOW_CIRCLE("yellow_circle"),
    RED_CIRCLE("red_circle"),
    WHITE_CIRCLE("white_circle")
    ;

    fun get(): String = EmojiUtils.getEmoji(value)?.emoji ?: value

    companion object {
        fun isChecked(flag: Boolean) = if (flag) CHECK.get() else NOT.get()
    }
}
