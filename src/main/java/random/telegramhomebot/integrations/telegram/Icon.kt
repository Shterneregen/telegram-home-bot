package random.telegramhomebot.integrations.telegram

import com.vdurmont.emoji.EmojiManager

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
    WHITE_CIRCLE("white_circle"),
    WARNING("warning")
    ;

    fun get(): String {
        val emoji = EmojiManager.getForAlias(value)
        return emoji?.unicode ?: ":$value:"
    }

    companion object {
        fun isChecked(flag: Boolean) = if (flag) CHECK.get() else NOT.get()
    }
}
